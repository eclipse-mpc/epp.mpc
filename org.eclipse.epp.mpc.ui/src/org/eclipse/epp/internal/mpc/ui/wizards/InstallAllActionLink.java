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
import org.eclipse.jface.viewers.StructuredSelection;

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
		//We need to first select the items in the selection model and then
		//set the selection to the viewer. Otherwise the MarketplacePage listener
		//will advance the wizard on the first selected item.
		SelectionModel selectionModel = viewer.getSelectionModel();
		for (CatalogItem catalogItem : items) {
			if (catalogItem instanceof MarketplaceNodeCatalogItem) {
				MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) catalogItem;
				if (selectionModel.getSelectedOperation(nodeItem) == null) {
					selectionModel.select(nodeItem, Operation.INSTALL);
				}
			}
		}
		//viewer.getCheckedItems() is based on the SelectionModel state, so it already has the
		//updated selection. Just let the viewer synchronize its remaining selection state with it.
		viewer.setSelection(new StructuredSelection(viewer.getCheckedItems()));
	}
}
