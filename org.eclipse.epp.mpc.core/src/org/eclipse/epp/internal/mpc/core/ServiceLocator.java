/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.service.CachingMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.DefaultCatalogService;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.MarketplaceStorageService;
import org.eclipse.epp.internal.mpc.core.service.UserFavoritesService;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A service locator for obtaining {@link IMarketplaceService} instances.
 *
 * @author David Green
 * @author Carsten Reckord
 */
public class ServiceLocator implements IMarketplaceServiceLocator {

	private abstract class DynamicBindingOperation<T, B> implements ServiceReferenceOperation<T> {

		private final String dynamicBindId;

		private final ServiceReference<B> binding;

		public DynamicBindingOperation(String dynamicBindId, ServiceReference<B> binding) {
			this.dynamicBindId = dynamicBindId;
			this.binding = binding;
		}

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

	private ServiceTracker<IMarketplaceStorageService, IMarketplaceStorageService> storageServiceTracker;

	private ServiceTracker<IUserFavoritesService, IUserFavoritesService> favoritesServiceTracker;

	private URL defaultCatalogUrl;

	private URL defaultMarketplaceUrl;

	private final List<ServiceRegistration<?>> dynamicServiceRegistrations = new ArrayList<ServiceRegistration<?>>();

	public ServiceLocator() {
		defaultMarketplaceUrl = DefaultMarketplaceService.DEFAULT_SERVICE_URL;
		defaultCatalogUrl = DefaultCatalogService.DEFAULT_CATALOG_SERVICE_URL;
	}

	/**
	 * @deprecated use {@link #getDefaultMarketplaceService()} or {@link #getMarketplaceService(String)} instead
	 */
	@Deprecated
	public IMarketplaceService getMarketplaceService() {
		return getDefaultMarketplaceService();
	}

	public IMarketplaceService getDefaultMarketplaceService() {
		return getMarketplaceService(defaultMarketplaceUrl.toExternalForm());
	}

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
				properties = new Hashtable<String, Object>(1);
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
		IUserFavoritesService favoritesService = getFavoritesService(baseUrl);
		defaultService.setUserFavoritesService(favoritesService);//FIXME WIP service reference!
		service = new CachingMarketplaceService(defaultService);
		return service;
	}

	public IMarketplaceStorageService getStorageService(String marketplaceUrl) {
		return getService(storageServiceTracker, marketplaceUrl);
	}

	public IMarketplaceStorageService getDefaultStorageService() {
		return getStorageService(defaultMarketplaceUrl.toExternalForm());
	}

	public IUserFavoritesService getFavoritesService(String marketplaceUrl) {
		return getService(favoritesServiceTracker, marketplaceUrl);
	}

	public IUserFavoritesService getDefaultFavoritesService() {
		return getFavoritesService(defaultMarketplaceUrl.toExternalForm());
	}

	public IUserFavoritesService registerFavoritesService(String marketplaceBaseUrl, String apiServerUrl,
			String apiKey) {
		IMarketplaceStorageService storageService = getStorageService(marketplaceBaseUrl);
		if (storageService == null) {
			storageService = registerStorageService(marketplaceBaseUrl, apiServerUrl, apiKey);
		}
		UserFavoritesService favoritesService = new UserFavoritesService();
		favoritesService.bindStorageService(storageService);
		registerService(marketplaceBaseUrl, IUserFavoritesService.class, favoritesService);
		return favoritesService;
	}

