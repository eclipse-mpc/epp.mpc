/*******************************************************************************
 * Copyright (c) 2010, 2020 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.service.CachingMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.DefaultCatalogService;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientService;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A service locator for obtaining {@link IMarketplaceService} instances.
 *
 * @author David Green
 * @author Carsten Reckord
 */
@Component(name = "org.eclipse.epp.mpc.core.servicelocator", service = IMarketplaceServiceLocator.class)
public class ServiceLocator implements IMarketplaceServiceLocator {

	private static final String DEBUG_CLIENT_REMOVE_OPTION = "xxx"; //$NON-NLS-1$

	private static final String DEBUG_CLIENT_OPTIONS_PATH = MarketplaceClientCore.BUNDLE_ID + "/client/"; //$NON-NLS-1$

	private static boolean DEBUG_FAKE_CLIENT = false;

	private static final String DEBUG_OPTION = "/debug"; //$NON-NLS-1$

	private static final String DEBUG_FAKE_CLIENT_OPTION = "/client/fakeVersion"; //$NON-NLS-1$

	@Component(name = "org.eclipse.epp.mpc.core.debug.options", property = {
			"listener.symbolic.name=org.eclipse.epp.mpc.core" })
	public static class DebugOptionsInitializer implements DebugOptionsListener {

		@Override
		public void optionsChanged(DebugOptions options) {
			boolean debug = options.getBooleanOption(MarketplaceClientCore.BUNDLE_ID + DEBUG_OPTION, false);
			boolean fakeClient = false;
			if (debug) {
				fakeClient = options.getBooleanOption(MarketplaceClientCore.BUNDLE_ID + DEBUG_FAKE_CLIENT_OPTION,
						false);
			}
			DEBUG_FAKE_CLIENT = fakeClient;
		}

	}

	private abstract class DynamicBindingOperation<T, B> implements ServiceReferenceOperation<T> {

		private final String dynamicBindId;

		private final ServiceReference<B> binding;

		public DynamicBindingOperation(String dynamicBindId, ServiceReference<B> binding) {
			this.dynamicBindId = dynamicBindId;
			this.binding = binding;
		}

		@Override
		public void apply(ServiceReference<T> reference) {
			ServiceRegistration<T> registration = getDynamicServiceInstance(reference);
			if (registration != null) {
				Dictionary<String, Object> properties = ServiceUtil.getProperties(reference);
				if (properties.get(dynamicBindId) != null) {
					return;
				}
				T service = ServiceUtil.getService(registration);
				B currentBinding = service == null ? null : getCurrentBinding(service);
				apply(service, currentBinding, registration, properties);
			}
		}

		protected void apply(T service, B currentBinding, ServiceRegistration<T> registration,
				Dictionary<String, Object> properties) {
			if (service != null && currentBinding == null) {
				properties.put(dynamicBindId, binding);
				registration.setProperties(properties);
			}
		}

		protected abstract B getCurrentBinding(T service);
	}

	private static interface ServiceReferenceOperation<T> {
		void apply(ServiceReference<T> reference);
	}

	private static ServiceLocator instance;

	private ICatalogService catalogService;

	private ServiceTracker<IMarketplaceService, IMarketplaceService> marketplaceServiceTracker;

	private ServiceTracker<ICatalogService, ICatalogService> catalogServiceTracker;

	private URL defaultCatalogUrl;

	private URL defaultMarketplaceUrl;

	private final List<ServiceRegistration<?>> dynamicServiceRegistrations = new ArrayList<>();

	private HttpClientService httpClient;

	public ServiceLocator() {
		defaultMarketplaceUrl = DefaultMarketplaceService.DEFAULT_SERVICE_URL;
		defaultCatalogUrl = DefaultCatalogService.DEFAULT_CATALOG_SERVICE_URL;
	}

