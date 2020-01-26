/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.rest.client.compatibility;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.rest.api.CatalogsApi;
import org.eclipse.epp.mpc.rest.client.compatibility.mapping.CatalogMapper;
import org.eclipse.epp.mpc.rest.model.Catalog;
import org.mapstruct.factory.Mappers;

public class CompatibilityCatalogService implements ICatalogService {

	//TODO bind IMarketplaceRestClientFactory and init endpoints in service lifecycle
	private CatalogsApi catalogsEndpoint;

	@Override
	public List<? extends ICatalog> listCatalogs(IProgressMonitor monitor) throws CoreException {
		List<Catalog> catalogs = catalogsEndpoint.getCatalogs();
		CatalogMapper mapper = Mappers.getMapper(CatalogMapper.class);
		return mapper.mapAll(catalogs, mapper::toCatalog);
	}

}
