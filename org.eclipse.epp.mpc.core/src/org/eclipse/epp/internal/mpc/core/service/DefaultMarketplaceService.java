/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.util.HttpUtil;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IIdentifiable;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
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

	public static final String API_RECENT_URI = "recent"; //$NON-NLS-1$

	public static final String API_SEARCH_URI = "search/apachesolr_search/"; //$NON-NLS-1$

	public static final String API_SEARCH_URI_FULL = API_URI_SUFFIX + '/' + API_SEARCH_URI;

	public static final String API_TAXONOMY_URI = "taxonomy/term/"; //$NON-NLS-1$

	public static final String DEFAULT_SERVICE_LOCATION = System.getProperty(MarketplaceService.class.getName()
			+ ".url", "http://marketplace.eclipse.org"); //$NON-NLS-1$//$NON-NLS-2$

	public static final URL DEFAULT_SERVICE_URL;

	/**
	 * parameter identifying client
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_CLIENT = "client"; //$NON-NLS-1$

	/**
	 * parameter identifying windowing system as reported by {@link org.eclipse.core.runtime.Platform#getWS()}
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_WS = "ws"; //$NON-NLS-1$

	/**
	 * parameter identifying operating system as reported by {@link org.eclipse.core.runtime.Platform#getOS()}
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_OS = "os"; //$NON-NLS-1$

	/**
	 * parameter identifying the current local as reported by {@link org.eclipse.core.runtime.Platform#getNL()}
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_NL = "nl"; //$NON-NLS-1$

	/**
	 * parameter identifying Java version
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_JAVA_VERSION = "java.version"; //$NON-NLS-1$

	/**
	 * parameter identifying the Eclipse runtime version (the version of the org.eclipse.core.runtime bundle)
	 *
	 * @see {@link #setRequestMetaParameters(Map)}
	 */
	public static final String META_PARAM_RUNTIME_VERSION = "runtime.version"; //$NON-NLS-1$

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

	static {
		DEFAULT_SERVICE_URL = ServiceUtil.parseUrl(DEFAULT_SERVICE_LOCATION);
	}

	public DefaultMarketplaceService(URL baseUrl) {
		this.baseUrl = baseUrl == null ? DEFAULT_SERVICE_URL : baseUrl;
	}

	public DefaultMarketplaceService() {
		this(null);
	}

	public List<Market> listMarkets(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_URI_SUFFIX, monitor);
		return marketplace.getMarket();
	}

	public Market getMarket(IMarket market, IProgressMonitor monitor) throws CoreException {
		if (market.getId() == null && market.getUrl() != null) {
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
		} else if (market.getUrl() != null) {
			String marketUrl = market.getUrl();
			for (Market aMarket : markets) {
				if (marketUrl.equals(aMarket.getUrl())) {
					return aMarket;
				}
			}
		}
		throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_marketNotFound, null));
	}

	public Market getMarket(Market market, IProgressMonitor monitor) throws CoreException {
		return getMarket((IMarket) market, monitor);
	}

	public Category getCategory(ICategory category, IProgressMonitor monitor) throws CoreException {
		if (category.getId() != null && category.getUrl() == null) {
			SubMonitor progress = SubMonitor.convert(monitor, 100);
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
				throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_categoryNotFound, null));
			} else {
				return getCategory(resolvedCategory, progress.newChild(50));
			}
		}
		Marketplace marketplace = processRequest(category.getUrl(), API_URI_SUFFIX, monitor);
		if (marketplace.getCategory().isEmpty()) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_categoryNotFound, null));
		} else if (marketplace.getCategory().size() > 1) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, null));
		}
		return marketplace.getCategory().get(0);
	}

	public Category getCategory(Category category, IProgressMonitor monitor) throws CoreException {
		return getCategory((ICategory) category, monitor);
	}

	public Node getNode(INode node, IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace;
		if (node.getId() != null) {
			// bug 304928: prefer the id method rather than the URL, since the id provides a stable URL and the
			// URL is based on the name, which could change.
			String encodedId = urlEncode(node.getId());
			marketplace = processRequest(API_NODE_URI + '/' + encodedId + '/' + API_URI_SUFFIX, monitor);
		} else {
			marketplace = processRequest(node.getUrl(), API_URI_SUFFIX, monitor);
		}
		if (marketplace.getNode().isEmpty()) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_nodeNotFound, null));
		} else if (marketplace.getNode().size() > 1) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, null));
		}
		return marketplace.getNode().get(0);
	}

	public Node getNode(Node node, IProgressMonitor monitor) throws CoreException {
		return getNode((INode) node, monitor);
	}

	public SearchResult search(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		SearchResult result = new SearchResult();
		String relativeUrl = computeRelativeSearchUrl(market, category, queryText, true);
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
			if (search == null) {
				throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, null));
			}
			result.setMatchCount(search.getCount());
			result.setNodes(search.getNode());
		}
		return result;
	}

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

	public SearchResult featured(IProgressMonitor monitor) throws CoreException {
		return featured(null, null, monitor);
	}

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

	public SearchResult featured(IProgressMonitor monitor, Market market, Category category) throws CoreException {
		return featured(market, category, monitor);
	}

	public SearchResult recent(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_RECENT_URI + '/' + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getRecent());
	}

	public SearchResult favorites(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_FAVORITES_URI + '/' + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getFavorites());
	}

	public SearchResult popular(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(API_POPULAR_URI + '/' + API_URI_SUFFIX, monitor);
		return createSearchResult(marketplace.getPopular());
	}

	protected SearchResult createSearchResult(NodeListing nodeList) throws CoreException {
		if (nodeList == null) {
			throw new CoreException(createErrorStatus(Messages.DefaultMarketplaceService_unexpectedResponse, null));
		}
		SearchResult result = new SearchResult();
		result.setMatchCount(nodeList.getCount());
		result.setNodes(nodeList.getNode());
		return result;
	}

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
	@Deprecated
	public void reportInstallError(IProgressMonitor monitor, IStatus result, Set<Node> nodes,
			Set<String> iuIdsAndVersions, String resolutionDetails) throws CoreException {
		reportInstallError(result, nodes, iuIdsAndVersions, resolutionDetails, monitor);
	}

	public void reportInstallError(IStatus result, Set<? extends INode> nodes, Set<String> iuIdsAndVersions,
			String resolutionDetails, IProgressMonitor monitor) throws CoreException {
		HttpClient client;
		URL location;
		HttpPost method;
		try {
			location = new URL(baseUrl, "install/error/report"); //$NON-NLS-1$
			String target = location.toURI().toString();
			client = HttpUtil.createHttpClient(target);
			method = new HttpPost(target);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
		try {
			List<NameValuePair> parameters = new ArrayList<NameValuePair>();

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
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, "UTF-8"); //$NON-NLS-1$
				method.setEntity(entity);
				client.execute(method);
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.DefaultMarketplaceService_cannotCompleteRequest_reason,
					location.toString(), e.getMessage());
			throw new CoreException(createErrorStatus(message, e));
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public void reportInstallSuccess(INode node, IProgressMonitor monitor) {
		String url = node.getUrl();
		if (!url.endsWith("/")) { //$NON-NLS-1$
			url += "/"; //$NON-NLS-1$
		}
		url += "success"; //$NON-NLS-1$
		url = addMetaParameters(url);
		try {
			InputStream stream = transport.stream(new URI(url), monitor);

			try {
				while (stream.read() != -1) {
					// nothing to do
				}
			} finally {
				stream.close();
			}
		} catch (Throwable e) {
			//per bug 314028 logging this error is not useful.
		}
	}
}
