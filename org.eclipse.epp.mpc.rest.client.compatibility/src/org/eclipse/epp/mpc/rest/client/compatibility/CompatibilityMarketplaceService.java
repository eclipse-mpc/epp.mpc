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

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.IIdentifiable;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.rest.api.CategoriesApi;
import org.eclipse.epp.mpc.rest.api.InstallsApi;
import org.eclipse.epp.mpc.rest.api.ListingsApi;
import org.eclipse.epp.mpc.rest.api.MarketsApi;
import org.eclipse.epp.mpc.rest.model.Market;

public class CompatibilityMarketplaceService implements IMarketplaceService {

	//TODO bind IMarketplaceRestClientFactory and init endpoints in service lifecycle
	private CategoriesApi categoriesEndpoint;

	private InstallsApi installsEndpoint;

	private ListingsApi listingsEndpoint;

	private MarketsApi marketsEndpoint;

	@Override
	public URL getBaseUrl() {
		// TODO need to get this from rest.client somehow
		return null;
	}

	@Override
	public IUserFavoritesService getUserFavoritesService() {
		// TODO delegate to mpc.core
		return null;
	}

	@Override
	public List<? extends IMarket> listMarkets(IProgressMonitor monitor) throws CoreException {
		//TODO need progress monitor support - check either Apache HttpClient API (possibly async api?) or ECF implementation
		List<Market> markets = marketsEndpoint.getMarkets(null, null);
		//TODO map via mapstruct
		return null;
	}

	@Override
	public IMarket getMarket(IMarket market, IProgressMonitor monitor) throws CoreException {
		marketsEndpoint.getMarket(mapId(market));
		//TODO map via mapstruct
		return null;
	}

	private Integer mapId(IIdentifiable identifyable) throws CoreException {
		// TODO
		return null;
	}

	@Override
	public ICategory getCategory(ICategory category, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public INode getNode(INode node, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public List<INode> getNodes(Collection<? extends INode> nodes, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult search(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult tagged(String tag, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult tagged(List<String> tags, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult featured(IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult featured(IMarket market, ICategory category, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult recent(IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult favorites(IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult topFavorites(IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult popular(IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult related(List<? extends INode> basedOn, IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public ISearchResult userFavorites(IProgressMonitor monitor) throws CoreException {
		// ignore
		//TODO map via mapstruct
		return null;
	}

	@Override
	public void userFavorites(List<? extends INode> nodes, IProgressMonitor monitor) throws CoreException {
		// ignore

	}

	@Override
	public ISearchResult userFavorites(URI favoritesUri, IProgressMonitor monitor) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public List<IFavoriteList> userFavoriteLists(IProgressMonitor monitor) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public INews news(IProgressMonitor monitor) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public void reportInstallError(IStatus result, Set<? extends INode> nodes, Set<String> iuIdsAndVersions,
			String resolutionDetails, IProgressMonitor monitor) throws CoreException {
		// ignore

	}

	@Override
	public void reportInstallSuccess(INode node, IProgressMonitor monitor) {
		// ignore

	}

}
