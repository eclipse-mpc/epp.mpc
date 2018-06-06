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
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.MarketplaceService;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.osgi.framework.BundleContext;

/**
 * This service manages instances of {@link IMarketplaceService} and {@link ICatalogService} for accessing marketplace
 * servers. An instance of this service can be retrieved from the OSGi {@link BundleContext#getServiceReference(Class)
 * service registry}.
 * <p>
 * This service references a single {@link ICatalogService} for catalog {@link #getCatalogService() discovery}. For each
 * of the discovered catalogs, a {@link IMarketplaceService marketplace service} can be
 * {@link #getMarketplaceService(String) acquired} using the catalog's {@link ICatalog#getUrl() base url}.
 * <p>
 * The default implementation of this service will look for registered {@link ICatalogService} and
 * {@link IMarketplaceService} instances with matching url paramters in the OSGi service registry and return matching
 * services. If no match is found, the service locator will create a new instance and return this on subsequent calls.
 *
 * @author David Green
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMarketplaceServiceLocator {

	/**
	 * Property key for registered IMarketplaceServiceLocator OSGi services indicating the URL of the default
	 * marketplace and catalog service.
	 * <p>
	 * This is a convenience property to use a common value for
	 * {@link IMarketplaceServiceLocator#DEFAULT_MARKETPLACE_URL} and {@link #CATALOG_URL}
	 *
	 * @see #DEFAULT_MARKETPLACE_URL
	 * @see #CATALOG_URL
	 */
	public static final String DEFAULT_URL = "url"; //$NON-NLS-1$

	/**
	 * Property key for registered IMarketplaceServiceLocator OSGi services indicating the URL of the default
	 * marketplace service.
	 *
	 * @see #DEFAULT_URL
	 * @see #CATALOG_URL
	 */
	public static final String DEFAULT_MARKETPLACE_URL = "marketplaceUrl"; //$NON-NLS-1$

	/**
	 * System property key to override the default marketplace url
	 *
	 * @see DEFAULT_MARKETPLACE_URL
	 * @see DefaultMarketplaceService#DEFAULT_SERVICE_LOCATION
	 */
	@SuppressWarnings("deprecation")
	public static final String DEFAULT_MARKETPLACE_PROPERTY_NAME = MarketplaceService.class.getName() + ".url"; //$NON-NLS-1$

	/**
	 * Property key for registered IMarketplaceServiceLocator OSGi services indicating the URL of the catalog service.
	 *
	 * @see #DEFAULT_URL
	 * @see #DEFAULT_MARKETPLACE_URL
	 */
	public static final String CATALOG_URL = "catalogUrl"; //$NON-NLS-1$

	/**
	 * Same as {@link #getMarketplaceService(String) getMarketplaceService(DEFAULT_MARKETPLACE_URL)}
	 *
	 * @return a marketplace service for the {@link #DEFAULT_MARKETPLACE_URL default marketplace url}
	 */
	IMarketplaceService getDefaultMarketplaceService();

	/**
	 * Get a marketplace service for the given base url. The OSGi registry is searched for a registered instance
	 * matching the url. If none is found, a new instance is created and ued for subsequent calls.
	 */
	IMarketplaceService getMarketplaceService(String baseUrl);

	/**
	 * Get a catalog service for the default {@link #CATALOG_URL discovery url}.
	 */
	ICatalogService getCatalogService();

	/**
	 * Get a userstorage service for the given marketplace url. The OSGi registry is searched for a registered instance
	 * matching the url. If none is found, a marketplace service is located for the url and its configured storage
	 * service - which may be null - is returned.
	 */
	IMarketplaceStorageService getStorageService(String marketplaceUrl);

	/**
	 * Get a userstorage service for the default {@link #MARKETPLACE_URL marketplace url}.
	 */
	IMarketplaceStorageService getDefaultStorageService();

	/**
	 * Get the favorites service for the marketplace identified by the given url.
	 *
	 * @param marketplaceUrl
	 *            the REST base url of the marketplace for which the favorites service is requested
	 * @return a favorites service that manages the favorites for the given marketplace, or null if no such service is
	 *         registered.
	 */
	IUserFavoritesService getFavoritesService(String marketplaceUrl);

	/**
	 * Get the favorites service for the default marketplace.
	 *
	 * @return the favorites service for the default marketplace, or null if no default marketplace exists or it doesn't
	 *         support the favorites API.
	 */
	IUserFavoritesService getDefaultFavoritesService();
}