	public HttpClientService getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(HttpClientService httpClient) {
		HttpClientService oldClient = this.httpClient;
		this.httpClient = httpClient;
		if (oldClient != httpClient) {
			updateHttpClient(httpClient);
		}
	}

	@Reference(unbind = "unbindHttpClient", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	public void bindHttpClient(HttpClientService httpClient) {
		setHttpClient(httpClient);
	}

	public void unbindHttpClient(HttpClientService httpClient) {
		if (this.httpClient == httpClient) {
			setHttpClient(null);
		}
	}

	private void updateHttpClient(HttpClientService httpClient) {
		applyServiceReferenceOperation(marketplaceServiceTracker, new ServiceReferenceOperation<IMarketplaceService>() {

			@Override
			public void apply(ServiceReference<IMarketplaceService> reference) {
				ServiceRegistration<IMarketplaceService> registration = getDynamicServiceInstance(reference);
				if (registration != null) {
					IMarketplaceService service = ServiceUtil.getService(registration);
					if (service instanceof DefaultMarketplaceService) {
						((DefaultMarketplaceService) service).setHttpClient(httpClient);
					}
				}
			}
		});
	}

	/**
	 * @deprecated use {@link #getDefaultMarketplaceService()} or {@link #getMarketplaceService(String)} instead
	 */
	@Deprecated
	public IMarketplaceService getMarketplaceService() {
		return getDefaultMarketplaceService();
	}

	@Override
	public IMarketplaceService getDefaultMarketplaceService() {
		return getMarketplaceService(defaultMarketplaceUrl.toExternalForm());
	}

	@Override
	public synchronized IMarketplaceService getMarketplaceService(String baseUrl) {
		IMarketplaceService service = getService(marketplaceServiceTracker, baseUrl);
		if (service != null) {
			return service;
		}
		service = createMarketplaceService(baseUrl);
		registerService(baseUrl, IMarketplaceService.class, service);
		return service;
	}

	private <T> void registerService(String baseUrl, Class<T> serviceClass, T service) {
		registerService(baseUrl, serviceClass, service, null);
	}

	private <T> ServiceRegistration<T> registerService(String baseUrl, Class<T> serviceClass, T service,
			Dictionary<String, Object> properties) {
		if (baseUrl != null) {
			if (properties == null) {
				properties = new Hashtable<>(1);
			}
			properties.put(IMarketplaceService.BASE_URL, baseUrl);
		}
		ServiceRegistration<T> registration = FrameworkUtil.getBundle(IMarketplaceServiceLocator.class)
				.getBundleContext()
				.registerService(serviceClass, service, properties);
		dynamicServiceRegistrations.add(registration);
		return registration;
	}

	private <T> T getService(ServiceTracker<T, T> serviceTracker, String baseUrl) {
		if (serviceTracker != null) {
			ServiceReference<T>[] serviceReferences = serviceTracker.getServiceReferences();
			if (serviceReferences != null) {
				for (ServiceReference<T> serviceReference : serviceReferences) {
					Object serviceBaseUrl = ServiceUtil.getOverridablePropertyValue(serviceReference,
							IMarketplaceService.BASE_URL);
					if (baseUrl.equals(serviceBaseUrl)) {
						T service = serviceTracker.getService(serviceReference);
						//we don't cache this on our own, since it might become invalid
						if (service != null) {
							return service;
						}
					}
				}
			}
		}
		return null;
	}

	protected IMarketplaceService createMarketplaceService(String baseUrl) {
		IMarketplaceService service;
		URL base;
		try {
			base = URLUtil.toURL(baseUrl);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		DefaultMarketplaceService defaultService = new DefaultMarketplaceService(base);
		Map<String, String> requestMetaParameters = computeDefaultRequestMetaParameters();
		defaultService.setRequestMetaParameters(requestMetaParameters);
		defaultService.setHttpClient(httpClient);
		service = new CachingMarketplaceService(defaultService);
		return service;
	}

	private void unregisterService(ServiceRegistration<?> registration) {
		if (registration != null) {
			dynamicServiceRegistrations.remove(registration);
			registration.unregister();
		}
	}

	/**
	 * OSGi service activation method. Activation will cause the locator to start managing individual marketplace
	 * services and return the same instances per base url on subsequent calls to {@link #getMarketplaceService(String)}
	 * .
	 */
	public synchronized void activate(BundleContext context, Map<?, ?> properties) {
		URL baseUrl = ServiceUtil.getUrl(properties, DEFAULT_URL, null);

		URL catalogUrl = ServiceUtil.getUrl(properties, CATALOG_URL, baseUrl);
		if (catalogUrl != null) {
			this.defaultCatalogUrl = catalogUrl;
		} //else the default value from the constructor is used

		URL marketplaceUrl = ServiceUtil.getUrl(properties, DEFAULT_MARKETPLACE_URL, baseUrl);
		if (marketplaceUrl != null) {
			this.defaultMarketplaceUrl = marketplaceUrl;
		} //else the default value from the constructor is used

		marketplaceServiceTracker = new ServiceTracker<>(context, IMarketplaceService.class, null);
		marketplaceServiceTracker.open(true);

		catalogServiceTracker = new ServiceTracker<>(context, ICatalogService.class, null);
		catalogServiceTracker.open(true);
	}

	private static <T> void applyServiceReferenceOperation(ServiceTracker<T, ?> serviceTracker,
			ServiceReferenceOperation<T> op) {
		if (serviceTracker != null) {
			ServiceReference<T>[] serviceReferences = serviceTracker.getServiceReferences();
			if (serviceReferences != null) {
				for (ServiceReference<T> serviceReference : serviceReferences) {
					op.apply(serviceReference);
				}
			}
		}
	}

	private <T, R extends T> ServiceRegistration<T> getDynamicServiceInstance(ServiceReference<T> reference) {
		for (ServiceRegistration<?> serviceRegistration : dynamicServiceRegistrations) {
			if (reference.equals(serviceRegistration.getReference())) {
				@SuppressWarnings("unchecked")
				ServiceRegistration<T> referencedRegistration = (ServiceRegistration<T>) serviceRegistration;
				return referencedRegistration;
			}
		}
		return null;
	}

	/**
	 * OSGi service activation method. Deactivation will cause the locator to stop managing individual marketplace
	 * services. Multiple calls to {@link #getMarketplaceService(String)} will return a new instance every time.
	 */
	public synchronized void deactivate() {
		if (marketplaceServiceTracker != null) {
			marketplaceServiceTracker.close();
			marketplaceServiceTracker = null;
		}
		if (catalogServiceTracker != null) {
			catalogServiceTracker.close();
			catalogServiceTracker = null;
		}
		for (ServiceRegistration<?> serviceRegistration : dynamicServiceRegistrations) {
			serviceRegistration.unregister();
		}
	}

	@Override
	public synchronized ICatalogService getCatalogService() {
		if (catalogServiceTracker != null) {
			ICatalogService registeredService = catalogServiceTracker.getService();
			if (registeredService != null) {
				//we don't cache this on our own, since it might become invalid
				return registeredService;
			}
		}
		if (catalogService != null) {
			return catalogService;
		}
		ICatalogService catalogService = new DefaultCatalogService(defaultCatalogUrl);
		registerService(null, ICatalogService.class, catalogService);
		return catalogService;
	}

	/**
	 * for testing purposes
	 *
	 * @deprecated don't call this outside of tests
	 */
	@Deprecated
	public static synchronized void setInstance(ServiceLocator instance) {
		ServiceLocator.instance = instance;
	}

	/**
	 * @deprecated acquire the registered {@link IMarketplaceServiceLocator} OSGi service instead.
	 */
	@Deprecated
	public static synchronized ServiceLocator getInstance() {
		if (instance != null) {
			return instance;
		}
		IMarketplaceServiceLocator locator = getCompatibilityLocator();
		if (locator != null && locator instanceof ServiceLocator) {
			//don't remember service instance, it might get deregistered
			return (ServiceLocator) locator;
		}
		//remember new default instance
		instance = new ServiceLocator();
		return instance;
	}

	/**
	 * This method is not intended to be referenced by clients. Acquire the registered
	 * {@link IMarketplaceServiceLocator} OSGi service instead.
	 * <p>
	 * This method provides legacy compatibility with the ServiceLocator singleton for internal use only. It will return
	 * the ServiceLocator singleton if it has been set explicitly. Otherwise it will return the registered default
	 * {@link IMarketplaceServiceLocator} OSGi service.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static synchronized IMarketplaceServiceLocator getCompatibilityLocator() {
		if (instance != null) {
			return instance;
		}
		IMarketplaceServiceLocator locator = ServiceHelper.getMarketplaceServiceLocator();
		if (locator == null) {
			//remember new default instance
			instance = new ServiceLocator();
			locator = instance;
		}
		return locator;
	}

	public static Map<String, String> computeDefaultRequestMetaParameters() {
		Map<String, String> requestMetaParameters = new LinkedHashMap<>();

		addDefaultRequestMetaParameter(requestMetaParameters, DefaultMarketplaceService.META_PARAM_CLIENT,
				MarketplaceClientCore.BUNDLE_ID);

		addDefaultRequestMetaParameter(requestMetaParameters, DefaultMarketplaceService.META_PARAM_OS,
				Platform.getOS());

		// also send the platform version to distinguish between 3.x and 4.x platforms using the same runtime
		Bundle platformBundle = Platform.getBundle("org.eclipse.platform"); //$NON-NLS-1$
		addDefaultRequestMetaParameter(requestMetaParameters, DefaultMarketplaceService.META_PARAM_PLATFORM_VERSION,
				platformBundle == null ? null : shortenVersionString(platformBundle.getVersion().toString()));

		return requestMetaParameters;
	}

	public static Map<String, String> computeProductInfo() {
		Map<String, String> productInfo = new LinkedHashMap<>();

		BundleContext bundleContext = FrameworkUtil.getBundle(MarketplaceClientCore.class).getBundleContext();

		IProduct product = Platform.getProduct();
		String productId;
		{
			productId = bundleContext.getProperty("eclipse.product"); //$NON-NLS-1$
			if (productId == null && product != null) {
				productId = product.getId();
			}
		}
		addDefaultRequestMetaParameter(productInfo, DefaultMarketplaceService.META_PARAM_PRODUCT, productId);
		String productVersion = null;
		if (productId != null) {
			productVersion = bundleContext.getProperty("eclipse.buildId"); //$NON-NLS-1$
			if (productVersion == null && product != null) {
				Bundle productBundle = product.getDefiningBundle();
				if (productBundle != null) {
					productVersion = productBundle.getVersion().toString();
				}
			}
		}
		addDefaultRequestMetaParameter(productInfo, DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION,
				productVersion);

		return productInfo;
	}

	private static void addDefaultRequestMetaParameter(Map<String, String> requestMetaParameters, String key,
			String value) {
		if (DEBUG_FAKE_CLIENT) {
			String debugOption = Platform.getDebugOption(DEBUG_CLIENT_OPTIONS_PATH + key);
			if (DEBUG_CLIENT_REMOVE_OPTION.equals(debugOption)) {
				requestMetaParameters.remove(key);
				return;
			} else if (debugOption != null && !"".equals(debugOption)) { //$NON-NLS-1$
				value = debugOption;
			}
		}
		if (value != null) {
			requestMetaParameters.put(key, value);
		}
	}

	private static String shortenVersionString(String version) {
		int index = version.indexOf('.');
		if (index > -1) {
			index = version.indexOf('.', index + 1);
			if (index > -1) {
				return version.substring(0, index);
			}
		}
		return version;
	}
}
