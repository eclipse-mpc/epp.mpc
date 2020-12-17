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

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiResources;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.swt.widgets.Composite;

public class OpenFavoritesNotificationItem extends AbstractUserActionItem {

	private final MarketplacePage marketplacePage;

	public OpenFavoritesNotificationItem(Composite parent, MarketplaceDiscoveryResources resources,
			UserActionCatalogItem connector, MarketplacePage page) {
		super(parent, resources, connector, page.getViewer());
		this.marketplacePage = page;
	}

	@Override
	protected boolean alignIconWithName() {
		return true;
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiResources.DEFAULT_MARKETPLACE_ICON;
		return createIcon(path);
	}

	@Override
	protected String getDescriptionText() {
		return Messages.OpenFavoritesNotificationItem_description;
	}

	@Override
	protected String getNameLabelText() {
		return Messages.OpenFavoritesNotificationItem_title;
	}

	@Override
	protected void createButtons(Composite parent) {
		createButton(parent, Messages.OpenFavoritesNotificationItem_InstallButtonLabel, null, 0);
	}

	@Override
	protected void buttonPressed(int id) {
		marketplacePage.setActiveTab(ContentType.FAVORITES);
	}
}
