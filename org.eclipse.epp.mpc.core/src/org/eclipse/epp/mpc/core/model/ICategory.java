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
package org.eclipse.epp.mpc.core.model;

import java.util.List;

import org.eclipse.epp.mpc.core.service.IMarketplaceService;

/**
 * Marketplaces are categorized in {@link IMarket markets} and {@link ICategory categories}. Each category can occur in
 * one or more markets and can be associated with any number of {@link #getNode() nodes}.
 *
 * @author David Green
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICategory extends IIdentifiable {

	/**
	 * @return the number of {@link #getNode() nodes} in this category
	 */
	Integer getCount();

	/**
	 * A list of nodes for this category. Entries in this list are typically not fully realized. They will only have
	 * their {@link INode#getId() ids} and {@link INode#getName() names} set. Use
	 * {@link IMarketplaceService#getNode(INode, org.eclipse.core.runtime.IProgressMonitor) the marketplace service} to
	 * retrieve a fully realized node instance from the marketplace server.
	 *
	 * @return the list of nodes in this category.
	 */
	List<? extends INode> getNode();

}