/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;

import java.util.Set;

import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;

/**
 * This service allows opening the Eclipse Marketplace Wizard in a predefined state, e.g. to show a specific search,
 * preselected items, or start the install of selected nodes.
 * <p>
 * An instance of this class can be acquired as an OSGi service or through the {@link MarketplaceClient}
 * {@link MarketplaceClient#getMarketplaceClientService() convenience method}.
 *
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMarketplaceClientService {
	/**
	 * @return an editable configuration object for the Marketplace Wizard
	 */
	IMarketplaceClientConfiguration newConfiguration();

	/**
	 * Open the Marketplace Wizard using the given configuration. Regardless of an
	 * {@link IMarketplaceClientConfiguration#setInitialOperations(java.util.Map) initial selection} defined in the
	 * configuration, this will always launch the wizard on the initial catalog page.
	 *
	 * @param configuration
	 *            the initial configuration applied to the MPC wizard
	 */
	void open(IMarketplaceClientConfiguration configuration);

	/**
	 * Open the Marketplace Wizard showing the
	 * {@link IMarketplaceClientConfiguration#setInitialOperations(java.util.Map) initial selection} defined in the
	 * configuration.
	 *
	 * @param configuration
	 *            the initial configuration applied to the MPC wizard
	 * @throws IllegalArgumentException
	 *             if the configuration does not contain an initial selection either in
	 *             {@link IMarketplaceClientConfiguration#getInitialOperations()} or
	 *             {@link IMarketplaceClientConfiguration#getInitialState()}
	 */
	void openSelected(IMarketplaceClientConfiguration configuration);

	/**
	 * Open the Marketplace Wizard showing the "Installed" tab for the
	 * {@link IMarketplaceClientConfiguration#getCatalogDescriptor() active catalog}.
	 *
	 * @param configuration
	 *            the initial configuration applied to the MPC wizard
	 */
	void openInstalled(IMarketplaceClientConfiguration configuration);

	/**
	 * Open the Marketplace Wizard showing the result of the given search on the
	 * {@link IMarketplaceClientConfiguration#getCatalogDescriptor() active catalog}.
	 *
	 * @param configuration
	 *            the initial configuration applied to the MPC wizard
	 * @param market
	 *            the market to search in or null to search in all markets
	 * @param category
	 *            the category to search in or null to search in all categories
	 * @param query
	 *            the search terms
	 */
	void openSearch(IMarketplaceClientConfiguration configuration, IMarket market, ICategory category, String query);

	/**
	 * Open the Marketplace Wizard showing the given list of nodes in the MPC's search view.
	 *
	 * @param configuration
	 *            the initial configuration applied to the MPC wizard
	 * @param nodes
	 *            the nodes to show
	 */
	void open(IMarketplaceClientConfiguration configuration, Set<INode> nodes);

	/**
	 * Trigger the specified {@link IMarketplaceClientConfiguration#getInitialOperations() provisioning operations} in
	 * the Marketplace Wizard. This will launch the Wizard on the feature selection page, as if selecting the respective
	 * operations on the catalog page and clicking "Install Now" afterwards.
	 *
	 * @param configuration
	 *            the initial configuration applied to the MPC wizard
	 * @throws IllegalArgumentException
	 *             if the configuration does not contain an initial selection either in
	 *             {@link IMarketplaceClientConfiguration#getInitialOperations()} or
	 *             {@link IMarketplaceClientConfiguration#getInitialState()}
	 */
	void openProvisioning(IMarketplaceClientConfiguration configuration);

}
