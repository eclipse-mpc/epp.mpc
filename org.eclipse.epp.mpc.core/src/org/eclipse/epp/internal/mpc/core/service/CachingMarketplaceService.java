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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;

public class CachingMarketplaceService implements IMarketplaceService {

	private final IMarketplaceService delegate;

	private int maxCacheSize = 30;

	private final Map<String, Reference<Object>> cache = new LinkedHashMap<String, Reference<Object>>() {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Reference<Object>> eldest) {
			return size() > maxCacheSize || eldest.getValue().get() == null;
		}
	};

	public CachingMarketplaceService(IMarketplaceService delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException();
		}
		this.delegate = delegate;
	}

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	public void setMaxCacheSize(int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}

	public List<? extends IMarket> listMarkets(IProgressMonitor monitor) throws CoreException {
		return delegate.listMarkets(monitor);
	}

	public IMarket getMarket(IMarket market, IProgressMonitor monitor) throws CoreException {
		return delegate.getMarket(market, monitor);
	}

	public ICategory getCategory(ICategory category, IProgressMonitor monitor) throws CoreException {
		return delegate.getCategory(category, monitor);
	}

	public INode getNode(INode node, IProgressMonitor monitor) throws CoreException {
		String nodeKey = computeNodeKey(node);
		INode nodeResult = null;
		if (nodeKey != null) {
			synchronized (cache) {
				Reference<Object> reference = cache.get(nodeKey);
				if (reference != null) {
					nodeResult = (Node) reference.get();
				}
			}
		}
		if (nodeResult == null) {
			nodeResult = delegate.getNode(node, monitor);
			if (nodeResult != null) {
				synchronized (cache) {
					cache.put(computeNodeKey(nodeResult), new SoftReference<Object>(nodeResult));
				}
			}
		}
		return nodeResult;
	}

	private String computeNodeKey(INode node) {
		if (node.getId() != null) {
			return "Node:" + node.getId(); //$NON-NLS-1$
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
					cache.put(key, new SoftReference<Object>(result));
					for (INode node : result.getNodes()) {
						cache.put(computeNodeKey(node), new SoftReference<Object>(node));
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

	public ISearchResult favorites(IProgressMonitor monitor) throws CoreException {
		String key = computeSearchKey("favorites", null, null, null); //$NON-NLS-1$
		return performSearch(monitor, key, new SearchOperation() {

			public ISearchResult doSearch(IProgressMonitor monitor) throws CoreException {
				return delegate.favorites(monitor);
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

	public INews news(IProgressMonitor monitor) throws CoreException {
		return delegate.news(monitor);
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

}
