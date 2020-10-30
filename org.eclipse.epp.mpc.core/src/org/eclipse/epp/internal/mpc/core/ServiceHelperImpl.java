/*******************************************************************************
 * Copyright (c) 2014, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.IMarketplaceUnmarshaller;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Carsten Reckord
 */
@Component(name = "org.eclipse.epp.mpc.core.servicehelper", service = { ServiceHelper.class, ServiceHelperImpl.class })
public class ServiceHelperImpl extends ServiceHelper {

	private static ServiceHelperImpl instance;

	private ServiceTracker<IMarketplaceServiceLocator, IMarketplaceServiceLocator> locatorServiceTracker;

	private ServiceTracker<ITransportFactory, ITransportFactory> transportFactoryTracker;

	private ServiceTracker<ITransportFactory, TransportFactory> legacyTransportFactoryTracker;

	private ServiceTracker<IMarketplaceUnmarshaller, IMarketplaceUnmarshaller> unmarshallerTracker;

	private ComponentContext context;

	private List<ServiceRegistration<?>> serviceRegistrations;

	@Activate
	void activate(final ComponentContext context) throws InvalidSyntaxException {
		this.context = context;
		BundleContext bundleContext = context.getBundleContext();

		registerServices(bundleContext);

		locatorServiceTracker = new ServiceTracker<>(bundleContext,
				IMarketplaceServiceLocator.class, null);
		locatorServiceTracker.open(true);

		transportFactoryTracker = new ServiceTracker<>(bundleContext,
				ITransportFactory.class, null);
		transportFactoryTracker.open(true);

		legacyTransportFactoryTracker = new TransportFactory.LegacyTransportFactoryTracker(bundleContext);
		legacyTransportFactoryTracker.open(true);

		unmarshallerTracker = new ServiceTracker<>(bundleContext,
				IMarketplaceUnmarshaller.class, null);
		unmarshallerTracker.open(true);
		synchronized (ServiceHelperImpl.class) {
			if (instance == null) {
				instance = this;
			}
		}
	}

	@Deactivate
	void deactivate(BundleContext context) {
		synchronized (ServiceHelperImpl.class) {
			if (instance == this) {
				instance = null;
			}
		}
		this.context = null;
		if (locatorServiceTracker != null) {
			locatorServiceTracker.close();
			locatorServiceTracker = null;
		}
		if (transportFactoryTracker != null) {
			transportFactoryTracker.close();
			transportFactoryTracker = null;
		}
		if (legacyTransportFactoryTracker != null) {
			legacyTransportFactoryTracker.close();
			legacyTransportFactoryTracker = null;
		}
		if (unmarshallerTracker != null) {
			unmarshallerTracker.close();
			unmarshallerTracker = null;
		}
		unregisterServices();
	}

	private void registerServices(BundleContext context) throws InvalidSyntaxException {
		List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<>();
		this.serviceRegistrations = serviceRegistrations;

		List<ITransportFactory> factories = TransportFactory.listAvailableFactories();//highest-prio factory comes first
		if (factories.isEmpty()) {
			return;
		}

		Collection<ServiceReference<ITransportFactory>> serviceReferences = context
				.getServiceReferences(ITransportFactory.class, null);
		int lowestPriority = Integer.MAX_VALUE;
		for (ServiceReference<ITransportFactory> serviceReference : serviceReferences) {
			Object legacyProperty = serviceReference.getProperty(TransportFactory.LEGACY_TRANSPORT_KEY);
			if (legacyProperty != null) {
				continue;
			}
			Integer ranking = (Integer) serviceReference.getProperty(Constants.SERVICE_RANKING);
			lowestPriority = Math.min(lowestPriority, ranking == null ? 0 : ranking.intValue());
		}

		int maxLegacyPriority, step;
		if (lowestPriority >= 0) {
			maxLegacyPriority = -100;
			step = 100;
		} else {
			int available = lowestPriority - Integer.MIN_VALUE;
			step = Math.min(100, available / factories.size());
			if (step == 0) {
				step = 1;
				maxLegacyPriority = Integer.MIN_VALUE + factories.size();
			} else {
				maxLegacyPriority = lowestPriority - step;
			}
		}
		int prio = maxLegacyPriority;//prio counts down from 0 in steps of 100
		for (ITransportFactory factory : factories) {
			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(Constants.SERVICE_RANKING, prio);
			properties.put(ComponentConstants.COMPONENT_NAME, "legacy:" + factory.getClass().getName());
			properties.put(TransportFactory.LEGACY_TRANSPORT_KEY, true);
			ServiceRegistration<ITransportFactory> registration = context.registerService(ITransportFactory.class,
					factory, properties);
			serviceRegistrations.add(registration);
			prio -= step;
		}
	}

