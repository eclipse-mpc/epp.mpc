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
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.model.FavoriteList;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientService;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.RequestTemplate;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.osgi.util.NLS;
import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.internal.Session;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NoServiceException;
import org.eclipse.userstorage.util.NotFoundException;
import org.eclipse.userstorage.util.ProtocolException;

@SuppressWarnings("restriction")
public class UserFavoritesService extends AbstractDataStorageService implements IUserFavoritesService {

	private static final String MARKETPLACE_USER_FAVORITES_ENDPOINT = "user/%s/favorites"; //$NON-NLS-1$

	private static final String RANDOM_FAVORITE_LISTS_ENDPOINT = "marketplace/favorites/random"; //$NON-NLS-1$

	private static final int MALFORMED_CONTENT_ERROR_CODE = 499;

	private static final String TEMPLATE_VARIABLE = "%s"; //$NON-NLS-1$

	/**
	 * Matches a single object/dict in a list, returning its body (i.e. without the braces) in its first match group.
	 * This only supports dicts with simple attributes. Nested dicts will result in wrong matches.
	 */
	private static final String JSON_LIST_OBJECTS_REGEX = "\\{([^\\{\\}]+)\\}"; //$NON-NLS-1$

	private static final Pattern JSON_LIST_OBJECTS_PATTERN = Pattern.compile(JSON_LIST_OBJECTS_REGEX,
			Pattern.MULTILINE);

	/**
	 * Returns the body of the list value for the attribute with the given name, e.g. for <code>
	 *    {"users":[{...},{...}], "count"="2"}
	 * </code> and the name "users" it will return "{...},{...}" in its first match group.
	 */
	private static final String JSON_ATTRIBUTE_OBJECT_LIST_REGEX = "\\{(?:.*,)?\\s*\"" + TEMPLATE_VARIABLE //$NON-NLS-1$
			+ "\"\\s*:\\s*\\[((?:\\s*" + JSON_LIST_OBJECTS_REGEX + "\\s*,?\\s*)*)\\],.*\\}"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Matches a single string attribute in a dict. Returns the matched attribute name in the first match group and the
	 * attribute value in the second.
	 */
	private static final String JSON_ATTRIBUTE_REGEX = "(?<=[,\\{]|^)\\s*\"(" + TEMPLATE_VARIABLE //$NON-NLS-1$
			+ ")\"\\s*:\\s*\"([^\"]*)\"\\s*(?=[,\\}]|$)"; //$NON-NLS-1$

