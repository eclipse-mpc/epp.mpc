/*******************************************************************************
 * Copyright (c) 2010, 2019 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      The Eclipse Foundation - initial API and implementation
 *      Yatta Solutions - bug 397004, bug 385936, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.model.Category;
import org.eclipse.epp.internal.mpc.core.model.Market;
import org.eclipse.epp.internal.mpc.core.model.Marketplace;
import org.eclipse.epp.internal.mpc.core.model.News;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.model.NodeListing;
import org.eclipse.epp.internal.mpc.core.model.Search;
import org.eclipse.epp.internal.mpc.core.model.SearchResult;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientService;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.RequestTemplate;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.IIdentifiable;
import org.eclipse.epp.mpc.core.model.IIus;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 * @author Carsten Reckord
 */
@SuppressWarnings("deprecation")
public class DefaultMarketplaceService extends RemoteMarketplaceService<Marketplace> implements IMarketplaceService,
MarketplaceService {

//	This provisional API will be identified by /api/p at the end of most urls.
//
//	/api/p - Returns Markets + Categories
//	/node/%/api/p OR /content/%/api/p - Returns a single listing's detail
//	/taxonomy/term/%/api/p - Returns a category listing of results
//	/featured/api/p - Returns a server-defined number of featured results.
//	/recent/api/p - Returns a server-defined number of recent updates
//	/favorites/top/api/p - Returns a server-defined number of top favorites
//	/popular/top/api/p - Returns a server-defined number of most active results
//	/related/api/p - Returns a server-defined number of recommendations based on a list of nodes provided as query parameter
//	/news/api/p - Returns the news configuration details (news location/title...).
//
//	There is one exception to adding /api/p at the end and that is for search results.
//
//	/api/p/search/apachesolr_search/[query]?page=[]&filters=[] - Returns search result from the Solr Search DB.
//
//	Once we've locked down the provisional API it will likely be named api/1.

	public static final String API_FAVORITES_URI = "favorites/top"; //$NON-NLS-1$

	public static final String API_FEATURED_URI = "featured"; //$NON-NLS-1$

	public static final String API_NEWS_URI = "news"; //$NON-NLS-1$

	public static final String API_NODE_CONTENT_URI = "content"; //$NON-NLS-1$

	public static final String API_NODE_URI = "node"; //$NON-NLS-1$

	public static final String API_POPULAR_URI = "popular/top"; //$NON-NLS-1$

	public static final String API_RELATED_URI = "related"; //$NON-NLS-1$

	public static final String API_RECENT_URI = "recent"; //$NON-NLS-1$

	public static final String API_SEARCH_URI = "search/apachesolr_search/"; //$NON-NLS-1$

	public static final String API_SEARCH_URI_FULL = API_URI_SUFFIX + '/' + API_SEARCH_URI;

	public static final String API_TAXONOMY_URI = "taxonomy/term/"; //$NON-NLS-1$

	public static final String API_FREETAGGING_URI = "category/free-tagging/"; //$NON-NLS-1$

	private static final String API_ERROR_REPORT_URI = "install/error/report"; //$NON-NLS-1$

	public static final String DEFAULT_SERVICE_LOCATION = System
			.getProperty(IMarketplaceServiceLocator.DEFAULT_MARKETPLACE_PROPERTY_NAME, "http://marketplace.eclipse.org"); //$NON-NLS-1$

	public static final URL DEFAULT_SERVICE_URL;

	/**
	 * parameter identifying client
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_CLIENT = "client"; //$NON-NLS-1$

	/**
	 * parameter identifying operating system as reported by {@link org.eclipse.core.runtime.Platform#getOS()}
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_OS = "os"; //$NON-NLS-1$

	/**
	 * parameter identifying the Eclipse platform version (the version of the org.eclipse.platform bundle) This
	 * parameter is optional and only sent if the platform bundle is present. It is used to identify the actual running
	 * platform's version (esp. where different platforms share the same runtime, like the parallel 3.x/4.x versions)
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_PLATFORM_VERSION = "platform.version"; //$NON-NLS-1$

	/**
	 * parameter identifying the Eclipse product version
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_PRODUCT_VERSION = "product.version"; //$NON-NLS-1$

	/**
	 * parameter identifying the product id, as provided by <code>Platform.getProduct().getId()</code>
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_PRODUCT = "product"; //$NON-NLS-1$

	/**
	 * parameter identifying a list of nodes for a {@link #related(List, IProgressMonitor)} query
	 */
	public static final String PARAM_BASED_ON_NODES = "nodes"; //$NON-NLS-1$

	static {
		DEFAULT_SERVICE_URL = ServiceUtil.parseUrl(DEFAULT_SERVICE_LOCATION);
	}

	private IUserFavoritesService userFavoritesService;

	private HttpClientService httpClient;

	public DefaultMarketplaceService(URL baseUrl) {
		this.baseUrl = baseUrl == null ? DEFAULT_SERVICE_URL : baseUrl;
	}

	public DefaultMarketplaceService() {
		this(null);
	}

	@Override
	public URL getBaseUrl() {
		return super.getBaseUrl();
	}

	@Override
	public List<Market> listMarkets(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_URI_SUFFIX, monitor);
		return marketplace.getMarket();
	}

	@Override
	public Market getMarket(IMarket market, IProgressMonitor monitor) throws CoreException {
		if (market.getId() == null && market.getUrl() == null) {
			throw new IllegalArgumentException();
		}
		List<Market> markets = listMarkets(monitor);
		if (market.getId() != null) {
			String marketId = market.getId();
			for (Market aMarket : markets) {
				if (marketId.equals(aMarket.getId())) {
					return aMarket;
				}
			}
			throw new CoreException(
					createErrorStatus(Messages.DefaultMarketplaceService_marketNotFound, market.getId()));
		} else { // (market.getUrl() != null)
			String marketUrl = market.getUrl();
			for (Market aMarket : markets) {
				if (marketUrl.equals(aMarket.getUrl())) {
					return aMarket;
				}
			}
			throw new CoreException(
					createErrorStatus(Messages.DefaultMarketplaceService_marketNotFound, market.getUrl()));
		}
	}

	@Override
	public Market getMarket(Market market, IProgressMonitor monitor) throws CoreException {
		return getMarket((IMarket) market, monitor);
	}

	@Override
	public Category getCategory(ICategory category, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 200);
		if (category.getId() != null && category.getUrl() == null) {
			List<Market> markets = listMarkets(progress.newChild(50));
			ICategory resolvedCategory = null;
			outer: for (Market market : markets) {
				List<Category> categories = market.getCategory();
				for (Category aCategory : categories) {
					if (aCategory.equalsId(category)) {
						resolvedCategory = aCategory;
						break outer;
					}
				}
			}
			if (progress.isCanceled()) {
				throw new OperationCanceledException();
			} else if (resolvedCategory == null) {
				throw new CoreException(
						createErrorStatus(Messages.DefaultMarketplaceService_categoryNotFound, category.getId()));
			} else {
				return getCategory(resolvedCategory, progress.newChild(150));
			}
		}
		Marketplace marketplace = processRequest(category.getUrl(), API_URI_SUFFIX, progress.newChild(200));
		if (marketplace.getCategory().isEmpty()) {
			throw new CoreException(
					createErrorStatus(Messages.DefaultMarketplaceService_categoryNotFound, category.getUrl()));
		} else if (marketplace.getCategory().size() > 1) {
			throw new CoreException(
					createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, category.getUrl()));
		}
		Category resolvedCategory = marketplace.getCategory().get(0);
		return resolvedCategory;
	}

	@Override
	public Category getCategory(Category category, IProgressMonitor monitor) throws CoreException {
		return getCategory((ICategory) category, monitor);
	}

	@Override
	public Node getNode(INode node, IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace;
		String query;
		if (node.getId() != null) {
			// bug 304928: prefer the id method rather than the URL, since the id provides a stable URL and the
			// URL is based on the name, which could change.
			query = node.getId();
			String encodedId = urlEncode(node.getId());
			marketplace = processRequest(API_NODE_URI + '/' + encodedId + '/' + API_URI_SUFFIX, monitor);
		} else {
			query = node.getUrl();
			marketplace = processRequest(node.getUrl(), API_URI_SUFFIX, monitor);
		}
		if (marketplace.getNode().isEmpty()) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_nodeNotFound, query));
		} else if (marketplace.getNode().size() > 1) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, query));
		}
		Node resolvedNode = marketplace.getNode().get(0);
		return resolvedNode;
	}

	@Override
	public Node getNode(Node node, IProgressMonitor monitor) throws CoreException {
		return getNode((INode) node, monitor);
	}

	@Override
	public List<INode> getNodes(Collection<? extends INode> nodes, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DefaultMarketplaceService_getNodesProgress, nodes.size());
		if (nodes.isEmpty()) {
			return new ArrayList<>();
		}
		List<INode> nodesById = null;
		List<INode> nodesByUrl = null;
		for (INode node : nodes) {
			if (node.getId() == null && node.getUrl() == null) {
				throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_invalidNode, node));
			}
			if (node.getUrl() != null) {
				if (nodesByUrl == null) {
					nodesByUrl = new ArrayList<>();
				}
				nodesByUrl.add(node);
			}
			if (node.getId() != null) {
				if (nodesById == null) {
					nodesById = new ArrayList<>(nodes.size());
				}
				nodesById.add(node);
			}
		}
		Map<INode, INode> resolvedNodeMapping = new HashMap<>(nodes.size());
		Map<INode, CoreException> resolutionErrors = new HashMap<>(2);
		if (nodesById != null) {
			getNodesById(nodesById, resolvedNodeMapping, progress.newChild(nodesById.size()));
		}
		if (nodesByUrl != null) {
			getNodesByUrl(nodesByUrl, resolvedNodeMapping, resolutionErrors, progress.newChild(nodesByUrl.size()));
		}

		List<INode> resultNodes = new ArrayList<>(nodes.size());
		MultiStatus missingNodes = null;
		for (INode inputNode : nodes) {
			INode resolvedNode = resolvedNodeMapping.get(inputNode);
			if (resolvedNode != null) {
				resultNodes.add(resolvedNode);
			} else {
				String query;
				if (inputNode.getId() != null) {
					query = inputNode.getId();
				} else {
					query = inputNode.getUrl();
				}
				CoreException error = resolutionErrors.get(inputNode);
				IStatus missingNodeDetailStatus = error == null
						? createStatus(IStatus.INFO, Messages.DefaultMarketplaceService_nodeNotFound, query)
								: createStatus(IStatus.INFO, Messages.DefaultMarketplaceService_nodeNotFound, query, error);
				if (missingNodes == null) {
					missingNodes = new MultiStatus(MarketplaceClientCore.BUNDLE_ID, 0,
							"Some entries could not be found on the Marketplace", null); //$NON-NLS-1$
				}
				missingNodes.add(missingNodeDetailStatus);
			}
		}
		if (missingNodes != null) {
			MarketplaceClientCore.getLog().log(missingNodes);
		}
		return resultNodes;
	}

	private void getNodesById(Collection<? extends INode> nodes, Map<INode, INode> resolvedNodeMapping,
			IProgressMonitor monitor) throws CoreException {
		StringBuilder nodeIdQuery = new StringBuilder();
		Map<String, INode> nodeIds = new HashMap<>(nodes.size());
		for (INode node : nodes) {
			if (node.getId() == null) {
				continue;
			}
			nodeIds.put(node.getId(), node);
			String encodedId = urlEncode(node.getId());
			if (nodeIdQuery.length() > 0) {
				nodeIdQuery.append(","); //$NON-NLS-1$
			}
			nodeIdQuery.append(encodedId);
		}
		Marketplace marketplace = processRequest(API_NODE_URI + '/' + nodeIdQuery + '/' + API_URI_SUFFIX, monitor);
		List<Node> resolvedNodes = marketplace.getNode();
		for (Node node : resolvedNodes) {
			INode inputNode = nodeIds.get(node.getId());
			if (inputNode != null) {
				resolvedNodeMapping.put(inputNode, node);
			} else {
				throw new CoreException(
						createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, nodeIdQuery));
			}
		}
	}

	private void getNodesByUrl(Collection<? extends INode> nodes, Map<INode, INode> resolvedNodeMapping,
			Map<INode, CoreException> resolutionErrors, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, nodes.size());
		int remaining = nodes.size();
		for (INode node : nodes) {
			if (node.getUrl() != null && !resolvedNodeMapping.containsKey(node)) {
				try {
					Node resolvedNode = getNode(node, progress.newChild(1));
					resolvedNodeMapping.put(node, resolvedNode);
				} catch (CoreException ex) {
					resolutionErrors.put(node, ex);
				}
			}
			progress.setWorkRemaining(--remaining);
		}
	}

	@Override
	public SearchResult search(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		String relativeUrl = computeRelativeSearchUrl(market, category, queryText, true);
		return processSearchRequest(relativeUrl, queryText, monitor);
	}

	@Override
	public SearchResult search(Market market, Category category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		return search((IMarket) market, (ICategory) category, queryText, monitor);
	}

	/**
	 * Creates the query URL for the Marketplace REST API.
	 * <p>
	 * If the query string is non-empty, the format for the returned relative URL is
	 * <code>search/apachesolr_search/[query]?filters=[filters]</code> where [query] is the URL encoded query string and
	 * [filters] are the category and market IDs (category first for browser urls, market first for API urls). If both
	 * market and category are null, the filters are omitted completely.
	 * <p>
	 * If the query is empty and either market or category are not null, the format for the relative URL is
	 * <code>taxonomy/term/[filters]</code> where [filters] is the comma-separated list of category and market, in that
	 * order.
	 * <p>
	 * If the query is empty and both category and market are null, the result is null
	 *
	 * @param market
	 *            the market to search or null
	 * @param category
	 *            the category to search or null
	 * @param queryText
	 *            the search query
	 * @param api
	 *            true to create REST API url, false for browser url
	 * @return the relative search url, e.g.
	 *         <code>api/p/search/apachesolr_search/WikiText?filters=tid:38%20tid:31</code> or
	 *         <code>taxonomy/term/38,31/api/p</code>
	 */
	public String computeRelativeSearchUrl(IMarket market, ICategory category, String queryText, boolean api) {
		String relativeUrl;
		if (queryText != null && queryText.trim().length() > 0) {
			relativeUrl = (api ? API_SEARCH_URI_FULL : API_SEARCH_URI) + urlEncode(queryText.trim());
			String queryString = ""; //$NON-NLS-1$
			if (market != null || category != null) {
				queryString += "filters="; //$NON-NLS-1$
				IIdentifiable first = api ? market : category;
				IIdentifiable second = api ? category : market;
				if (first != null) {
					queryString += "tid:" + urlEncode(first.getId()); //$NON-NLS-1$
					if (second != null) {
						queryString += "%20"; //$NON-NLS-1$
					}
				}
				if (second != null) {
					queryString += "tid:" + urlEncode(second.getId()); //$NON-NLS-1$
				}
			}
			if (queryString.length() > 0) {
				relativeUrl += '?' + queryString;
			}
		} else if (market != null || category != null) {
			// http://marketplace.eclipse.org/taxonomy/term/38,31
			relativeUrl = API_TAXONOMY_URI;
			if (category != null) {
				relativeUrl += urlEncode(category.getId());
				if (market != null) {
					relativeUrl += ',';
				}
			}
			if (market != null) {
				relativeUrl += urlEncode(market.getId());
			}
			if (api) {
				relativeUrl += '/' + API_URI_SUFFIX;
			}
		} else {
			relativeUrl = null;
		}
		return relativeUrl;
	}

	private SearchResult processSearchRequest(String relativeUrl, String queryText, IProgressMonitor monitor)
			throws CoreException {
		SearchResult result = new SearchResult();
		if (relativeUrl == null) {
			// empty search
			result.setMatchCount(0);
			result.setNodes(new ArrayList<Node>());
		} else {
			Marketplace marketplace;
			try {
				marketplace = processRequest(relativeUrl, monitor);
			} catch (CoreException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof FileNotFoundException) {
					throw new CoreException(createErrorStatus(
							NLS.bind(Messages.DefaultMarketplaceService_UnsupportedSearchString, queryText), cause));
				}
				throw ex;
			}
			Search search = marketplace.getSearch();
			if (search != null) {
				result.setMatchCount(search.getCount());
				result.setNodes(search.getNode());
			} else if (marketplace.getCategory().size() == 1) {
				Category category = marketplace.getCategory().get(0);
				result.setMatchCount(category.getNode().size());
				result.setNodes(category.getNode());
			} else {
				throw new CoreException(
						createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, relativeUrl));
			}
		}
		return result;
	}

	@Override
	public SearchResult tagged(String tag, IProgressMonitor monitor) throws CoreException {
		return processSearchRequest(API_FREETAGGING_URI + URLUtil.urlEncode(tag) + '/' + API_URI_SUFFIX, tag, monitor);
	}

	@Override
	public SearchResult tagged(List<String> tags, IProgressMonitor monitor) throws CoreException {
		String combinedTags = tags.stream().collect(Collectors.joining(",")); //$NON-NLS-1$
		return tagged(combinedTags, monitor);
	}

	@Override
	public SearchResult featured(IProgressMonitor monitor) throws CoreException {
		return featured(null, null, monitor);
	}

	@Override
	public SearchResult featured(IMarket market, ICategory category, IProgressMonitor monitor) throws CoreException {
		String nodePart = ""; //$NON-NLS-1$
		if (market != null) {
			nodePart += urlEncode(market.getId());
		}
		if (category != null) {
			if (nodePart.length() > 0) {
				nodePart += ","; //$NON-NLS-1$
			}
			nodePart += urlEncode(category.getId());
		}
		String uri = API_FEATURED_URI + '/';
		if (nodePart.length() > 0) {
			uri += nodePart + '/';
		}
		Marketplace marketplace = processRequest(uri + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getFeatured());
	}

	@Override
	public SearchResult featured(IProgressMonitor monitor, Market market, Category category) throws CoreException {
		return featured(market, category, monitor);
	}

	@Override
	public SearchResult recent(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_RECENT_URI + '/' + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getRecent());
	}

	/**
	 * @deprecated use {@link #topFavorites(IProgressMonitor)} instead
	 */
	@Override
	@Deprecated
	public SearchResult favorites(IProgressMonitor monitor) throws CoreException {
		return topFavorites(monitor);
	}

	@Override
	public SearchResult topFavorites(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_FAVORITES_URI + '/' + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getFavorites());
	}

	@Override
	public List<IFavoriteList> userFavoriteLists(IProgressMonitor monitor) throws CoreException {
		IUserFavoritesService userFavoritesService = getUserFavoritesService();
		if (userFavoritesService == null) {
			throw new UnsupportedOperationException();
		}
		try {
			return userFavoritesService.getRandomFavoriteLists(monitor);
		} catch (Exception e) {
			throw new CoreException(MarketplaceClientCore.computeStatus(e,
					Messages.DefaultMarketplaceService_FavoritesErrorRetrieving));
		}
	}

	@Override
	public ISearchResult userFavorites(IProgressMonitor monitor) throws CoreException, NotAuthorizedException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DefaultMarketplaceService_FavoritesRetrieve, 10000);
		IUserFavoritesService userFavoritesService = getUserFavoritesService();
		if (userFavoritesService == null) {
			throw new UnsupportedOperationException();
		}
		final List<INode> favorites;
		try {
			favorites = userFavoritesService.getFavorites(progress.newChild(1000));
		} catch (NotAuthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(MarketplaceClientCore.computeStatus(e, Messages.DefaultMarketplaceService_FavoritesErrorRetrieving));
		}
		progress.setWorkRemaining(9000);
		return resolveFavoriteNodes(favorites, progress.newChild(9000), true);
	}

	@Override
	public ISearchResult userFavorites(URI favoritesUri, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DefaultMarketplaceService_FavoritesRetrieve, 10000);
		IUserFavoritesService userFavoritesService = getUserFavoritesService();
		if (userFavoritesService == null) {
			throw new UnsupportedOperationException();
		}
		final List<INode> favorites;
		try {
			favorites = userFavoritesService.getFavorites(favoritesUri, progress.newChild(1000));
		} catch (Exception e) {
			throw new CoreException(MarketplaceClientCore.computeStatus(e,
					Messages.DefaultMarketplaceService_FavoritesErrorRetrieving));
		}
		progress.setWorkRemaining(9000);
		return resolveFavoriteNodes(favorites, progress.newChild(9000), false);
	}

	@Override
	public void userFavorites(List<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DefaultMarketplaceService_FavoritesUpdate, 10000);
		IUserFavoritesService userFavoritesService = getUserFavoritesService();
		if (userFavoritesService == null) {
			throw new UnsupportedOperationException();
		}
		if (nodes == null || nodes.isEmpty()) {
			return;
		}
		Set<String> favorites = null;
		try {
			favorites = userFavoritesService == null ? null : userFavoritesService.getFavoriteIds(progress);
		} catch (NotAuthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(MarketplaceClientCore.computeStatus(e, Messages.DefaultMarketplaceService_FavoritesErrorRetrieving));
		} finally {
			for (INode node : nodes) {
				((Node) node).setUserFavorite(favorites == null ? null : favorites.contains(node.getId()));
			}
		}
	}

	private ISearchResult resolveFavoriteNodes(final List<INode> nodes, IProgressMonitor monitor, boolean filterIncompatible) throws CoreException {
		IMarketplaceService resolveService = this;
		IMarketplaceService registeredService = ServiceHelper.getMarketplaceServiceLocator()
				.getMarketplaceService(this.getBaseUrl().toString());
		if (registeredService instanceof CachingMarketplaceService) {
			CachingMarketplaceService cachingService = (CachingMarketplaceService) registeredService;
			if (cachingService.getDelegate() == this) {
				resolveService = cachingService;
			}
		}
		final List<INode> resolvedNodes = resolveService.getNodes(nodes, monitor);
		for (ListIterator<INode> i = resolvedNodes.listIterator(); i.hasNext();) {
			INode resolved = i.next();
			((Node) resolved).setUserFavorite(true);
			if (filterIncompatible && !isInstallable(resolved)) {
				i.remove();
			}
		}
		if (!filterIncompatible) {
			//sort the node list so uninstallable nodes come last
			Collections.sort(resolvedNodes, (n1, n2) -> {
				if (n1 == n2) {
					return 0;
				}
				boolean n1Installable = isInstallable(n1);
				boolean n2Installable = isInstallable(n2);
				if (n1Installable == n2Installable) {
					return 0;
				}
				if (n1Installable) { // && !n2Installable
					return -1;
				}
				// !n1Installable && n2Installable
				return 1;
			});
		}

		return new ISearchResult() {

			@Override
			public List<? extends INode> getNodes() {
				return resolvedNodes;
			}

			@Override
			public Integer getMatchCount() {
				return resolvedNodes.size();
			}
		};
	}

	private boolean isInstallable(INode resolved) {
		IIus ius = resolved.getIus();
		return ius != null && !ius.getIuElements().isEmpty();
	}

	@Override
	public SearchResult popular(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_POPULAR_URI + '/' + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getPopular());
	}

	@Override
	public SearchResult related(List<? extends INode> basedOn, IProgressMonitor monitor) throws CoreException {
		String basedOnQuery = ""; //$NON-NLS-1$
		if (basedOn != null && !basedOn.isEmpty()) {
			StringBuilder sb = new StringBuilder().append('?').append(PARAM_BASED_ON_NODES).append('=');
			boolean first = true;
			for (INode node : basedOn) {
				if (!first) {
					sb.append('+');
				}
				sb.append(node.getId());
				first = false;
			}
			basedOnQuery = sb.toString();
		}
		Marketplace marketplace = processRequest(API_RELATED_URI + '/' + API_URI_SUFFIX + basedOnQuery, monitor);
		return createSearchResult(marketplace.getRelated());
	}

	protected SearchResult createSearchResult(NodeListing nodeList) throws CoreException {
		if (nodeList == null) {
			throw new CoreException(
					createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, Messages.DefaultMarketplaceService_nullResultNodes));
		}
		SearchResult result = new SearchResult();
		result.setMatchCount(nodeList.getCount());
		result.setNodes(nodeList.getNode());
		return result;
	}

	@Override
	public News news(IProgressMonitor monitor) throws CoreException {
		try {
			Marketplace marketplace = processRequest(API_NEWS_URI + '/' + API_URI_SUFFIX, monitor);
			return marketplace.getNews();
		} catch (CoreException ex) {
			final Throwable cause = ex.getCause();
			if (cause instanceof FileNotFoundException) {
				// optional news API not supported
				return null;
			}
			throw ex;
		}
	}

	/**
	 * @deprecated use {@link #reportInstallError(IStatus, Set, Set, String, IProgressMonitor)} instead
	 */
	@Override
	@Deprecated
	public void reportInstallError(IProgressMonitor monitor, IStatus result, Set<Node> nodes,
			Set<String> iuIdsAndVersions, String resolutionDetails) throws CoreException {
		reportInstallError(result, nodes, iuIdsAndVersions, resolutionDetails, monitor);
	}

	@Override
	public void reportInstallError(IStatus result, Set<? extends INode> nodes, Set<String> iuIdsAndVersions,
			String resolutionDetails, IProgressMonitor monitor) throws CoreException {
		try {
			List<NameValuePair> parameters = new ArrayList<>();

			Map<String, String> requestMetaParameters = getRequestMetaParameters();
			for (Map.Entry<String, String> metaParam : requestMetaParameters.entrySet()) {
				if (metaParam.getKey() != null) {
					parameters.add(new BasicNameValuePair(metaParam.getKey(), metaParam.getValue()));
				}
			}

			parameters.add(new BasicNameValuePair("status", Integer.toString(result.getSeverity()))); //$NON-NLS-1$
			parameters.add(new BasicNameValuePair("statusMessage", result.getMessage())); //$NON-NLS-1$
			for (INode node : nodes) {
				parameters.add(new BasicNameValuePair("node", node.getId())); //$NON-NLS-1$
			}
			if (iuIdsAndVersions != null && !iuIdsAndVersions.isEmpty()) {
				for (String iuAndVersion : iuIdsAndVersions) {
					parameters.add(new BasicNameValuePair("iu", iuAndVersion)); //$NON-NLS-1$
				}
			}
			parameters.add(new BasicNameValuePair("detailedMessage", resolutionDetails)); //$NON-NLS-1$
			if (!parameters.isEmpty()) {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
				new RequestTemplate<Void>() {

					@Override
					protected ClassicHttpRequest createRequest(URI uri) {
						return ClassicRequestBuilder.post(uri.resolve(API_ERROR_REPORT_URI))
								.setEntity(entity)
								.build();
					}

					@Override
					protected Void handleResponseStream(InputStream content, Charset charset) throws IOException {
						// ignore
						return null;
					}

				}.execute(httpClient, baseUrl.toURI());
			}
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			String message = NLS.bind(Messages.DefaultMarketplaceService_cannotCompleteRequest_reason,
					baseUrl.toString() + API_ERROR_REPORT_URI, e.getMessage());
			throw new CoreException(createErrorStatus(message, e));
		}
	}

	@Override
	public void reportInstallSuccess(INode node, IProgressMonitor monitor) {
		String url = node.getUrl();
		if (!url.endsWith("/")) { //$NON-NLS-1$
			url += "/"; //$NON-NLS-1$
		}
		url += "success"; //$NON-NLS-1$
		url = addMetaParameters(url);
		try (InputStream stream = transport.stream(new URI(url), monitor)) {
			while (stream.read() != -1) {
				// nothing to do
			}
		} catch (Throwable e) {
			//per bug 314028 logging this error is not useful.
		}
	}

	@Override
	public IUserFavoritesService getUserFavoritesService() {
		return userFavoritesService;
	}

	public void setUserFavoritesService(IUserFavoritesService userFavoritesService) {
		this.userFavoritesService = userFavoritesService;
	}

	public void setHttpClient(HttpClientService httpClient) {
		this.httpClient = httpClient;
	}

	public HttpClientService getHttpClient() {
		return httpClient;
	}
}
