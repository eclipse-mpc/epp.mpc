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

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.swt.widgets.Composite;

public class UserFavoritesUnsupportedActionItem extends AbstractUserActionItem {

	private final MarketplacePage marketplacePage;

	public UserFavoritesUnsupportedActionItem(Composite parent, MarketplaceDiscoveryResources resources,
			UserActionCatalogItem connector, MarketplacePage marketplacePage) {
		super(parent, resources, connector, marketplacePage.getViewer());
		this.marketplacePage = marketplacePage;
	}

	@Override
	protected String getNameLabelText() {
		return Messages.UserFavoritesUnsupportedActionItem_unsupportedFavoritesLabel;
	}

	@Override
	protected String getDescriptionText() {
		return Messages.UserFavoritesUnsupportedActionItem_Body;
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiPlugin.ACTION_ICON_WARNING;
		return createIcon(path);
	}

	@Override
	protected String getSublineText() {
		return Messages.UserFavoritesUnsupportedActionItem_Subline;
	}

	@Override
	protected void createButtons(Composite parent) {
		createButton(parent, Messages.UserFavoritesUnsupportedActionItem_GoBackButtonLabel, null, 0);
	}

	@Override
	protected void buttonPressed(int id) {
		marketplacePage.setPreviouslyActiveTab();
	}
}
