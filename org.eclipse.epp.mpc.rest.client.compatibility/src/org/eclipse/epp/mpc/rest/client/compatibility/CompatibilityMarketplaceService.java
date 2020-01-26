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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.IIdentifiable;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.rest.api.CatalogsApi;
import org.eclipse.epp.mpc.rest.api.CategoriesApi;
import org.eclipse.epp.mpc.rest.api.InstallsApi;
import org.eclipse.epp.mpc.rest.api.ListingQuery;
import org.eclipse.epp.mpc.rest.api.ListingsApi;
import org.eclipse.epp.mpc.rest.api.MarketsApi;
import org.eclipse.epp.mpc.rest.api.PagingInfo;
import org.eclipse.epp.mpc.rest.api.PlatformInfo;
import org.eclipse.epp.mpc.rest.api.RequestHelper;
import org.eclipse.epp.mpc.rest.api.SortingInfo;
import org.eclipse.epp.mpc.rest.client.compatibility.mapping.CatalogMapper;
import org.eclipse.epp.mpc.rest.client.compatibility.mapping.CategoryMapper;
import org.eclipse.epp.mpc.rest.client.compatibility.mapping.MarketMapper;
import org.eclipse.epp.mpc.rest.client.compatibility.mapping.NodeMapper;
import org.eclipse.epp.mpc.rest.client.compatibility.util.CoreFunctions;
import org.eclipse.epp.mpc.rest.model.Catalog;
import org.eclipse.epp.mpc.rest.model.Category;
import org.eclipse.epp.mpc.rest.model.Listing;
import org.eclipse.epp.mpc.rest.model.Market;
import org.eclipse.epp.mpc.rest.model.SortWhitelist;
import org.mapstruct.factory.Mappers;

public class CompatibilityMarketplaceService implements IMarketplaceService {

	//TODO bind IMarketplaceRestClientFactory and init endpoints in service lifecycle
	private CategoriesApi categoriesEndpoint;

	private InstallsApi installsEndpoint;

	private ListingsApi listingsEndpoint;

	private MarketsApi marketsEndpoint;

	private CatalogsApi catalogsEndpoint;

	private final RequestHelper requestHelper = RequestHelper.of()
			.withDefaultPlatform(platformInfo())
			.withDefaultPage(pagingInfo());

	private Integer catalogId;

	private PagingInfo pagingInfo() {
		// TODO
		return null;
	}

