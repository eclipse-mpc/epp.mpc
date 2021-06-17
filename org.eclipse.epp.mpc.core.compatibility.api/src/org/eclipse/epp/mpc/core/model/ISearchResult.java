/*******************************************************************************
 * Copyright (c) 2014, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.model;

import java.util.List;

import org.eclipse.epp.mpc.core.service.IMarketplaceService;

/**
 * Search results returned by the marketplace server through the {@link IMarketplaceService}. The actual number of
 * returned nodes might be less than the number of matches on the server. In that case, the {@link #getMatchCount()
 * match count} reflects the number of actual matches and the {@link #getNodes() returned results} are a subset of all
 * matches.
 *
 * @author David Green
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISearchResult {

	/**
	 * The number of matches that matched the query, which may not be equal to the number of nodes returned.
	 */
	Integer getMatchCount();

	/**
	 * The nodes that were matched by the query
	 */
	List<? extends INode> getNodes();

}