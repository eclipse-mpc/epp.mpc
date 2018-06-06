/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.List;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceInfo;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;

public class InstallUpdatesNotificationItem extends AbstractUserActionItem {

	private final MarketplacePage marketplacePage;

	private boolean mpcSelfUpdate;

	private boolean generalUpdate;

	public InstallUpdatesNotificationItem(Composite parent, MarketplaceDiscoveryResources resources,
			UserActionCatalogItem connector, MarketplacePage page) {
		super(parent, resources, connector, page.getViewer());
		this.marketplacePage = page;
	}

	@SuppressWarnings("unchecked")
	private List<MarketplaceNodeCatalogItem> getAvailableUpdates() {
		return (List<MarketplaceNodeCatalogItem>) connector.getData();
	}

	@Override
	protected void createContent() {
		mpcSelfUpdate = computeIsMPCSelfUpdate();
		generalUpdate = computeIsGeneralUpdate();
		super.createContent();
	}

	private boolean computeIsMPCSelfUpdate() {
		List<MarketplaceNodeCatalogItem> availableUpdates = getAvailableUpdates();
		for (MarketplaceNodeCatalogItem item : availableUpdates) {
			if (MarketplaceInfo.isMPCNode(item.getData())) {
				return true;
			}
		}
		return false;
	}

	private boolean computeIsGeneralUpdate() {
		List<MarketplaceNodeCatalogItem> availableUpdates = getAvailableUpdates();
		return availableUpdates.size() > 1 || !mpcSelfUpdate;
	}

	public boolean isMpcSelfUpdate() {
		return mpcSelfUpdate;
	}

	public boolean isGeneralUpdate() {
		return generalUpdate;
	}

	@Override
	protected boolean alignIconWithName() {
		return true;
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiPlugin.ACTION_ICON_UPDATE;
		return createIcon(path);
	}

	@Override
	protected String getDescriptionText() {
		return isMpcSelfUpdate() ? Messages.InstallUpdatesNotificationItem_MPCUpdateDescription
				: Messages.InstallUpdatesNotificationItem_GeneralUpdateDescription;
	}

	@Override
	protected String getSublineText() {
		return isMpcSelfUpdate() && isGeneralUpdate() ? Messages.InstallUpdatesNotificationItem_OtherUpdatesDescription
				: null;
	}

	@Override
	protected String getNameLabelText() {
		return isMpcSelfUpdate() ? Messages.InstallUpdatesNotificationItem_MPCUpdateHeader
				: Messages.InstallUpdatesNotificationItem_GeneralUpdateHeader;
	}

	@Override
	protected void createButtons(Composite parent) {
		createButton(parent, Messages.InstallUpdatesNotificationItem_ShowUpdatesAction, null, 0);
		createButton(parent, Messages.InstallUpdatesNotificationItem_UpdateNowAction, null, 1);
	}

	@Override
	protected void buttonPressed(int id) {
		switch (id) {
		case 0:
			marketplacePage.setActiveTab(ContentType.INSTALLED);
			break;
		case 1:
			triggerUpdate();
			break;
		}
	}

	private void triggerUpdate() {
		MarketplaceViewer viewer = marketplacePage.getViewer();

		//We need to first select the items in the selection model and then
		//set the selection to the viewer. Otherwise the MarketplacePage listener
		//will advance the wizard on the first selected item.
		SelectionModel selectionModel = viewer.getSelectionModel();
		for (MarketplaceNodeCatalogItem nodeItem : getAvailableUpdates()) {
			if (selectionModel.getSelectedOperation(nodeItem) == org.eclipse.epp.mpc.ui.Operation.NONE) {
				selectionModel.select(nodeItem, org.eclipse.epp.mpc.ui.Operation.UPDATE);
			}
		}
		//viewer.getCheckedItems() is based on the SelectionModel state, so it already has the
		//updated selection. Just let the viewer synchronize its remaining selection state with it.
		viewer.setSelection(new StructuredSelection(viewer.getCheckedItems()));
		if (!viewer.getSelection().isEmpty()) {
			marketplacePage.showNextPage();
		}
	}

}
