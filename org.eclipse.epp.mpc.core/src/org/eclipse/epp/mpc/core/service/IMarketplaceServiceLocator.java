/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

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

}