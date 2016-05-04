/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.List;

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

public class InstallAllActionLink extends ActionLink {

	private static final String INSTALL_ALL_ACTION_ID = "installAll"; //$NON-NLS-1$

	private final MarketplacePage marketplacePage;

	public InstallAllActionLink(MarketplacePage marketplacePage) {
		super(INSTALL_ALL_ACTION_ID, Messages.UserFavoritesInstallAllActionItem_installAllActionLabel,
				Messages.UserFavoritesInstallAllActionItem_installAllTooltip);
		this.marketplacePage = marketplacePage;
	}

	@Override
	public void selected() {
		installAll();
	}

	protected void installAll() {
		MarketplaceViewer viewer = marketplacePage.getViewer();
		List<CatalogItem> items = viewer.getCatalog().getItems();
		SelectionModel selectionModel = viewer.getSelectionModel();
		for (CatalogItem catalogItem : items) {
			if (catalogItem instanceof MarketplaceNodeCatalogItem) {
				MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) catalogItem;
				if (selectionModel.getSelectedOperation(nodeItem) != Operation.INSTALL
						&& nodeItem.getAvailableOperations().contains(Operation.INSTALL)) {
					viewer.modifySelection(nodeItem, Operation.INSTALL);
				}
			}
		}
	}
}
