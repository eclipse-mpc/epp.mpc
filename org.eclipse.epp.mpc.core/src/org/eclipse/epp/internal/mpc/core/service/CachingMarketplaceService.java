/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API, bug 413871: performance
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;

public class CachingMarketplaceService implements IMarketplaceService {

	private final IMarketplaceService delegate;

	private final Map<String, Reference<Object>> cache = new LinkedHashMap<String, Reference<Object>>();

	private final ReferenceQueue<Object> cacheReferenceQueue = new ReferenceQueue<Object>();

	public CachingMarketplaceService(IMarketplaceService delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException();
		}
		this.delegate = delegate;
	}

	public List<? extends IMarket> listMarkets(IProgressMonitor monitor) throws CoreException {
		String marketsKey = "Markets:Markets"; //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<? extends IMarket> marketsResult = getCached(marketsKey, List.class);
		if (marketsResult == null) {
			marketsResult = delegate.listMarkets(monitor);
			synchronized (cache) {
				cache(marketsKey, marketsResult);
				for (IMarket market : marketsResult) {
					cacheMarket(market);
				}
			}
		}
		return marketsResult;
	}

	public IMarket getMarket(IMarket market, IProgressMonitor monitor) throws CoreException {
		String marketKey = computeMarketKey(market);
		IMarket marketResult = null;
		if (marketKey != null) {
			marketResult = getCached(marketKey, IMarket.class);
		}
		if (marketResult == null) {
			marketResult = delegate.getMarket(market, monitor);
			if (marketResult != null) {
				synchronized (cache) {
					cacheMarket(marketResult);
				}
			}
		}
		return marketResult;
	}

	private void cacheMarket(IMarket market) {
		String marketKey = computeMarketKey(market);
		cache(marketKey, market);
		List<? extends ICategory> categories = market.getCategory();
		for (ICategory category : categories) {
			cacheCategory(category);
		}
	}

	private void cacheCategory(ICategory category) {
		String categoryKey = computeCategoryKey(category);
		cache(categoryKey, category);
	}

	public ICategory getCategory(ICategory category, IProgressMonitor monitor) throws CoreException {
		String categoryKey = computeCategoryKey(category);
		ICategory categoryResult = null;
		if (categoryKey != null) {
			categoryResult = getCached(categoryKey, ICategory.class);
		}
		if (categoryResult == null) {
			categoryResult = delegate.getCategory(category, monitor);
			if (categoryResult != null) {
				synchronized (cache) {
					cacheCategory(categoryResult);
				}
			}
		}
		return categoryResult;
	}

	public INode getNode(INode node, IProgressMonitor monitor) throws CoreException {
		String nodeKey = computeNodeKey(node);
		INode nodeResult = null;
		if (nodeKey != null) {
			nodeResult = getCached(nodeKey, INode.class);
		}
		if (nodeResult == null) {
			String nodeUrlKey = computeNodeUrlKey(node);
			if (nodeUrlKey != null) {
				nodeResult = getCached(nodeUrlKey, INode.class);
			}
		}
		if (nodeResult == null) {
			nodeResult = delegate.getNode(node, monitor);
			if (nodeResult != null) {
				synchronized (cache) {
					cache(nodeKey, nodeResult);
					cache(computeNodeUrlKey(node), nodeResult);
					cache(computeNodeIdUrlKey(node), nodeResult);
				}
			}
		}
		return nodeResult;
	}

	private void cache(String key, Object value) {
		cache.put(key, new SoftReference<Object>(value, cacheReferenceQueue));
	}

	private <T> T getCached(String key, Class<T> type) {
		synchronized (cache) {
			gcCache();
			Reference<Object> reference = cache.get(key);
			if (reference != null) {
				return type.cast(reference.get());
			}
		}
		return null;
	}

	private void gcCache() {
		if (cacheReferenceQueue.poll() != null) {
			while (cacheReferenceQueue.poll() != null) {
				//clear the queue
			}
			for (Iterator<Reference<Object>> i = cache.values().iterator(); i.hasNext();) {
				Reference<Object> reference = i.next();
				if (reference.isEnqueued()) {
					i.remove();
				}
			}
		}
	}

	private String computeNodeKey(INode node) {
		if (node.getId() != null) {
			return "Node:" + node.getId(); //$NON-NLS-1$
		}
		return null;
	}

	private String computeNodeUrlKey(INode node) {
		if (node.getUrl() != null) {
			return "Node:" + node.getUrl(); //$NON-NLS-1$
		}
		return null;
	}

	private String computeNodeIdUrlKey(INode node) {
		if (node.getId() != null) {
			String url = URLUtil.appendPath(getBaseUrl().toString(), DefaultMarketplaceService.API_NODE_URI,
					node.getId());

			return "Node:" + url; //$NON-NLS-1$
		}
		return null;
	}

	private String computeMarketKey(IMarket market) {
		if (market.getId() != null) {
			return "Market:" + market.getId(); //$NON-NLS-1$
		}
		return null;
	}

	private String computeCategoryKey(ICategory category) {
		if (category.getId() != null) {
			return "Category:" + category.getId(); //$NON-NLS-1$
		}
		return null;
	}

	private interface SearchOperation {
		public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException;
	}

	public ISearchResult search(final IMarket market, final ICategory category, final String queryText,
			IProgressMonitor monitor) throws CoreException {
		String key = computeSearchKey("search", market, category, queryText); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {

			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.search(market, category, queryText, monitor);
			}
		});
	}

	private ISearchResult performSearch(IProgressMonitor monitor, String key, SearchOperation searchOperation)
			throws CoreException {
		ISearchResult result = null;
		synchronized (cache) {
			Reference<Object> reference = cache.get(key);
			if (reference != null) {
				result = (ISearchResult) reference.get();
			}
		}
		if (result == null) {
			result = searchOperation.doSearch(monitor);
			if (result != null) {
				synchronized (cache) {
					cache(key, result);
					for (INode node : result.getNodes()) {
						cache(computeNodeKey(node), node);
					}
				}
			}
		}
		return result;
	}

	private String computeSearchKey(String prefix, IMarket market, ICategory category, String queryText) {
		return prefix
				+ ":" + (market == null ? "" : market.getId()) + ":" + (category == null ? "" : category.getId()) + ":" + (queryText == null ? "" : queryText.trim()); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$ //$NON-NLS-6$
	}

	public URL getBaseUrl() {
		return delegate.getBaseUrl();
	}

	public ISearchResult featured(IProgressMonitor monitor) throws CoreException {
		String key = computeSearchKey("featured", null, null, null); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {

			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.featured(monitor);
			}
		});
	}

	public ISearchResult featured(final IMarket market, final ICategory category, IProgressMonitor monitor)
			throws CoreException {
		String key = computeSearchKey("featured", market, category, null); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {

			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.featured(market, category, monitor);
			}
		});
	}

	public ISearchResult recent(IProgressMonitor monitor) throws CoreException {
		String key = computeSearchKey("recent", null, null, null); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {

			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.recent(monitor);
			}
		});
	}

	public ISearchResult topFavorites(IProgressMonitor monitor) throws CoreException {
		String key = computeSearchKey("favorites", null, null, null); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {

			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.topFavorites(monitor);
			}
		});
	}

	public ISearchResult popular(IProgressMonitor monitor) throws CoreException {
		String key = computeSearchKey("popular", null, null, null); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {
			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.popular(monitor);
			}
		});

	}

	public ISearchResult related(final List<? extends INode> basedOn, IProgressMonitor monitor) throws CoreException {
		String searchKey = null;
		if (basedOn != null && !basedOn.isEmpty()) {
			StringBuilder searchKeyBldr = new StringBuilder();
			for (INode node : basedOn) {
				searchKeyBldr.append(node.getId()).append('+');
			}
			searchKey = searchKeyBldr.substring(0, searchKeyBldr.length() - 1);
		}
		String key = computeSearchKey("related", null, null, searchKey); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {
			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.related(basedOn, monitor);
			}
		});
	}

	public INews news(IProgressMonitor monitor) throws CoreException {
		String newsKey = "News:News"; //$NON-NLS-1$
		INews newsResult = getCached(newsKey, INews.class);
		if (newsResult == null) {
			newsResult = delegate.news(monitor);
			synchronized (cache) {
				cache(newsKey, newsResult);
			}
		}
		return newsResult;
	}

	public void reportInstallError(IProgressMonitor monitor, IStatus result, Set<Node> nodes,
			Set<String> iuIdsAndVersions, String resolutionDetails) throws CoreException {
		reportInstallError(result, nodes, iuIdsAndVersions, resolutionDetails, monitor);
	}

	public void reportInstallError(IStatus result, Set<? extends INode> nodes, Set<String> iuIdsAndVersions,
			String resolutionDetails, IProgressMonitor monitor) throws CoreException {
		delegate.reportInstallError(result, nodes, iuIdsAndVersions, resolutionDetails, monitor);
	}

	public void reportInstallSuccess(INode node, IProgressMonitor monitor) {
		delegate.reportInstallSuccess(node, monitor);
	}

	@Deprecated
	public ISearchResult favorites(IProgressMonitor monitor) throws CoreException {
		return topFavorites(monitor);
	}

	public ISearchResult userFavorites(IProgressMonitor monitor) throws CoreException, NotAuthorizedException {
		//we don't cache the favorite status, only contents individual nodes, which happens internally...
		return delegate.userFavorites(monitor);
	}

	public void userFavorites(List<? extends INode> nodes, IProgressMonitor monitor)
			throws CoreException, NotAuthorizedException {
		//we don't cache the favorite status, only contents individual nodes, which happens internally...
		delegate.userFavorites(nodes, monitor);
	}

	public UserFavoritesService getUserFavoritesService() {
		return delegate.getUserFavoritesService();
	}

}