	private void unregisterServices() {
		List<ServiceRegistration<?>> serviceRegistrations = this.serviceRegistrations;
		this.serviceRegistrations = null;
		if (serviceRegistrations != null) {
			for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
				serviceRegistration.unregister();
			}
		}
	}

	@Override
	protected IMarketplaceServiceLocator doGetMarketplaceServiceLocator() {
		return locatorServiceTracker == null ? null : locatorServiceTracker.getService();
	}

	@Override
	protected IMarketplaceUnmarshaller doGetMarketplaceUnmarshaller() {
		return unmarshallerTracker == null ? null : unmarshallerTracker.getService();
	}

	@Override
	protected ITransportFactory doGetTransportFactory() {
		return transportFactoryTracker == null ? null : transportFactoryTracker.getService();
	}

	/**
	 * This method is just here to provide access to the legacy TransportFactory implementations without the need of a
	 * singleton.
	 *
	 * @deprecated use {@link #getTransportFactory()}
	 */
	@Deprecated
	public TransportFactory getLegacyTransportFactory() {
		return legacyTransportFactoryTracker == null ? null : legacyTransportFactoryTracker.getService();
	}

	public ServiceRegistration<IMarketplaceServiceLocator> registerMarketplaceServiceLocator(
			IMarketplaceServiceLocator marketplaceServiceLocator) {
		return context.getBundleContext()
				.registerService(IMarketplaceServiceLocator.class, marketplaceServiceLocator,
						ServiceUtil.higherServiceRanking(locatorServiceTracker.getServiceReference(), null));
	}

	public ServiceRegistration<IMarketplaceUnmarshaller> registerMarketplaceUnmarshaller(
			IMarketplaceUnmarshaller unmarshaller) {
		return context.getBundleContext()
				.registerService(IMarketplaceUnmarshaller.class, unmarshaller,
						ServiceUtil.higherServiceRanking(unmarshallerTracker.getServiceReference(), null));
	}

	public ServiceRegistration<ITransportFactory> registerTransportFactory(ITransportFactory transportFactory) {
		return context.getBundleContext()
				.registerService(ITransportFactory.class, transportFactory,
						ServiceUtil.higherServiceRanking(transportFactoryTracker.getServiceReference(), null));
	}

	public ServiceRegistration<IMarketplaceService> registerMarketplaceService(String baseUrl,
			IMarketplaceService marketplaceService) {
		Dictionary<String, Object> properties = ServiceUtil.serviceRanking(Integer.MAX_VALUE, null);
		properties.put(IMarketplaceService.BASE_URL, baseUrl);
		return context.getBundleContext().registerService(IMarketplaceService.class, marketplaceService, properties);
	}

	public ServiceRegistration<ICatalogService> registerCatalogService(ICatalogService catalogService) {
		return context.getBundleContext()
				.registerService(ICatalogService.class, catalogService,
						ServiceUtil.serviceRanking(Integer.MAX_VALUE, null));
	}

	/**
	 * @noreference For test purposes only. This method is not intended to be referenced by clients.
	 */
	public void setSuspended(boolean suspend) throws Exception {
		String pid = (String) context.getProperties().get(Constants.SERVICE_PID);
		if (suspend) {
			context.disableComponent(pid);
			unregisterServices();
		} else {
			context.enableComponent(pid);
			//registerServices(context.getBundleContext());
		}
	}

	public static ServiceHelperImpl getImplInstance() {
		synchronized (ServiceHelperImpl.class) {
			if (instance == null) {
				ServiceHelperImpl registered = ServiceUtil.getService(ServiceHelperImpl.class, ServiceHelperImpl.class);
				if (instance == null && registered != null) {
					instance = registered;
				}
			}
			return instance;
		}
	}
}
