/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NoHttpResponseException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.internal.mpc.core.service.ServiceUnavailableException;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Factory to retrieve Transport instances of p2. Will delegate to version-dependent implementations.
 *
 * @author David Green
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public abstract class TransportFactory implements ITransportFactory {

	public static final String LEGACY_TRANSPORT_KEY = "org.eclipse.epp.mpc.core.service.transport.legacy"; //$NON-NLS-1$

	public static final String LEGACY_TRANSPORT_COMPONENT_NAME = "org.eclipse.epp.mpc.core.transportfactory.legacy"; //$NON-NLS-1$

	public static final String DISABLED_TRANSPORTS_KEY = "org.eclipse.epp.mpc.core.service.transport.disabled"; //$NON-NLS-1$

	private static final String[] factoryClasses = new String[] { //
			"org.eclipse.epp.internal.mpc.core.util.P2TransportFactory", // //$NON-NLS-1$
			"org.eclipse.epp.internal.mpc.core.util.Eclipse36TransportFactory", // //$NON-NLS-1$
	"org.eclipse.epp.internal.mpc.core.util.JavaPlatformTransportFactory" }; //$NON-NLS-1$

	private static final String HTTP_TRANSPORT_FACTORY_ID = "org.eclipse.epp.mpc.core.transport.http.factory"; //$NON-NLS-1$

	private static final String HTTP_TRANSPORT_WRAPPER_ID = "org.eclipse.epp.mpc.core.transport.http.wrapper"; //$NON-NLS-1$

	private static final String ECF_HTTPCLIENT4_TRANSPORT_ID = "org.eclipse.ecf.provider.filetransfer.httpclient4"; //$NON-NLS-1$

	private static final String ECF_EXCLUDES_PROPERTY = "org.eclipse.ecf.provider.filetransfer.excludeContributors"; //$NON-NLS-1$

	public static String computeDisabledTransportsFilter() {
		BundleContext bundleContext = FrameworkUtil.getBundle(TransportFactory.class).getBundleContext();
		String disabledTransportsStr = bundleContext.getProperty(DISABLED_TRANSPORTS_KEY);
		if (disabledTransportsStr == null) {
			disabledTransportsStr = ""; //$NON-NLS-1$
		}
		String excludeContributors = bundleContext
				.getProperty(ECF_EXCLUDES_PROPERTY);
		if (excludeContributors != null && excludeContributors.contains(ECF_HTTPCLIENT4_TRANSPORT_ID)) {
			disabledTransportsStr += "," + HTTP_TRANSPORT_WRAPPER_ID + "," + HTTP_TRANSPORT_FACTORY_ID; //$NON-NLS-1$//$NON-NLS-2$
		}
		Set<String> disabledTransports = new HashSet<String>();
		StringBuilder bldr = new StringBuilder("(&"); //$NON-NLS-1$
		for (String transportName : disabledTransportsStr.split(",")) { //$NON-NLS-1$
			transportName = transportName.trim();
			if (!"".equals(transportName) && disabledTransports.add(transportName)) { //$NON-NLS-1$
				bldr.append("(!(") //$NON-NLS-1$
				.append(ComponentConstants.COMPONENT_NAME)
				.append("=") //$NON-NLS-1$
				.append(transportName)
				.append("))"); //$NON-NLS-1$
			}
		}
		bldr.append(")"); //$NON-NLS-1$
		String disabledTransportsFilter = disabledTransports.isEmpty() ? "" : bldr.toString(); //$NON-NLS-1$
		return disabledTransportsFilter;
	}

	public static final class LegacyFactory implements ITransportFactory {
		private ITransportFactory delegate;

		private ServiceReference<ITransportFactory> delegateReference;

		public LegacyFactory() {
		}

		public ITransportFactory getDelegate() {
			return delegate;
		}

		public org.eclipse.epp.mpc.core.service.ITransport getTransport() {
			return delegate.getTransport();
		}

		public void activate(ComponentContext context) throws InvalidSyntaxException {
			BundleContext bundleContext = context.getBundleContext();
			Collection<ServiceReference<ITransportFactory>> serviceReferences = bundleContext.getServiceReferences(
					ITransportFactory.class,
					"(&" //$NON-NLS-1$
					+ "(" + LEGACY_TRANSPORT_KEY + "=true)" //$NON-NLS-1$//$NON-NLS-2$
					+ "(!(" + ComponentConstants.COMPONENT_NAME + "=" + LEGACY_TRANSPORT_COMPONENT_NAME + "))" //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					+ computeDisabledTransportsFilter()
					+ ")"); //$NON-NLS-1$
			if (!serviceReferences.isEmpty()) {
				for (ServiceReference<ITransportFactory> serviceReference : serviceReferences) {
					ITransportFactory service = bundleContext.getService(serviceReference);
					if (service instanceof TransportFactory) {
						delegate = service;
						delegateReference = serviceReference;
						return;
					} else {
						bundleContext.ungetService(serviceReference);
					}
				}
			}
			List<ITransportFactory> availableFactories = listAvailableFactories();
			if (availableFactories.isEmpty()) {
				context.disableComponent(LEGACY_TRANSPORT_COMPONENT_NAME);
				throw new IllegalStateException(Messages.TransportFactory_NoLegacyTransportFactoriesError);
			}
			delegate = availableFactories.get(0);
			delegateReference = null;
		}

		public void deactivate(ComponentContext context) {
			delegate = null;
			if (delegateReference != null) {
				context.getBundleContext().ungetService(delegateReference);
				delegateReference = null;
			}
		}
	}

	public static class LegacyTransportFactoryTracker extends ServiceTracker<ITransportFactory, TransportFactory> {

		public LegacyTransportFactoryTracker(final BundleContext context) {
			super(context, ITransportFactory.class, new LegacyTransportFactoryTrackerCustomizer(context));
		}
	}

	public static class LegacyTransportFactoryTrackerCustomizer
			implements ServiceTrackerCustomizer<ITransportFactory, TransportFactory> {
		private final BundleContext context;

		private final Map<ServiceReference<ITransportFactory>, TransportFactory> trackedServices = new HashMap<ServiceReference<ITransportFactory>, TransportFactory>();

		private LegacyTransportFactoryTrackerCustomizer(BundleContext context) {
			this.context = context;
		}

		public TransportFactory addingService(ServiceReference<ITransportFactory> reference) {
			Object legacyProperty = reference.getProperty(TransportFactory.LEGACY_TRANSPORT_KEY);
			if (!Boolean.parseBoolean(String.valueOf(legacyProperty))) {
				return null;
			}
			ITransportFactory service = context.getService(reference);
			if (service instanceof TransportFactory.LegacyFactory) {
				TransportFactory.LegacyFactory legacyFactory = (TransportFactory.LegacyFactory) service;
				service = legacyFactory.getDelegate();
			}
			if (service instanceof TransportFactory) {
				TransportFactory transportFactory = (TransportFactory) service;
				if (!trackedServices.containsValue(transportFactory)) {
					trackedServices.put(reference, transportFactory);
					return transportFactory;
				}
			}
			return null;
		}

		public void modifiedService(ServiceReference<ITransportFactory> reference, TransportFactory service) {
			// ignore
		}

		public void removedService(ServiceReference<ITransportFactory> reference, TransportFactory service) {
			trackedServices.remove(reference);
		}
	}

	/**
	 * @deprecated use registered {@link ITransportFactory} OSGi service
	 * @see ServiceHelper#getTransportFactory()
	 */
	@Deprecated
	public static synchronized TransportFactory instance() {
		TransportFactory legacyTransportFactory = MarketplaceClientCorePlugin.getDefault()
				.getServiceHelper()
				.getLegacyTransportFactory();
		if (legacyTransportFactory == null) {
			throw new IllegalStateException();
		}
		return legacyTransportFactory;
	}

	public static org.eclipse.epp.mpc.core.service.ITransport createTransport() {
		//search for registered factory service
		BundleContext context = MarketplaceClientCorePlugin.getBundle().getBundleContext();
		ServiceReference<ITransportFactory> serviceReference = getTransportServiceReference(context);
		if (serviceReference != null) {
			ITransportFactory transportService = context.getService(serviceReference);
			if (transportService != null) {
				try {
					return transportService.getTransport();
				} finally {
					transportService = null;
					context.ungetService(serviceReference);
				}
			}
		}
		throw new IllegalStateException();
	}

	public static ServiceReference<ITransportFactory> getTransportServiceReference(BundleContext context) {
		ServiceReference<ITransportFactory> serviceReference = null;
		String disabledTransportsFilter = computeDisabledTransportsFilter();
		if ("".equals(disabledTransportsFilter)) { //$NON-NLS-1$
			serviceReference = context.getServiceReference(ITransportFactory.class);
		} else {
			try {
				Collection<ServiceReference<ITransportFactory>> serviceReferences = context
						.getServiceReferences(ITransportFactory.class, disabledTransportsFilter);
				if (!serviceReferences.isEmpty()) {
					serviceReference = serviceReferences.iterator().next();
				}
			} catch (InvalidSyntaxException e) {
				MarketplaceClientCore.error(e);
				serviceReference = context.getServiceReference(ITransportFactory.class);
			}
		}
		return serviceReference;
	}

	public static List<ITransportFactory> listAvailableFactories() {
		List<ITransportFactory> factories = new ArrayList<ITransportFactory>();
		for (String factoryClass : factoryClasses) {
			TransportFactory factory;
			try {
				factory = (TransportFactory) Class.forName(factoryClass, true, TransportFactory.class.getClassLoader())
						.newInstance();
			} catch (Throwable t) {
				// ignore
				continue;
			}
			try {
				if (factory.isAvailable()) {
					factories.add(factory);
				}
			} catch (Throwable t) {
				MarketplaceClientCore.getLog().log(new Status(IStatus.WARNING, MarketplaceClientCore.BUNDLE_ID,
						Messages.TransportFactory_transportAvailabilityError, t));
			}
		}
		return factories;
	}

	@SuppressWarnings("deprecation")
	public ITransport getTransport() {
		return new ITransport() {

			public InputStream stream(URI location, IProgressMonitor monitor) throws FileNotFoundException,
			org.eclipse.epp.mpc.core.service.ServiceUnavailableException, CoreException {
				try {
					return invokeStream(location, monitor);
				} catch (Exception e) {
					handleStreamExceptions(e);
				}
				return null;
			}

		};
	}

	protected abstract boolean isAvailable();

	protected abstract InputStream invokeStream(URI location, IProgressMonitor monitor) throws Exception;

	@SuppressWarnings("deprecation")
	protected void handleStreamExceptions(Exception e) throws ServiceUnavailableException, CoreException,
	FileNotFoundException {
		if (e instanceof InvocationTargetException) {
			InvocationTargetException targetException = (InvocationTargetException) e;
			Throwable cause = targetException.getCause();
			if (cause instanceof CoreException) {
				CoreException coreCause = (CoreException) cause;
				handleServiceUnavailable(coreCause);
				throw coreCause;
			} else if (cause instanceof FileNotFoundException) {
				throw (FileNotFoundException) cause;
			}

		} else {
			throw new CoreException(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, e.getMessage(), e));
		}
	}


	@SuppressWarnings("deprecation")
	protected static void handleServiceUnavailable(CoreException e) throws ServiceUnavailableException {
		if (e.getStatus().getCode() == 1002) { //failed to read
			Throwable cause = e.getCause();
			if (cause != null) {
				if (cause instanceof NoHttpResponseException || cause.getMessage() != null
						&& cause.getMessage().indexOf("503") != -1) { //$NON-NLS-1$
					throw new ServiceUnavailableException(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID,
							503, Messages.DefaultMarketplaceService_serviceUnavailable503, e));
				}
			}
		}
	}
}