	private PlatformInfo platformInfo() {
		// TODO
		return null;
	}

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
		MarketMapper mapper = Mappers.getMapper(MarketMapper.class);
		return mapper.mapAll(markets, mapper::toMarket);
	}

	@Override
	public IMarket getMarket(IMarket market, IProgressMonitor monitor) throws CoreException {
		Market result = marketsEndpoint.getMarket(mapId(market));
		MarketMapper mapper = Mappers.getMapper(MarketMapper.class);
		return mapper.toMarket(result);
	}

	@Override
	public ICategory getCategory(ICategory category, IProgressMonitor monitor) throws CoreException {
		Category result = categoriesEndpoint.getCategory(mapId(category));
		CategoryMapper mapper = Mappers.getMapper(CategoryMapper.class);
		return mapper.toCategory(result);
	}

	@Override
	public INode getNode(INode node, IProgressMonitor monitor) throws CoreException {
		Listing listing = requestHelper.getListing(listingsEndpoint, mapId(node));
		NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
		return nodeMapper.toNode(listing);
	}

	@Override
	public List<INode> getNodes(Collection<? extends INode> nodes, IProgressMonitor monitor) throws CoreException {
		//TODO some nodes might be identified by URL instead of id...
		List<Integer> nodeIds = CoreFunctions.unwrap(() -> nodes.stream()
				.map(CoreFunctions.wrap(node -> mapId(node)))
				.filter(id -> id != null)
				.collect(Collectors.toList()));
		ListingQuery query = ListingQuery.builder().addAllIds(nodeIds).build();
		List<Listing> listings = requestHelper.getListings(listingsEndpoint, query);
		NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
		return nodeMapper.mapAll(listings, nodeMapper::toNode);
	}

	@Override
	public ISearchResult search(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		ListingQuery query = ListingQuery.builder()
				.marketId(Optional.ofNullable(market).map(CoreFunctions.wrap(id -> mapId(id))))
				.categoryId(Optional.ofNullable(category).map(CoreFunctions.wrap(id -> mapId(id))))
				.query(Optional.ofNullable(queryText))
				.build();
		return search(query, monitor);
	}

	@Override
	public ISearchResult tagged(String tag, IProgressMonitor monitor) throws CoreException {
		return search(ListingQuery.builder().addTags(tag).build(), monitor);
	}

	@Override
	public ISearchResult tagged(List<String> tags, IProgressMonitor monitor) throws CoreException {
		ListingQuery query = ListingQuery.builder().tags(tags).build();
		return search(query, monitor);
	}

	private ISearchResult search(ListingQuery query, IProgressMonitor monitor) {
		List<Listing> listings = requestHelper.getListings(listingsEndpoint, query);
		NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
		return nodeMapper.toSearchResult(listings);
	}

	@Override
	public ISearchResult featured(IProgressMonitor monitor) throws CoreException {
		return searchSorted(SortingInfo.featuredInstance(), monitor);
	}

	@Override
	public ISearchResult featured(IMarket market, ICategory category, IProgressMonitor monitor) throws CoreException {
		ListingQuery query = ListingQuery.builder()
				.marketId(Optional.ofNullable(market).map(CoreFunctions.wrap(id -> mapId(id))))
				.categoryId(Optional.ofNullable(category).map(CoreFunctions.wrap(id -> mapId(id))))
				.build();
		List<Listing> listings = requestHelper.getListings(listingsEndpoint, query, SortingInfo.featuredInstance());
		NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
		return nodeMapper.toSearchResult(listings);
	}

	@Override
	public ISearchResult recent(IProgressMonitor monitor) throws CoreException {
		return searchSorted(SortingInfo.sortedInstance(SortWhitelist.CHANGED), monitor);
	}

	private ISearchResult searchSorted(SortingInfo sort, IProgressMonitor monitor) {
		List<Listing> listings = requestHelper.getListings(listingsEndpoint, null, sort);
		NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
		return nodeMapper.toSearchResult(listings);
	}

	@Override
	@Deprecated
	public ISearchResult favorites(IProgressMonitor monitor) throws CoreException {
		return topFavorites(monitor);
	}

	@Override
	public ISearchResult topFavorites(IProgressMonitor monitor) throws CoreException {
		return searchSorted(SortingInfo.sortedInstance(SortWhitelist.FAVORITE_COUNT), monitor);
	}

	@Override
	public ISearchResult popular(IProgressMonitor monitor) throws CoreException {
		return searchSorted(SortingInfo.sortedInstance(SortWhitelist.INSTALL_COUNT_RECENT), monitor);
	}

	@Override
	public ISearchResult related(List<? extends INode> basedOn, IProgressMonitor monitor) throws CoreException {
		throw new CoreException(MarketplaceClientCore.computeStatus(new UnsupportedOperationException(),
				"Related listing queries are not supported by this marketplace"));
	}

	@Override
	public ISearchResult userFavorites(IProgressMonitor monitor) throws CoreException {
		//TODO use mpc.core
		return null;
	}

	@Override
	public void userFavorites(List<? extends INode> nodes, IProgressMonitor monitor) throws CoreException {
		// TODO use mpc.core
	}

	@Override
	public ISearchResult userFavorites(URI favoritesUri, IProgressMonitor monitor) throws CoreException {
		// TODO use mpc.core
		return null;
	}

	@Override
	public List<IFavoriteList> userFavoriteLists(IProgressMonitor monitor) throws CoreException {
		// TODO use mpc.core
		return null;
	}

	@Override
	public INews news(IProgressMonitor monitor) throws CoreException {
		Catalog catalog = catalogsEndpoint.getCatalog(catalogId);
		CatalogMapper mapper = Mappers.getMapper(CatalogMapper.class);
		return mapper.toNews(catalog);
	}

	@Override
	public void reportInstallError(IStatus result, Set<? extends INode> nodes, Set<String> iuIdsAndVersions,
			String resolutionDetails, IProgressMonitor monitor) throws CoreException {
		// TODO
	}

	@Override
	public void reportInstallSuccess(INode node, IProgressMonitor monitor) {
		try {
			installsEndpoint.postInstall(mapId(node), mapVersion(node));
		} catch (Exception ex) {
			// ignore
		}
	}

	private Integer mapId(IIdentifiable identifyable) throws CoreException {
		try {
			return Integer.parseInt(identifyable.getId());
		} catch (Exception ex) {
			throw new CoreException(MarketplaceClientCore.computeStatus(ex, MessageFormat.format(
					"Malformed Id: Id {0} of element {1} cannot be parsed", identifyable.getId(), identifyable)));
		}
	}

	private Float mapVersion(INode node) {
		String numericVersion = node.getVersion().replaceAll("[^0-9.]", ""); //$NON-NLS-1$//$NON-NLS-2$
		int firstDot = numericVersion.indexOf('.');
		if (firstDot != -1) {
			numericVersion = numericVersion.substring(0, firstDot + 1)
					+ numericVersion.substring(firstDot + 1).replaceAll("\\.", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			return Float.parseFloat(numericVersion);
		} catch (NumberFormatException ex) {
			// ignore
			return null;
		}
	}

}
