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

import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;

public class UserFavoritesUnsupportedActionItem extends UserActionViewerItem<UserActionCatalogItem> {

	private final MarketplacePage marketplacePage;

	public UserFavoritesUnsupportedActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, MarketplacePage marketplacePage) {
		super(parent, resources, shellProvider, element, marketplacePage.getViewer());
		this.marketplacePage = marketplacePage;
		createContent();
	}

	@Override
	protected String getLinkText() {
		return "Favorites are not supported on this Marketplace. <a>Go back</a>";
	}

	@Override
	protected void actionPerformed(Object data) {
		marketplacePage.setPreviouslyActiveTab();
	}
}
