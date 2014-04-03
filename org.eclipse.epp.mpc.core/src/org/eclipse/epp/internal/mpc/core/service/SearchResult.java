/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.util.List;

import org.eclipse.epp.mpc.core.model.ISearchResult;

/**
 * @author David Green
 */
public class SearchResult implements ISearchResult {

	private Integer matchCount;

	private List<Node> nodes;

	/**
	 * The number of matches that matched the query, which may not be equal to the number of nodes returned.
	 */
	public Integer getMatchCount() {
		return matchCount;
	}

	/**
	 * The number of matches that matched the query, which may not be equal to the number of nodes returned.
	 */
	public void setMatchCount(Integer matchCount) {
		this.matchCount = matchCount;
	}

	/**
	 * The nodes that were matched by the query
	 */
	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * The nodes that were matched by the query
	 */
	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

}
