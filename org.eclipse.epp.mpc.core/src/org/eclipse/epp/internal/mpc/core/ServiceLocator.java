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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.service.CachingMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.DefaultCatalogService;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A service locator for obtaining {@link IMarketplaceService} instances.
 *
 * @author David Green
 * @author Carsten Reckord
 */
public class ServiceLocator implements IMarketplaceServiceLocator {

	private static ServiceLocator instance;

	private Map<String, IMarketplaceService> marketplaceServices;

	private ICatalogService catalogService;

	private ServiceTracker<IMarketplaceService, IMarketplaceService> marketplaceServiceTracker;

	private ServiceTracker<ICatalogService, ICatalogService> catalogServiceTracker;

	private URL defaultCatalogUrl;

	private URL defaultMarketplaceUrl;

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
		IMarketplaceService service;
		if (marketplaceServiceTracker != null) {
			ServiceReference<IMarketplaceService>[] serviceReferences = marketplaceServiceTracker.getServiceReferences();
			if (serviceReferences != null) {
				for (ServiceReference<IMarketplaceService> serviceReference : serviceReferences) {
					Object serviceBaseUrl = serviceReference.getProperty(IMarketplaceService.BASE_URL);
					if (baseUrl.equals(serviceBaseUrl)) {
						service = marketplaceServiceTracker.getService(serviceReference);
						//we don't cache this on our own, since it might become invalid
						if (service != null) {
							return service;
						}
					}
				}
			}
		}
		if (marketplaceServices != null) {
			service = marketplaceServices.get(baseUrl);
			if (service != null) {
				return service;
			}
		}
		service = createMarketplaceService(baseUrl);

		if (marketplaceServices != null) {
			marketplaceServices.put(baseUrl, service);
		}
		return service;
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
		service = new CachingMarketplaceService(defaultService);
		return service;
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

		if (marketplaceServices == null) {
			marketplaceServices = new HashMap<String, IMarketplaceService>();
		}

		marketplaceServiceTracker = new ServiceTracker<IMarketplaceService, IMarketplaceService>(context,
				IMarketplaceService.class, null);
		marketplaceServiceTracker.open(true);

		catalogServiceTracker = new ServiceTracker<ICatalogService, ICatalogService>(context,
				ICatalogService.class, null);
		catalogServiceTracker.open(true);
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
		marketplaceServices = null;
		catalogService = null;
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
		if (marketplaceServices != null) {//used as an indicator to cache value
			this.catalogService = catalogService;
		}
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
		if (locator != null) {
			if (locator instanceof ServiceLocator) {
				//don't remember service instance, it might get deregistered
				return (ServiceLocator) locator;
			}
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
