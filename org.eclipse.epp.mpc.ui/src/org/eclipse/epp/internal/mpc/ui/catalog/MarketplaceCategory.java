/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.util.List;

import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.epp.internal.mpc.core.service.SearchResult;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;

/**
 * @author David Green
 */
public class MarketplaceCategory extends CatalogCategory {

	public enum Contents {
		FEATURED, POPULAR, INSTALLED, RECENT, QUERY
	}

	private List<Market> markets;

	private Contents contents;

	private int matchCount;

	public void setMarkets(List<Market> markets) {
		this.markets = markets;
	}

	public List<Market> getMarkets() {
		return markets;
	}

	/**
	 * Indicate what kind of contents are populated in this category. The marketplace catalog is large and therefore the
	 * client side model for this data is sparse.
	 */
	public Contents getContents() {
		return contents;
	}

	public void setContents(Contents contents) {
		this.contents = contents;
	}

	/**
	 * Indicate how many solutions matched the query, which may not be the same as the number of nodes returned.
	 * 
	 * @see SearchResult#getMatchCount()
	 */
	public int getMatchCount() {
		return matchCount;
	}

	public void setMatchCount(int matchCount) {
		this.matchCount = matchCount;
	}

}