	public IMarketplaceStorageService registerStorageService(String marketplaceBaseUrl, String apiServerUrl,
			String apiKey) {
		MarketplaceStorageService marketplaceStorageService = new MarketplaceStorageService();
		Hashtable<String, Object> config = new Hashtable<String, Object>();
		config.put("serviceUrl", apiServerUrl);
		if (apiKey != null) {
			config.put("applicationToken", apiKey);
		}
		ServiceRegistration<IMarketplaceStorageService> registration = registerService(marketplaceBaseUrl,
				IMarketplaceStorageService.class, marketplaceStorageService, config);
		BundleContext bundleContext = ServiceUtil.getBundleContext(registration);
		if (bundleContext != null) {
			marketplaceStorageService.activate(bundleContext, config);
		}
		return marketplaceStorageService;
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

		marketplaceServiceTracker = new ServiceTracker<IMarketplaceService, IMarketplaceService>(context,
				IMarketplaceService.class, null);
		marketplaceServiceTracker.open(true);

		catalogServiceTracker = new ServiceTracker<ICatalogService, ICatalogService>(context,
				ICatalogService.class, null);
		catalogServiceTracker.open(true);

		storageServiceTracker = new ServiceTracker<IMarketplaceStorageService, IMarketplaceStorageService>(context,
				IMarketplaceStorageService.class,
				new ServiceTrackerCustomizer<IMarketplaceStorageService, IMarketplaceStorageService>() {

			public IMarketplaceStorageService addingService(
					ServiceReference<IMarketplaceStorageService> reference) {
				IMarketplaceStorageService service = storageServiceTracker.addingService(reference);
				Object marketplaceUrl = ServiceUtil.getOverridablePropertyValue(reference,
						IMarketplaceService.BASE_URL);
				if (marketplaceUrl != null && service != null) {
					bindToUserFavoritesServices(marketplaceUrl.toString(), reference);
				}
				return service;
			}

			public void modifiedService(ServiceReference<IMarketplaceStorageService> reference,
					IMarketplaceStorageService service) {
				Object marketplaceUrl = ServiceUtil.getOverridablePropertyValue(reference,
						IMarketplaceService.BASE_URL);
				if (marketplaceUrl != null) {
					rebindToUserFavoritesServices(marketplaceUrl.toString(), reference, service);
				} else {
					unbindFromUserFavoritesServices(reference, service);
				}
			}

			public void removedService(ServiceReference<IMarketplaceStorageService> reference,
					IMarketplaceStorageService service) {
				unbindFromUserFavoritesServices(reference, service);
			}
		});
		storageServiceTracker.open(true);

		favoritesServiceTracker = new ServiceTracker<IUserFavoritesService, IUserFavoritesService>(context,
				IUserFavoritesService.class,
				new ServiceTrackerCustomizer<IUserFavoritesService, IUserFavoritesService>() {
			public IUserFavoritesService addingService(ServiceReference<IUserFavoritesService> reference) {
				return ServiceUtil.getService(reference);
			}

			public void modifiedService(ServiceReference<IUserFavoritesService> reference,
					IUserFavoritesService service) {
				if (!(service instanceof UserFavoritesService)) {
					return;
				}
				ServiceReference<?> storageServiceBinding = (ServiceReference<?>) reference
						.getProperty("bind.storageService");
				if (storageServiceBinding != null && service.getStorageService() == null) {
					((UserFavoritesService) service).bindStorageService(
							(IMarketplaceStorageService) ServiceUtil.getService(storageServiceBinding));
				} else if (service.getStorageService() != null
						&& getDynamicServiceInstance(reference) != null) {
					((UserFavoritesService) service).setStorageService(null);
				}
			}

			public void removedService(ServiceReference<IUserFavoritesService> reference,
					IUserFavoritesService service) {
				// ignore

			}

		});
		favoritesServiceTracker.open(true);
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

	private void bindToUserFavoritesServices(final String marketplaceUrl,
			ServiceReference<IMarketplaceStorageService> serviceReference) {
		applyServiceReferenceOperation(favoritesServiceTracker,
				new DynamicBindingOperation<IUserFavoritesService, IMarketplaceStorageService>("bind.storageService",
						serviceReference) {

			@Override
			public void apply(ServiceReference<IUserFavoritesService> reference) {
				Object baseUrl = ServiceUtil.getOverridablePropertyValue(reference,
						IMarketplaceService.BASE_URL);
				if (marketplaceUrl.equals(baseUrl)) {
					super.apply(reference);
				}
			}

			@Override
			protected IMarketplaceStorageService getCurrentBinding(IUserFavoritesService service) {
				return service.getStorageService();
			}
		});
	}

	private void rebindToUserFavoritesServices(final String marketplaceUrl,
			final ServiceReference<IMarketplaceStorageService> serviceReference,
			final IMarketplaceStorageService serviceInstance) {
		applyServiceReferenceOperation(favoritesServiceTracker,
				new DynamicBindingOperation<IUserFavoritesService, IMarketplaceStorageService>("bind.storageService",
						serviceReference) {

			@Override
			public void apply(ServiceReference<IUserFavoritesService> reference) {
				Object baseUrl = ServiceUtil.getOverridablePropertyValue(reference,
						IMarketplaceService.BASE_URL);
				if (marketplaceUrl.equals(baseUrl)) {
					super.apply(reference);
				} else {
					unbindFromUserFavoritesService(reference, serviceReference, serviceInstance);
				}
			}

			@Override
			protected IMarketplaceStorageService getCurrentBinding(IUserFavoritesService service) {
				return service.getStorageService();
			}
		});
	}

	private void unbindFromUserFavoritesServices(final ServiceReference<IMarketplaceStorageService> serviceReference,
			final IMarketplaceStorageService serviceInstance) {
		applyServiceReferenceOperation(favoritesServiceTracker,
				new DynamicBindingOperation<IUserFavoritesService, IMarketplaceStorageService>("bind.storageService",
						serviceReference) {

			@Override
			public void apply(ServiceReference<IUserFavoritesService> reference) {
				unbindFromUserFavoritesService(reference, serviceReference, serviceInstance);
			}

			@Override
			protected IMarketplaceStorageService getCurrentBinding(IUserFavoritesService service) {
				return service.getStorageService();
			}
		});
	}

	private void unbindFromUserFavoritesService(ServiceReference<IUserFavoritesService> reference,
			ServiceReference<IMarketplaceStorageService> serviceReference, IMarketplaceStorageService serviceInstance) {
		ServiceRegistration<IUserFavoritesService> registration = getDynamicServiceInstance(reference);
		if (registration == null) {
			return;
		}
		Object binding = reference.getProperty("bind.storageService");
		if (binding != null && serviceReference.equals(binding)) {
			if (registration != null) {
				Dictionary<String, Object> properties = ServiceUtil.getProperties(reference);
				properties.remove("bind.storageService");
				registration.setProperties(properties);
			}
		}
		IUserFavoritesService service = ServiceUtil.getService(registration);
		if (service.getStorageService() == serviceInstance) {
			((UserFavoritesService)service).unbindStorageService(serviceInstance);
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
		if (favoritesServiceTracker != null) {
			favoritesServiceTracker.close();
			favoritesServiceTracker = null;
		}
		if (storageServiceTracker != null) {
			storageServiceTracker.close();
			storageServiceTracker = null;
		}
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
		Map<String, String> requestMetaParameters = new HashMap<String, String>();

		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_CLIENT, MarketplaceClientCore.BUNDLE_ID);
		Bundle clientBundle = Platform.getBundle(MarketplaceClientCore.BUNDLE_ID);
		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_CLIENT_VERSION, clientBundle.getVersion()
				.toString());

		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_OS, Platform.getOS());
		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_WS, Platform.getWS());
		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_NL, Platform.getNL());
		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_JAVA_VERSION, System.getProperty("java.version")); //$NON-NLS-1$
		IProduct product = Platform.getProduct();
		if (product != null) {
			requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_PRODUCT, product.getId());
			Bundle productBundle = product.getDefiningBundle();
			if (productBundle != null) {
				requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION,
						productBundle.getVersion().toString());
			}
		}
		Bundle runtimeBundle = Platform.getBundle("org.eclipse.core.runtime"); //$NON-NLS-1$
		if (runtimeBundle != null) {
			requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_RUNTIME_VERSION, runtimeBundle.getVersion()
					.toString());
		}
		// also send the platform version to distinguish between 3.x and 4.x platforms using the same runtime
		Bundle platformBundle = Platform.getBundle("org.eclipse.platform"); //$NON-NLS-1$
		if (platformBundle != null) {
			requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_PLATFORM_VERSION,
					platformBundle.getVersion().toString());
		}
		return requestMetaParameters;
	}
}
