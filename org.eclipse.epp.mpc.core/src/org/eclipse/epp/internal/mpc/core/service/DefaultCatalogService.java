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
package org.eclipse.epp.internal.mpc.core.service;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.model.Catalogs;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.service.ICatalogService;

public class DefaultCatalogService extends RemoteMarketplaceService<Catalogs> implements ICatalogService {

	public static final String DEFAULT_CATALOG_SERVICE_LOCATION = System.getProperty(DefaultCatalogService.class.getName()
			+ ".url", "http://marketplace.eclipse.org"); //$NON-NLS-1$//$NON-NLS-2$

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

	public List<? extends ICatalog> listCatalogs(IProgressMonitor monitor) throws CoreException {
		Catalogs result = processRequest("catalogs/" + API_URI_SUFFIX, monitor); //$NON-NLS-1$
		return result.getCatalogs();
	}
}