	private static final Pattern JSON_MPC_FAVORITES_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_OBJECT_LIST_REGEX, "mpc_favorites"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_FAVORITE_LISTS_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_OBJECT_LIST_REGEX, "users"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_USER_ID_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "name"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_OWNER_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "(?:full_)?name"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_NAME_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "mpc_list_name"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_CONTENT_ID_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "content_id"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_LIST_URL_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "html_mpc_favorites_url"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_OWNER_ICON_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "picture"), Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern JSON_OWNER_PROFILE_URL_ATTRIBUTE_PATTERN = Pattern
			.compile(String.format(JSON_ATTRIBUTE_REGEX, "html_profile_url"), Pattern.MULTILINE); //$NON-NLS-1$

	public static final Pattern FAVORITES_URL_PATTERN = Pattern
			.compile("^(?:https?:.*/)?user/([^/#?]+)(?:/favorites)?(?:[/#?].*)?$"); //$NON-NLS-1$

	private static final String KEY = "mpc_favorites"; //$NON-NLS-1$

	private static final int RETRY_COUNT = 3;

	private static final String SEPARATOR = ","; //$NON-NLS-1$

	private final Map<String, Integer> favoritesCorrections = new HashMap<>();

	private final Set<String> favorites = new HashSet<>();

	private HttpClientService httpClient;

	protected IBlob getFavoritesBlob() {
		return getStorageService().getBlob(KEY);
	}

	@Override
	public Integer getFavoriteCount(INode node) {
		Integer favorited = node.getFavorited();
		if (favorited == null) {
			return null;
		}
		Integer correction = favoritesCorrections.get(node.getId());
		if (correction != null) {
			favorited += correction;
		}
		return favorited;
	}

	public Set<String> getLastFavoriteIds() {
		return Collections.unmodifiableSet(favorites);
	}

	@Override
	public Set<String> getFavoriteIds(IProgressMonitor monitor)
			throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException {
//		throw new IOException("simulates favorites failure");
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DefaultMarketplaceService_FavoritesRetrieve, 1000);
		try {
			String favoritesData = getFavoritesBlob().getContentsUTF();
			progress.worked(950);//FIXME waiting for USS bug 488335 to have proper progress and cancelation
			Set<String> result = parseFavoritesBlobData(favoritesData);
			synchronized (this) {
				favorites.clear();
				favorites.addAll(result);
			}
			return result;
		} catch (NotFoundException ex) {
			//the user does not yet have favorites
			return new LinkedHashSet<>();
		} catch (OperationCanceledException ex) {
			throw processProtocolException(ex);
		} catch (ProtocolException ex) {
			throw processProtocolException(ex);
		}
	}

	public List<INode> getLastFavorites() {
		Set<String> favoriteIds = getLastFavoriteIds();
		List<INode> favoriteNodes = toNodes(favoriteIds);
		return favoriteNodes;
	}

	@Override
	public List<INode> getFavorites(IProgressMonitor monitor)
			throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException {
		Set<String> favoriteIds = getFavoriteIds(monitor);
		List<INode> favoriteNodes = toNodes(favoriteIds);
		return favoriteNodes;
	}

	@Override
	public List<IFavoriteList> getRandomFavoriteLists(IProgressMonitor monitor) throws IOException {
		URI serviceUri = getStorageService().getServiceUri();
		final URI randomFavoritesUri = serviceUri.resolve(RANDOM_FAVORITE_LISTS_ENDPOINT);
		return new AbstractJSONListRequest<IFavoriteList>(randomFavoritesUri, JSON_FAVORITE_LISTS_PATTERN) {

			@Override
			protected IFavoriteList parseListElement(String entryBody) {
				String id = findFavoritesListId(entryBody);
				if (id == null) {
					return null;
				}
				String owner = findFavoritesListOwner(entryBody);
				if (owner == null) {
					owner = id;
				}
				String label = findFavoritesListLabel(entryBody);
				if (label != null && (label.equals(id) || label.equals(owner))) {
					label = null;
				}
				String favoritesListUrl = getFavoritesListUrl(entryBody, id);
				if (favoritesListUrl == null) {
					return null;
				}
				String icon = getAttribute(JSON_OWNER_ICON_ATTRIBUTE_PATTERN, null, entryBody);
				String profileUrl = getAttribute(JSON_OWNER_PROFILE_URL_ATTRIBUTE_PATTERN, null, entryBody);
				IFavoriteList favoritesByUserId = QueryHelper.favoritesByUserId(id);
				((FavoriteList) favoritesByUserId).setOwner(owner);
				((FavoriteList) favoritesByUserId).setOwnerProfileUrl(profileUrl);
				((FavoriteList) favoritesByUserId).setName(label);
				((FavoriteList) favoritesByUserId).setUrl(favoritesListUrl);
				((FavoriteList) favoritesByUserId).setIcon(icon);
				return favoritesByUserId;
			}

		}.execute(httpClient, randomFavoritesUri);
	}

	private static String getAttribute(Pattern attributePattern, String attributeName, String entryBody) {
		Matcher matcher = attributePattern.matcher(entryBody);
		while (matcher.find()) {
			String matchedName = matcher.group(1);
			if (attributeName == null || attributeName.equals(matchedName)) {
				return matcher.group(2);
			}
		}
		return null;
	}

	private String getFavoritesListUrl(String entryBody, String id) {
		String marketplaceBaseUri = getMarketplaceBaseUri();
		//We use the HTML URL shown in the web frontend instead of the API URL, because that's what's advertised
		String explicitUrl = getAttribute(JSON_LIST_URL_ATTRIBUTE_PATTERN, null, entryBody);
		if (explicitUrl != null && explicitUrl.trim().length() > 0) {
			try {
				//Check that it's a valid URL
				URL url = URLUtil.toURL(explicitUrl);
				URI uri = url.toURI();
				if (!uri.isAbsolute()) {
					uri = new URI(marketplaceBaseUri).resolve(uri);
				}
				return uri.toURL().toString();
			} catch (Exception ex) {
				MarketplaceClientCore
				.error(NLS.bind("Invalid list URL {0} for favorites list {1} - falling back to default URL",
						explicitUrl, id),
						ex);
			}
		}
		String path = String.format(MARKETPLACE_USER_FAVORITES_ENDPOINT, URLUtil.encode(id));
		return URLUtil.appendPath(marketplaceBaseUri, path);
	}

	private String getMarketplaceBaseUri() {
		String marketplaceBaseUri = getStorageService().getMarketplaceBaseUri();
		if (marketplaceBaseUri != null) {
			return marketplaceBaseUri;
		}
		IMarketplaceService defaultMarketplaceService = ServiceHelper.getMarketplaceServiceLocator()
				.getDefaultMarketplaceService();
		if (defaultMarketplaceService != null) {
			return defaultMarketplaceService.getBaseUrl().toString();
		}
		return DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION;
	}

	private static String findFavoritesListOwner(String entryBody) {
		return findFavoritesNameOrId(entryBody, JSON_OWNER_ATTRIBUTE_PATTERN);
	}

	private static String findFavoritesListLabel(String entryBody) {
		return findFavoritesNameOrId(entryBody, JSON_NAME_ATTRIBUTE_PATTERN);
	}

	private static String findFavoritesListId(String entryBody) {
		return findFavoritesNameOrId(entryBody, JSON_USER_ID_ATTRIBUTE_PATTERN);
	}

	private static String findFavoritesNameOrId(String entryBody, Pattern pattern) {
		String result = null;
		Matcher matcher = pattern.matcher(entryBody);
		while (matcher.find()) {
			String name = matcher.group(1);
			String value = matcher.group(2);
			if ("name".equals(name)) { //$NON-NLS-1$
				//remember, but try to find a better match
				if (result == null) {
					result = value;
				}
			} else {
				return value;
			}
		}
		return result;
	}

	private static List<INode> toNodes(Collection<String> favoriteIds) {
		List<INode> favoriteNodes = new ArrayList<>(favoriteIds.size());
		for (String nodeId : favoriteIds) {
			INode node = QueryHelper.nodeById(nodeId);
			favoriteNodes.add(node);
		}
		return favoriteNodes;
	}

	protected Set<String> parseFavoritesBlobData(String favoritesData) {
		Set<String> favoriteIds = new LinkedHashSet<>();
		for (StringTokenizer tokenizer = new StringTokenizer(favoritesData, SEPARATOR); tokenizer.hasMoreTokens();) {
			String nodeId = tokenizer.nextToken();
			favoriteIds.add(nodeId);
		}
		return favoriteIds;
	}

	@Override
	public void setFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NoServiceException, ConflictException, NotAuthorizedException, IllegalStateException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UserFavoritesService_SettingUserFavorites, 1000);
		String favoritesData = createFavoritesBlobData(nodes);
		try {
			if (favoritesData == null || "".equals(favoritesData)) { //$NON-NLS-1$
				getFavoritesBlob().delete();
			} else {
				getFavoritesBlob().setContentsUTF(favoritesData);
			}
			progress.worked(900);//FIXME waiting for USS bug 488335 to have proper progress and cancelation
		} catch (OperationCanceledException ex) {
			throw processProtocolException(ex);
		} catch (ProtocolException ex) {
			throw processProtocolException(ex);
		}
		synchronized (this) {
			SubMonitor notifyNewProgress = SubMonitor.convert(progress.newChild(50), nodes.size());
			Set<String> newFavorites = new HashSet<>();
			for (INode node : nodes) {
				String id = node.getId();
				if (newFavorites.add(id)) {
					boolean newFavorite = favorites.add(id);
					if (newFavorite) {
						didChangeFavorite(id, true);
					}
				}
				notifyNewProgress.worked(1);
			}
			SubMonitor notifyRemovedProgress = SubMonitor.convert(progress.newChild(50), favorites.size());
			for (Iterator<String> i = favorites.iterator(); i.hasNext();) {
				String id = i.next();
				if (!newFavorites.contains(id)) {
					i.remove();
					didChangeFavorite(id, false);
				}
				notifyRemovedProgress.worked(1);
			}
		}
	}

	private void didChangeFavorite(String nodeId, boolean favorite) {
		Integer correction = favoritesCorrections.get(nodeId);
		if (correction == null) {
			correction = 0;
		}
		correction += favorite ? 1 : -1;
		if (correction < -1) {
			correction = -1;
		} else if (correction > 1) {
			correction = 1;
		}
		if (correction == 0) {
			favoritesCorrections.remove(nodeId);
		} else {
			favoritesCorrections.put(nodeId, correction);
		}
	}

	protected String createFavoritesBlobData(Collection<? extends INode> nodes) {
		if (nodes.isEmpty()) {
			return null;
		}
		List<String> nodeIds = new ArrayList<>(nodes.size());
		for (INode node : nodes) {
			nodeIds.add(node.getId());
		}
		Collections.sort(nodeIds);
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String nodeId : nodeIds) {
			if (first) {
				first = false;
			} else {
				builder.append(SEPARATOR);
			}
			builder.append(nodeId);
		}
		return builder.toString();
	}

	@Override
	public void setFavorite(INode node, boolean favorite, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		alterFavorites(Collections.singleton(node), favorite, monitor);
	}

	@Override
	public void addFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		alterFavorites(nodes, true, monitor);
	}

	@Override
	public void removeFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		alterFavorites(nodes, false, monitor);
	}

	private void alterFavorites(Collection<? extends INode> nodes, boolean favorite, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UserFavoritesService_SettingUserFavorites, 1000);
		ConflictException conflictException = null;
		for (int i = 0; i < RETRY_COUNT; i++) {
			try {
				progress.setWorkRemaining(1000);
				doAlterFavorites(nodes, favorite, progress.newChild(800));
				progress.done();
				return;
			} catch (ConflictException e) {
				conflictException = e;
			} catch (OperationCanceledException ex) {
				throw processProtocolException(ex);
			} catch (ProtocolException ex) {
				throw processProtocolException(ex);
			}
		}
		if (conflictException != null) {
			throw conflictException;
		}
	}

	private void doAlterFavorites(Collection<? extends INode> nodes, boolean favorite, IProgressMonitor monitor)
			throws ConflictException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UserFavoritesService_SettingUserFavorites, 1000);
		List<INode> favorites = getFavorites(progress.newChild(300));
		for (INode node : nodes) {
			INode currentFavorite = QueryHelper.findById(favorites, node);
			if (currentFavorite != null && !favorite) {
				favorites.remove(currentFavorite);
			} else if (currentFavorite == null && favorite) {
				favorites.add(node);
			}
		}
		setFavorites(favorites, progress.newChild(700));
	}

	@Override
	public List<INode> getFavorites(URI uri, IProgressMonitor monitor) throws IOException {
		List<String> nodeIds = getFavoriteIds(uri, monitor);
		return toNodes(nodeIds);
	}

	private URI normalizeURI(URI uri) {
		validateUri(uri);
		String marketplaceBaseUri = getMarketplaceBaseUri();
		marketplaceBaseUri = URLUtil.appendPath(marketplaceBaseUri, ""); //$NON-NLS-1$
		marketplaceBaseUri = URLUtil.setScheme(marketplaceBaseUri, uri.getScheme());
		if (!uri.toString().startsWith(marketplaceBaseUri)) {
			return uri;
		}
		Matcher matcher = FAVORITES_URL_PATTERN.matcher(uri.toString());
		if (matcher.find()) {
			String name = matcher.group(1);
			return getStorageService().getServiceUri()
					.resolve("marketplace/favorites/?name=" + URLUtil.urlEncode(name));
		}
		return uri;
	}

	public static void validateUri(URI uri) {
		if ("".equals(uri.toString()) //$NON-NLS-1$
				|| ((uri.getHost() == null || "".equals(uri.getHost())) //$NON-NLS-1$
						&& (uri.getScheme() != null && uri.getScheme().toLowerCase().startsWith("http"))) //$NON-NLS-1$
				|| (uri.getScheme() == null && (uri.getPath() == null || "".equals(uri.getPath())))) { //$NON-NLS-1$
			//incomplete uri
			throw new IllegalArgumentException(
					new URISyntaxException(uri.toString(), Messages.UserFavoritesService_uriMissingHost));
		}
	}

	@Override
	public List<String> getFavoriteIds(final URI uri, IProgressMonitor monitor) throws IOException {
		URI normalizedUri = normalizeURI(uri);
		try {
			return new AbstractJSONListRequest<String>(normalizedUri, JSON_MPC_FAVORITES_PATTERN) {

				@Override
				protected String parseListElement(String listElement) {
					Matcher contentIdMatcher = JSON_CONTENT_ID_ATTRIBUTE_PATTERN.matcher(listElement);
					if (contentIdMatcher.find()) {
						return contentIdMatcher.group(2);
					}
					return null;
				}

			}.execute(httpClient, uri);
		} catch (FileNotFoundException e) {
			return new ArrayList<>();
		}
	}

	private static ProtocolException malformedContentException(final URI endpoint, String body) {
		return new ProtocolException("GET", endpoint, "1.1", MALFORMED_CONTENT_ERROR_CODE, //$NON-NLS-1$ //$NON-NLS-2$
				"Malformed response content: " + body); //$NON-NLS-1$
	}

	public static boolean isInvalidFavoritesListException(Throwable error) {
		while (error != null) {
			if (isMalformedContentException(error) || isNotFoundException(error)) {
				return true;
			}
			if (error instanceof CoreException) {
				CoreException coreException = (CoreException) error;
				IStatus status = coreException.getStatus();
				if (status.isMultiStatus()) {
					for (IStatus childStatus : status.getChildren()) {
						if (childStatus.getException() != null
								&& isInvalidFavoritesListException(childStatus.getException())) {
							return true;
						}
					}
				}
			}
			error = error.getCause();
		}
		return false;
	}

	public static boolean isInvalidUrlException(Throwable error) {
		while (error != null) {
			if (error instanceof URISyntaxException || error instanceof MalformedURLException) {
				return true;
			}
			if (error instanceof CoreException) {
				CoreException coreException = (CoreException) error;
				IStatus status = coreException.getStatus();
				if (status.isMultiStatus()) {
					for (IStatus childStatus : status.getChildren()) {
						if (childStatus.getException() != null && isInvalidUrlException(childStatus.getException())) {
							return true;
						}
					}
				}
			}
			error = error.getCause();
		}
		return false;
	}

	private static boolean isMalformedContentException(Throwable error) {
		if (error instanceof ProtocolException) {
			ProtocolException protocolException = (ProtocolException) error;
			switch (protocolException.getStatusCode()) {
			case HttpStatus.SC_BAD_REQUEST:
			case HttpStatus.SC_METHOD_NOT_ALLOWED:
			case HttpStatus.SC_NOT_ACCEPTABLE:
			case HttpStatus.SC_EXPECTATION_FAILED:
			case MALFORMED_CONTENT_ERROR_CODE:
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	private static boolean isNotFoundException(Throwable error) {
		if (error instanceof NotFoundException) {
			return true;
		}
		if (error instanceof ProtocolException) {
			ProtocolException protocolException = (ProtocolException) error;
			switch (protocolException.getStatusCode()) {
			case HttpStatus.SC_NOT_FOUND:
			case HttpStatus.SC_GONE:
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	private static abstract class AbstractJSONListRequest<T> extends RequestTemplate<List<T>> {
		private final URI uri;

		private final Pattern listAttributePattern;

		private AbstractJSONListRequest(URI uri, Pattern listAttributePattern) {
			this.uri = uri;
			this.listAttributePattern = listAttributePattern;
		}

		@Override
		protected HttpUriRequest configureRequest(HttpClientService client, HttpUriRequest request) {
			HttpUriRequest configuredRequest = super.configureRequest(client, request);
			configuredRequest.setHeader(HttpHeaders.USER_AGENT, Session.USER_AGENT_ID);
			return configuredRequest;
		}

		@Override
		protected List<T> handleResponseStream(InputStream content, Charset charset) throws IOException {
			String body = read(content, charset);
			body = body.trim();
			return handleBody(uri, body);
		}

		private static String read(InputStream in, Charset charset) throws IOException {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				IOUtil.copy(in, baos);
				byte[] bytes = baos.toByteArray();
				if (bytes == null) {
					return StringUtil.EMPTY;
				}

				return new String(bytes, charset == null ? StringUtil.UTF8 : charset.name());
			} catch (RuntimeException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof IOException) {
					throw (IOException) cause;
				}
				throw ex;
			} finally {
				IOUtil.close(in);
			}
		}

		protected List<T> handleBody(final URI uri, String body) throws ProtocolException {
			List<T> favoriteIds = new ArrayList<>();
			if (!"".equals(body)) { //$NON-NLS-1$
				Matcher matcher = listAttributePattern.matcher(body);
				if (matcher.find()) {
					String listBody = matcher.group(1);
					Matcher entryMatcher = JSON_LIST_OBJECTS_PATTERN.matcher(listBody);
					while (entryMatcher.find()) {
						String listElement = entryMatcher.group(1);
						T parsedElement = parseListElement(listElement);
						if (parsedElement != null) {
							favoriteIds.add(parsedElement);
						}
					}
				} else {
					throw malformedContentException(uri, body);
				}
			}
			return favoriteIds;
		}

		protected abstract T parseListElement(String listElement);

		@Override
		protected HttpUriRequest createRequest(URI uri) {
			return RequestBuilder.get(uri)
					.addHeader(HttpHeaders.CONTENT_TYPE, Session.APPLICATION_JSON) //
					.addHeader(HttpHeaders.ACCEPT, Session.APPLICATION_JSON)
					.build();
		}
	}

	public void setHttpClient(HttpClientService httpClient) {
		this.httpClient = httpClient;
	}

	public HttpClientService getHttpClient() {
		return httpClient;
	}

	public void bindHttpClient(HttpClientService httpClient) {
		setHttpClient(httpClient);
	}

	public void unbindHttpClient(HttpClientService httpClient) {
		if (this.httpClient == httpClient) {
			setHttpClient(null);
		}
	}

}
