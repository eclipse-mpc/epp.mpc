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

public class UserFavoritesInstallAllActionItem extends UserFavoritesAbstractImportActionItem {

	private static final String INSTALL_ALL_ACTION_ID = "installAll"; //$NON-NLS-1$

	public UserFavoritesInstallAllActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, MarketplacePage page) {
		super(parent, resources, shellProvider, element, page);
	}

	@Override
	protected void secondaryActionPerformed() {
		installAll();
	}

	protected void installAll() {
		if (getViewer().getContentType() == ContentType.FAVORITES) {
			List<CatalogItem> items = getViewer().getCatalog().getItems();
			SelectionModel selectionModel = getViewer().getSelectionModel();
			for (CatalogItem catalogItem : items) {
				if (catalogItem instanceof MarketplaceNodeCatalogItem) {
					MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) catalogItem;
					if (selectionModel.getSelectedOperation(nodeItem) != Operation.INSTALL
							&& nodeItem.getAvailableOperations().contains(Operation.INSTALL)) {
						getViewer().modifySelection(nodeItem, Operation.INSTALL);
					}
				}
			}
		}
	}

	@Override
	protected ActionLink createSecondaryActionLink() {
		return new ActionLink(INSTALL_ALL_ACTION_ID, Messages.UserFavoritesInstallAllActionItem_installAllActionLabel,
				Messages.UserFavoritesInstallAllActionItem_installAllTooltip);
	}

	@Override
	protected MarketplaceViewer getViewer() {
		return (MarketplaceViewer) super.getViewer();
	}

}
