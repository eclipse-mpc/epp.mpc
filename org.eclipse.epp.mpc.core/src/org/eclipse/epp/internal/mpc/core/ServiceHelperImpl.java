/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import java.util.Dictionary;

import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.IMarketplaceUnmarshaller;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Carsten Reckord
 */
public class ServiceHelperImpl extends ServiceHelper {

	private ServiceTracker<IMarketplaceServiceLocator, IMarketplaceServiceLocator> locatorServiceTracker;

	private ServiceTracker<ITransportFactory, ITransportFactory> transportFactoryTracker;

	private ServiceTracker<IMarketplaceUnmarshaller, IMarketplaceUnmarshaller> unmarshallerTracker;

	private BundleContext context;

	void startTracking(BundleContext context) {
		this.context = context;
		locatorServiceTracker = new ServiceTracker<IMarketplaceServiceLocator, IMarketplaceServiceLocator>(context,
				IMarketplaceServiceLocator.class, null);
		locatorServiceTracker.open(true);

		transportFactoryTracker = new ServiceTracker<ITransportFactory, ITransportFactory>(context,
				ITransportFactory.class, null);
		transportFactoryTracker.open(true);

		unmarshallerTracker = new ServiceTracker<IMarketplaceUnmarshaller, IMarketplaceUnmarshaller>(context,
				IMarketplaceUnmarshaller.class, null);
		unmarshallerTracker.open(true);
	}

	void stopTracking(BundleContext context) {
		this.context = null;
		if (locatorServiceTracker != null) {
			locatorServiceTracker.close();
			locatorServiceTracker = null;
		}
		if (transportFactoryTracker != null) {
			transportFactoryTracker.close();
			transportFactoryTracker = null;
		}
		if (unmarshallerTracker != null) {
			unmarshallerTracker.close();
			unmarshallerTracker = null;
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

	public ServiceRegistration<IMarketplaceServiceLocator> registerMarketplaceServiceLocator(
			IMarketplaceServiceLocator marketplaceServiceLocator) {
		return context.registerService(IMarketplaceServiceLocator.class, marketplaceServiceLocator,
				ServiceUtil.higherServiceRanking(locatorServiceTracker.getServiceReference(), null));
	}

	public ServiceRegistration<IMarketplaceUnmarshaller> registerMarketplaceUnmarshaller(
			IMarketplaceUnmarshaller unmarshaller) {
		return context.registerService(IMarketplaceUnmarshaller.class, unmarshaller,
				ServiceUtil.higherServiceRanking(unmarshallerTracker.getServiceReference(), null));
	}

	public ServiceRegistration<ITransportFactory> registerTransportFactory(ITransportFactory transportFactory) {
		return context.registerService(ITransportFactory.class, transportFactory,
				ServiceUtil.higherServiceRanking(transportFactoryTracker.getServiceReference(), null));
	}

	public ServiceRegistration<IMarketplaceService> registerMarketplaceService(String baseUrl,
			IMarketplaceService marketplaceService) {
		Dictionary<String, Object> properties = ServiceUtil.serviceRanking(Integer.MAX_VALUE, null);
		properties.put(IMarketplaceService.BASE_URL, baseUrl);
		return context.registerService(IMarketplaceService.class, marketplaceService, properties);
	}

	public ServiceRegistration<ICatalogService> registerCatalogService(ICatalogService catalogService) {
		return context.registerService(ICatalogService.class, catalogService,
				ServiceUtil.serviceRanking(Integer.MAX_VALUE, null));
	}
}
