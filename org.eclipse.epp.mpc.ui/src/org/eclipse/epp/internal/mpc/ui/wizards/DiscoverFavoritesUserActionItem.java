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
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.swt.widgets.Composite;

class DiscoverFavoritesUserActionItem extends AbstractUserActionItem {

	public DiscoverFavoritesUserActionItem(Composite parent, MarketplaceDiscoveryResources resources, UserActionCatalogItem element,
			CatalogViewer viewer) {
		super(parent, resources, element, viewer);
	}

	@Override
	protected String getDescriptionText() {
		return getData().getDescription();
	}

	@Override
	protected String getNameLabelText() {
		return getData().getName();
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiResources.ACTION_ICON_FAVORITES;
		return createIcon(path);
	}

	@Override
	protected String getSublineText() {
		return Messages.DiscoverFavoritesUserActionItem_subline;
	}
}