/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;

/**
 * A service that provides access to the marketplace and implements the <a
 * href="https://wiki.eclipse.org/Marketplace/REST">Marketplace REST API</a>.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @author David Green
 * @author Carsten Reckord
 */
public interface IMarketplaceService {

	/**
	 * Property key for registered IMarketplaceService OSGi services indicating the marketplace's base URL.
	 */
	public static final String BASE_URL = "url"; //$NON-NLS-1$

	/**
	 * Get a list of all markets. This is the entrypoint to the marketplace.
	 */
	List<? extends IMarket> listMarkets(IProgressMonitor monitor) throws CoreException;

	/**
	 * Get a market by its id
	 *
	 * @param market
	 *            the market which must have an {@link IMarket#getUrl() url}.
	 * @return the identified node
	 */
	IMarket getMarket(IMarket market, IProgressMonitor monitor) throws CoreException;

	/**
	 * Get a category by its id
	 *
	 * @param category
	 *            A category which must have an {@link ICategory#getUrl() url}.
	 * @return the identified category
	 */
	ICategory getCategory(ICategory category, IProgressMonitor monitor) throws CoreException;

	/**
	 * Get a node by its id
	 *
	 * @param node
	 *            the node which must either have an {@link INode#getUrl() url} or an {@link INode#getId() id}.
	 * @return the identified node
	 */
	INode getNode(INode node, IProgressMonitor monitor) throws CoreException;

	/**
	 * Find nodes in the marketplace with a text query, and optionally specify the market/category
	 *
	 * @param market
	 *            the market to search in, or null if the search should span all markets
	 * @param category
	 *            the category to search in, or null if the search should span all categories
	 * @param queryText
	 *            the query text, must not be null
	 * @return the search result
	 */
	ISearchResult search(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * Find featured nodes in the marketplace
	 *
	 * @return the search result
	 */
	ISearchResult featured(IProgressMonitor monitor) throws CoreException;

	/**
	 * Find featured nodes in the marketplace
	 *
	 * @param market
	 *            the market in which to return featured, or null if featured should include all markets
	 * @param category
	 *            the category in which to return fetured, or null if featured should include all categories
	 * @return the search result
	 */
	ISearchResult featured(IMarket market, ICategory category, IProgressMonitor monitor) throws CoreException;

	/**
	 * Find recently added/modified nodes in the marketplace
	 *
	 * @return the search result
	 */
	ISearchResult recent(IProgressMonitor monitor) throws CoreException;

	/**
	 * Find most-favorited nodes in the marketplace
	 *
	 * @return the search result
	 */
	ISearchResult favorites(IProgressMonitor monitor) throws CoreException;

	/**
	 * Find most active nodes in the marketplace
	 *
	 * @return the search result
	 */
	ISearchResult popular(IProgressMonitor monitor) throws CoreException;

	/**
	 * Get the news configuration for the marketplace
	 *
	 * @return the news configuration
	 */
	INews news(IProgressMonitor monitor) throws CoreException;

	/**
	 * Report an error in resolving an install operation.
	 *
	 * @param result
	 *            the status of the install operation
	 * @param nodes
	 *            the nodes that were included in the install, or null if unknown.
	 * @param iuIdsAndVersions
	 *            the IUs and their versions (comma-delimited), or null if unknown.
	 * @param resolutionDetails
	 *            the detailed error message, or null if unknown.
	 * @param monitor
	 * @noreference This method is not intended to be called by clients directly. It should only ever be called as part
	 *              of an install operation.
	 */
	void reportInstallError(IStatus result, Set<? extends INode> nodes, Set<String> iuIdsAndVersions,
			String resolutionDetails, IProgressMonitor monitor) throws CoreException;

	/**
	 * Report a successful install.
	 *
	 * @param node
	 *            the installed node
	 * @param monitor
	 * @noreference This method is not intended to be called by clients directly. It should only ever be called as part
	 *              of an install operation.
	 */
	void reportInstallSuccess(INode node, IProgressMonitor monitor);

}