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
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class UserFavoritesInstallAllActionItem extends UserFavoritesAbstractImportActionItem {

	public UserFavoritesInstallAllActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, MarketplaceViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
	}

	@Override
	protected void secondaryActionPerformed() {
		installAll();
	}

	protected void installAll() {
		if (viewer.getContentType() == ContentType.FAVORITES) {
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

	@Override
	protected Control createSecondaryActionLink(Composite parent) {
		return createActionLink(parent, "<a href=\"installAll\">Install all...</a>",
				"Select all your favorited entries for installation");
	}

}
