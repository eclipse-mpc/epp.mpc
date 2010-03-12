/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.xml.Unmarshaller;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author David Green
 */
@SuppressWarnings("serial")
public class DefaultMarketplaceService implements MarketplaceService {

//	This provisional API will be identified by /api/p at the end of most urls.
//
//	/api/p � Returns Markets + Categories
//	/node/%/api/p OR /content/%/api/p � Returns a single listing's detail
//	/taxonomy/term/%/api/p � Returns a category listing of results
//	/featured/api/p � Returns a server-defined number of featured results.
//	/recent/api/p � Returns a server-defined number of recent updates
//	/favorites/top/api/p � Returns a server-defined number of top favorites
//	/popular/top/api/p � Returns a server-defined number of most active results
//
//	There is one exception to adding /api/p at the end and that is for search results.
//
//	/api/p/search/apachesolr_search/[query]?page=[]&filters=[] - Returns search result from the Solr Search DB.
//
//	Once we've locked down the provisional API it will likely be named api/1.

	private static final String UTF_8 = "UTF-8";

	public static final String DEFAULT_SERVICE_LOCATION = System.getProperty(MarketplaceService.class.getName()
			+ ".url", "http://www.eclipseplugincentral.net"); // FIXME "http://marketplace.eclipse.org");

	private URL baseUrl;

	public DefaultMarketplaceService(URL baseUrl) {
		this.baseUrl = baseUrl;
	}

	public DefaultMarketplaceService() {
		try {
			baseUrl = new URL(DEFAULT_SERVICE_LOCATION);
		} catch (MalformedURLException e) {
			MarketplaceClientCore.error(e);
			baseUrl = null;
		}
	}

	public URL getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(URL baseUrl) {
		this.baseUrl = baseUrl;
	}

	public List<Market> listMarkets(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest("api/p", monitor);
		return marketplace.getMarket();
	}

	public Market getMarket(Market market, IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(market.getUrl(), "api/p", monitor);
		if (marketplace.getMarket().isEmpty()) {
			throw new CoreException(createErrorStatus("Market not found", null));
		} else if (marketplace.getMarket().size() > 1) {
			throw new CoreException(createErrorStatus("Unexpected response", null));
		}
		return marketplace.getMarket().get(0);
	}

	public Category getCategory(Category category, IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(category.getUrl(), "api/p", monitor);
		if (marketplace.getCategory().isEmpty()) {
			throw new CoreException(createErrorStatus("Category not found", null));
		} else if (marketplace.getCategory().size() > 1) {
			throw new CoreException(createErrorStatus("Unexpected response", null));
		}
		return marketplace.getCategory().get(0);
	}

	public Node getNode(Node node, IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest(node.getUrl(), "api/p", monitor);
		if (marketplace.getNode().isEmpty()) {
			throw new CoreException(createErrorStatus("Node not found", null));
		} else if (marketplace.getNode().size() > 1) {
			throw new CoreException(createErrorStatus("Unexpected response", null));
		}
		return marketplace.getNode().get(0);
	}

	public SearchResult search(Market market, Category category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		// per bug 302825 - http://www.eclipseplugincentral.net/api/v2/search/apachesolr_search/e?filters=tid:31%20tid:38
		SearchResult result = new SearchResult();
		if (queryText == null || queryText.trim().length() == 0) {
			// search with no text gives us HTTP 404 
			result.setMatchCount(0);
			result.setNodes(new ArrayList<Node>());
		} else {
			String relativeUrl;
			try {
				relativeUrl = "api/p/search/apachesolr_search/" + URLEncoder.encode(queryText.trim(), UTF_8);
				String queryString = "";
				if (market != null || category != null) {
					queryString += "filters=";
					if (market != null) {
						queryString += "tid:" + URLEncoder.encode(market.getId(), UTF_8);
						if (category != null) {
							queryString += "%20";
						}
					}
					if (category != null) {
						queryString += "tid:" + URLEncoder.encode(category.getId(), UTF_8);
					}
				}
				if (queryString.length() > 0) {
					relativeUrl += '?' + queryString;
				}
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException();
			}
			Marketplace marketplace = processRequest(relativeUrl, monitor);
			Search search = marketplace.getSearch();
			if (search == null) {
				throw new CoreException(createErrorStatus("Unexpected response", null));
			}
			result.setMatchCount(search.getCount());
			result.setNodes(search.getNode());
		}
		return result;
	}

