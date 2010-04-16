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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * a service that provides access to the marketplace.
 * 
 * @author David Green
 */
public interface MarketplaceService {

	/**
	 * Get a list of all markets. This is the entrypoint to the marketplace.
	 */
	public List<Market> listMarkets(IProgressMonitor monitor) throws CoreException;

	/**
	 * Get a market by its id
	 * 
	 * @param market
	 *            the market which must have an {@link Market#getUrl() url}.
	 * @return the identified node
	 */
	public Market getMarket(Market market, IProgressMonitor monitor) throws CoreException;

	/**
	 * Get a category by its id
	 * 
	 * @param category
	 *            A category which must have an {@link Category#getUrl() url}.
	 * @return the identified category
	 */
	public Category getCategory(Category category, IProgressMonitor monitor) throws CoreException;

	/**
	 * Get a node by its id
	 * 
	 * @param node
	 *            the node which must either have an {@link Node#getUrl() url} or an {@link Node#getId() id}.
	 * @return the identified node
	 */
	public Node getNode(Node node, IProgressMonitor monitor) throws CoreException;

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
	public SearchResult search(Market market, Category category, String queryText, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * Find featured nodes in the marketplace
	 * 
	 * @return the search result
	 */
	public SearchResult featured(IProgressMonitor monitor) throws CoreException;

	/**
	 * Find featured nodes in the marketplace
	 * 
	 * @param market
	 *            the market in which to return featured, or null if featured should include all markets
	 * @param category
	 *            the category in which to return fetured, or null if featured should include all categories
	 * @return the search result
	 */
	public SearchResult featured(IProgressMonitor monitor, Market market, Category category) throws CoreException;

	/**
	 * Find recently added/modified nodes in the marketplace
	 * 
	 * @return the search result
	 */
	public SearchResult recent(IProgressMonitor monitor) throws CoreException;

	/**
	 * Find most-favorited nodes in the marketplace
	 * 
	 * @return the search result
	 */
	public SearchResult favorites(IProgressMonitor monitor) throws CoreException;

	/**
	 * Find most active nodes in the marketplace
	 * 
	 * @return the search result
	 */
	public SearchResult popular(IProgressMonitor monitor) throws CoreException;
}
