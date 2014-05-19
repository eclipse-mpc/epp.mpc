/*******************************************************************************
 * Copyright (c) 2011 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Yatta Solutions - news (bug 401721), public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Handler for Marketplace URLs. It supports parsing of Marketplace-related URLs through its {@link #handleUri(String)}
 * method and will call the appropriate <code>handleXXX</code> methods depending on the URL. Clients can override the
 * methods they wish to handle. Default behavior for handle methods is to do nothing unless specified otherwise on the
 * handler method.
 *
 * @author David Green
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public abstract class MarketplaceUrlHandler {

	public static final String DESCRIPTOR_HINT = "org.eclipse.epp.mpc.descriptorHint"; //$NON-NLS-1$

	public static final String MPC_INSTALL_URI = "/mpc/install?"; //$NON-NLS-1$

	public static final String SITE_SEARCH_URI = "/search/site"; //$NON-NLS-1$

	private static final Pattern CONTENT_URL_PATTERN = Pattern.compile("(?:^|/)content/([^/#?]+)"); //$NON-NLS-1$

	private static final Pattern NODE_URL_PATTERN = Pattern.compile("(?:^|/)node/([^/#?]+)"); //$NON-NLS-1$

	public static class SolutionInstallationInfo {

		private String requestUrl;

		private String installId;

		private String state;

		private CatalogDescriptor catalogDescriptor;

		public SolutionInstallationInfo() {
		}

		protected SolutionInstallationInfo(String installId, String state, CatalogDescriptor catalogDescriptor) {
			super();
			this.installId = installId;
			this.state = state;
			this.catalogDescriptor = catalogDescriptor;
		}

		public String getInstallId() {
			return installId;
		}

		public String getState() {
			return state;
		}

		public CatalogDescriptor getCatalogDescriptor() {
			return catalogDescriptor;
		}

		public void setRequestUrl(String requestUrl) {
			this.requestUrl = requestUrl;
		}

		public String getRequestUrl() {
			return requestUrl;
		}
	}

	protected static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

	private static final String PARAM_SPLIT_REGEX = "&"; //$NON-NLS-1$

	private static final String EQUALS_REGEX = "="; //$NON-NLS-1$

	private static final String MPC_STATE = "mpc_state"; //$NON-NLS-1$

	private static final String MPC_INSTALL = "mpc_install"; //$NON-NLS-1$

	public static SolutionInstallationInfo createSolutionInstallInfo(String url) {
		String installId = null;
		String state = null;
		Map<String, String> query = parseQuery(url);
		if (query != null) {
			installId = query.get(MPC_INSTALL);
			state = query.get(MPC_STATE);
		}
		if (installId != null) {
			CatalogDescriptor descriptor = CatalogRegistry.getInstance().findCatalogDescriptor(url);
			if (descriptor == null) {
				try {
					descriptor = new CatalogDescriptor(URLUtil.toURL(url), DESCRIPTOR_HINT);
				} catch (MalformedURLException e) {
					return null;
				}
			}
			SolutionInstallationInfo info = new SolutionInstallationInfo(installId, state, descriptor);
			info.setRequestUrl(url);
			return info;
		}
		return null;
	}

	public static String getMPCState(String url) {
		Map<String, String> query = parseQuery(url);
		return query == null ? null : query.get(MPC_STATE);
	}

	private static Map<String, String> parseQuery(String url) {
		String query;
		try {
			query = new URL(url).getQuery();
		} catch (MalformedURLException e) {
			return null;
		}
		if (query == null) {
			return null;
		}
		Map<String, String> values = new LinkedHashMap<String, String>();
		String[] params = query.split(PARAM_SPLIT_REGEX);
		for (String param : params) {
			String[] keyValue = param.split(EQUALS_REGEX);
			if (keyValue.length == 2) {
				String key = keyValue[0];
				String value = keyValue[1];
				values.put(key, value);
			}
		}
		return values;
	}

	public static boolean isPotentialSolution(String url) {
		return url != null && url.contains(MPC_INSTALL);
	}

	public static void triggerInstall(SolutionInstallationInfo info) {
		if (info.getRequestUrl() != null) {
			MarketplaceClientUi.getLog().log(
					new Status(IStatus.INFO, MarketplaceClientUi.BUNDLE_ID, NLS.bind(
							Messages.MarketplaceUrlHandler_performInstallRequest, info.getRequestUrl())));
		}
		String installId = info.getInstallId();
		String mpcState = info.getState();
		CatalogDescriptor catalogDescriptor = info.getCatalogDescriptor();
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setSelectedCatalogDescriptor(catalogDescriptor);
		try {
			if (mpcState != null) {
				command.setWizardState(URLDecoder.decode(mpcState, UTF_8));
			}
			Map<String, Operation> nodeToOperation = new HashMap<String, Operation>();
			nodeToOperation.put(URLDecoder.decode(installId, UTF_8), Operation.INSTALL);
			command.setOperations(nodeToOperation);
		} catch (UnsupportedEncodingException e1) {
			throw new IllegalStateException(e1);
		}
		try {
			command.execute(new ExecutionEvent());
		} catch (ExecutionException e) {
			IStatus status = MarketplaceClientUi.computeStatus(e,
					Messages.MarketplaceUrlHandler_cannotOpenMarketplaceWizard);
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		}
	}

	public boolean handleUri(String uri) {
		if (isPotentialSolution(uri)) {
			SolutionInstallationInfo installInfo = createSolutionInstallInfo(uri);
			if (installInfo != null) {
				return handleInstallRequest(installInfo, uri);
			}
		}

		CatalogDescriptor descriptor = CatalogRegistry.getInstance().findCatalogDescriptor(uri);
		if (descriptor == null) {
			descriptor = handleUnknownCatalog(uri);
			if (descriptor == null) {
				return false;
			}
		}

		String baseUri;
		try {
			baseUri = descriptor.getUrl().toURI().toString();
			if (!baseUri.endsWith("/")) { //$NON-NLS-1$
				baseUri += '/';
			}
		} catch (URISyntaxException e) {
			// should be unreachable
			throw new IllegalStateException(e);
		}

		if (!uri.startsWith(baseUri)) {
			uri = resolve(uri, baseUri, descriptor);
			if (!uri.startsWith(baseUri)) {
				return false;
			}
		}
		String relativeUri = uri.substring(baseUri.length());
		if (relativeUri.startsWith(DefaultMarketplaceService.API_FAVORITES_URI)) {
			return handleFavorites(descriptor, relativeUri);
		} else if (relativeUri.startsWith(DefaultMarketplaceService.API_FEATURED_URI)) {
			return handleFeatured(descriptor, relativeUri);
		} else if (relativeUri.startsWith(DefaultMarketplaceService.API_NODE_CONTENT_URI)) {
			return handleNodeContent(descriptor, relativeUri);
		} else if (relativeUri.startsWith(DefaultMarketplaceService.API_NODE_URI)) {
			return handleNode(descriptor, relativeUri);
		} else if (relativeUri.startsWith(DefaultMarketplaceService.API_POPULAR_URI)) {
			return handlePopular(descriptor, relativeUri);
		} else if (relativeUri.startsWith(DefaultMarketplaceService.API_RECENT_URI)) {
			return handleRecent(descriptor, relativeUri);
		} else if (relativeUri.startsWith(DefaultMarketplaceService.API_SEARCH_URI)
				|| relativeUri.startsWith(DefaultMarketplaceService.API_SEARCH_URI_FULL)) {
			return handleSolrSearch(descriptor, relativeUri);
		} else if (relativeUri.startsWith(SITE_SEARCH_URI.substring(1))) {
			return handleSiteSearch(descriptor, relativeUri);
		} else {
			return handleUnknownPath(descriptor, relativeUri);
		}
	}

	/**
	 * Resolve the given URL against the catalog's base URI. The default implementation changes the HTTP/HTTPS schema to
	 * match the catalog URI.
	 */
	protected String resolve(String url, String baseUri, CatalogDescriptor descriptor) {
		if (url.startsWith("https:") && baseUri.startsWith("http:")) { //$NON-NLS-1$ //$NON-NLS-2$
			url = "http:" + url.substring("https:".length()); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (url.startsWith("http:") && baseUri.startsWith("https:")) { //$NON-NLS-1$ //$NON-NLS-2$
			url = "https:" + url.substring("http:".length()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return url;
	}

	protected boolean handleUnknownPath(CatalogDescriptor descriptor, String url) {
		return false;
	}

	private boolean handleSolrSearch(CatalogDescriptor descriptor, String url) {
		try {
			Map<String, String> params = new HashMap<String, String>();
			String searchString = parseSearchQuery(descriptor, url, params);
			return handleSearch(descriptor, url, searchString, params);
		} catch (MalformedURLException e) {
			// don't handle malformed URLs
			return false;
		} catch (URISyntaxException e) {
			// don't handle malformed URLs
			return false;
		}
	}

	private boolean handleSiteSearch(CatalogDescriptor descriptor, String url) {
		try {
			Map<String, String> params = new HashMap<String, String>();
			String searchString = parseSearchQuery(descriptor, url, params);

			// convert queries of this format
			//   f[0]=im_taxonomy_vocabulary_1:38&f[1]=im_taxonomy_vocabulary_3:31
			// to internal solr format
			//   filter=tid:38 tid:31
			StringBuilder filter = new StringBuilder();
			for (Iterator<String> i = params.values().iterator(); i.hasNext();) {
				String str = i.next();
				if (str.startsWith("im_taxonomy_vocabulary_")) { //$NON-NLS-1$
					int sep = str.indexOf(':');
					if (sep != -1) {
						String tid = str.substring(sep + 1);
						if (filter.length() > 0) {
							filter.append(' ');
						}
						filter.append(tid);
						i.remove();
					}
				}
			}
			return handleSearch(descriptor, url, searchString, params);
		} catch (MalformedURLException e) {
			// don't handle malformed URLs
			return false;
		} catch (URISyntaxException e) {
			// don't handle malformed URLs
			return false;
		}
	}

	private String parseSearchQuery(CatalogDescriptor descriptor, String url, Map<String, String> params)
			throws URISyntaxException, MalformedURLException {
		URI searchUri = new URL(descriptor.getUrl(), url).toURI();
		String path = searchUri.getPath();
		if (path.endsWith("/")) { //$NON-NLS-1$
			path = path.substring(0, path.length() - 1);
		}
		int sep = path.lastIndexOf('/');
		String searchString = path.substring(sep + 1);
		String query = searchUri.getQuery();
		if (query != null) {
			extractParams(query, params);
		}
		return searchString;
	}

	protected boolean handleSearch(CatalogDescriptor descriptor, String url, String searchString,
			Map<String, String> params) {
		return false;
	}

	private void extractParams(String query, Map<String, String> params) {
		final String[] paramStrings = query.split("&"); //$NON-NLS-1$
		for (String param : paramStrings) {
			final String[] parts = param.split("="); //$NON-NLS-1$
			if (parts.length == 2) {
				params.put(parts[0], parts[1]);
			}
		}
	}

	protected boolean handleRecent(CatalogDescriptor descriptor, String url) {
		return false;
	}

	protected boolean handlePopular(CatalogDescriptor descriptor, String url) {
		return false;
	}

	private boolean handleNode(CatalogDescriptor descriptor, String url) {
		Matcher matcher = NODE_URL_PATTERN.matcher(url);
		String id = null;
		if (matcher.find()) {
			id = matcher.group(1);
		}
		Node node = new Node();
		node.setId(id);
		return handleNode(descriptor, url, node);
	}

	private boolean handleNodeContent(CatalogDescriptor descriptor, String url) {
		Matcher matcher = CONTENT_URL_PATTERN.matcher(url);
		String title = null;
		if (matcher.find()) {
			title = matcher.group(1);
		}
		Node node = new Node();
		node.setUrl(url);
		if (title != null) {
			String base = descriptor.getUrl().toExternalForm();
			if (!base.endsWith("/")) { //$NON-NLS-1$
				base += "/"; //$NON-NLS-1$
			}
			int titleEnd = matcher.end();
			if (titleEnd > -1) {
				//clean the url of other query parameters
				node.setUrl(base + url.substring(0, titleEnd));
			} else {
				//unknown format, leave as-is
				node.setUrl(base + url);
			}
		}
		return handleNode(descriptor, url, node);
	}

	protected boolean handleNode(CatalogDescriptor descriptor, String url, INode node) {
		return false;
	}

	private boolean handleFeatured(CatalogDescriptor descriptor, String url) {
		Matcher matcher = Pattern.compile("(?:^|/)featured/(\\d+)(?:,(\\d+))?").matcher(url); //$NON-NLS-1$
		String cat = null;
		String market = null;
		if (matcher.find()) {
			cat = matcher.group(1);
			if (matcher.groupCount() > 1) {
				market = matcher.group(2);
			}
		}
		return handleFeatured(descriptor, url, cat, market);
	}

	protected boolean handleFeatured(CatalogDescriptor descriptor, String url, String category, String market) {
		return false;
	}

	protected boolean handleFavorites(CatalogDescriptor descriptor, String url) {
		return false;
	}

	/**
	 * Called if no known {@link CatalogDescriptor} is registered for the given URL. Clients may override to return a
	 * custom {@link CatalogDescriptor} or to perform additional lookup of registered catalogs.
	 * <p>
	 * The default implementation looks for catalogs registered for the same host but a different schema - if the url
	 * uses http, a catalog with an equivalent https url is searched and vice versa.
	 */
	protected CatalogDescriptor handleUnknownCatalog(String url) {
		if (url.startsWith("https:")) { //$NON-NLS-1$
			url = "http:" + url.substring("https:".length()); //$NON-NLS-1$ //$NON-NLS-2$
			return CatalogRegistry.getInstance().findCatalogDescriptor(url);
		} else if (url.startsWith("http:")) { //$NON-NLS-1$
			url = "https:" + url.substring("http:".length()); //$NON-NLS-1$ //$NON-NLS-2$
			return CatalogRegistry.getInstance().findCatalogDescriptor(url);
		}
		return null;
	}

	protected boolean handleInstallRequest(SolutionInstallationInfo installInfo, String url) {
		return false;
	}
}
