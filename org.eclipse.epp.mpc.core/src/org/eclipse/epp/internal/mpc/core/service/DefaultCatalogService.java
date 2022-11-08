/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.model.Catalog;
import org.eclipse.epp.internal.mpc.core.model.CatalogBranding;
import org.eclipse.epp.internal.mpc.core.model.Catalogs;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;

public class DefaultCatalogService extends RemoteMarketplaceService<Catalogs> implements ICatalogService {

	public static final String DEFAULT_CATALOG_SERVICE_LOCATION = System.getProperty(DefaultCatalogService.class.getName()
			+ ".url", "https://marketplace.eclipse.org"); //$NON-NLS-1$//$NON-NLS-2$

	public static final URL DEFAULT_CATALOG_SERVICE_URL;

	static {
		DEFAULT_CATALOG_SERVICE_URL = ServiceUtil.parseUrl(DEFAULT_CATALOG_SERVICE_LOCATION);
	}

	public DefaultCatalogService() {
		this(null);
	}

	public DefaultCatalogService(URL baseUrl) {
		this.baseUrl = baseUrl == null ? DEFAULT_CATALOG_SERVICE_URL : baseUrl;
	}

	@Override
	public List<? extends ICatalog> listCatalogs(IProgressMonitor monitor) throws CoreException {
		Catalogs result = processRequest("catalogs/" + API_URI_SUFFIX, monitor); //$NON-NLS-1$
		List<Catalog> catalogs = result.getCatalogs();
		for (Catalog catalog : catalogs) {
			registerDynamicFavoritesService(catalog);
		}
		return catalogs;
	}

	private void registerDynamicFavoritesService(Catalog catalog) {
		CatalogBranding branding = catalog.getBranding();
		if (branding == null) {
			return;
		}
		if (!branding.hasFavoritesTab()) {
			return;
		}
		String favoritesServer = branding.getFavoritesServer();
		if (favoritesServer != null && !"".equals(favoritesServer.trim())) { //$NON-NLS-1$
			registerDynamicFavoritesService(catalog.getUrl(), favoritesServer.trim(), branding.getFavoritesApiKey());
		}
	}

	private void registerDynamicFavoritesService(String catalogUrl, String favoritesApiServer, String favoritesApiKey) {
		IMarketplaceServiceLocator marketplaceServiceLocator = ServiceHelper.getMarketplaceServiceLocator();
		IUserFavoritesService favoritesService = marketplaceServiceLocator.getFavoritesService(catalogUrl);
		if (favoritesService != null) {
			return;
		}
		((ServiceLocator) marketplaceServiceLocator).registerFavoritesService(catalogUrl, favoritesApiServer,
				favoritesApiKey);
	}
}