	public SearchResult featured(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest("featured/api/p", monitor);
		return createSearchResult(marketplace.getFeatured());
	}

	public SearchResult recent(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest("recent/api/p", monitor);
		return createSearchResult(marketplace.getRecent());
	}

	public SearchResult favorites(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest("favorites/top/api/p", monitor);
		return createSearchResult(marketplace.getFavorites());
	}

	public SearchResult popular(IProgressMonitor monitor) throws CoreException {
		Marketplace marketplace = processRequest("popular/top/api/p", monitor);
		return createSearchResult(marketplace.getActive());
	}

	protected SearchResult createSearchResult(NodeListing nodeList) throws CoreException {
		if (nodeList == null) {
			throw new CoreException(createErrorStatus("Unexpected response", null));
		}
		SearchResult result = new SearchResult();
		result.setMatchCount(nodeList.getCount());
		result.setNodes(nodeList.getNode());
		return result;
	}

	private void checkConfiguration() {
		if (baseUrl == null) {
			throw new IllegalStateException("Must configure Marketplace base url");
		}
	}

	private Marketplace processRequest(String relativeUrl, IProgressMonitor monitor) throws CoreException {
		URI baseUri;
		try {
			baseUri = baseUrl.toURI();
		} catch (URISyntaxException e) {
			// should never happen
			throw new IllegalStateException(e);
		}

		return processRequest(baseUri.toString(), relativeUrl, monitor);
	}

	private Marketplace processRequest(String baseUri, String relativePath, IProgressMonitor monitor)
			throws CoreException {
		checkConfiguration();

		String uri = baseUri;
		if (!uri.endsWith("/") && !relativePath.startsWith("/")) {
			uri += '/';
		}
		uri += relativePath;
		URI location;
		try {
			location = new URI(uri);
		} catch (URISyntaxException e) {
			String message = NLS.bind("Cannot complete request: Invalid location ''{0}'' specified", uri);
			throw new CoreException(createErrorStatus(message, e));
		}

		final Unmarshaller unmarshaller = new Unmarshaller();
		monitor.beginTask(NLS.bind("Retrieving data from {0}", baseUri), 100);
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			parserFactory.setNamespaceAware(true);
			final XMLReader xmlReader;
			try {
				xmlReader = parserFactory.newSAXParser().getXMLReader();
			} catch (Exception e1) {
				throw new IllegalStateException(e1);
			}
			xmlReader.setContentHandler(unmarshaller);

			// FIXME replace by InputStream in = RepositoryTransport.getInstance().stream(location, monitor);
			InputStream in = location.toURL().openStream();
			try {
				monitor.worked(30);

				// FIXME how can the charset be determined?
				Reader reader = new InputStreamReader(new BufferedInputStream(in), UTF_8);
				try {
					xmlReader.parse(new InputSource(reader));
				} catch (final SAXException e) {
					MarketplaceClientCore.error(NLS.bind("Cannot parse XML at URL {0}", location.toString()), e);
					throw new IOException(e.getMessage()) {
						@Override
						public Throwable getCause() {
							return e;
						}
					};
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			String message = NLS.bind("Cannot complete request to {0}: {1}", location.toString(), e.getMessage());
			throw new CoreException(createErrorStatus(message, e));
		} finally {
			monitor.done();
		}

		Object model = unmarshaller.getModel();
		if (model == null) {
			// if we reach here this should never happen
			throw new IllegalStateException();
		} else if (model instanceof Marketplace) {
			return (Marketplace) model;
		} else {
			String message = NLS.bind("Unexpected response content: {0}", model.getClass().getSimpleName());
			throw new CoreException(createErrorStatus(message, null));
		}
	}

	private IStatus createErrorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, 0, message, t);
	}

}
