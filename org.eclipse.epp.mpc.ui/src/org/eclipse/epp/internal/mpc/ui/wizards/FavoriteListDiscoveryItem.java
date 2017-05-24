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

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoriteListCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class FavoriteListDiscoveryItem extends AbstractSimpleDiscoveryItem<FavoriteListCatalogItem> {
	public FavoriteListDiscoveryItem(Composite parent, MarketplaceDiscoveryResources resources,
			FavoriteListCatalogItem connector, FavoritesViewer viewer) {
		super(parent, SWT.NONE, resources, null, connector, viewer);
	}

	@Override
	protected FavoritesViewer getViewer() {
		return (FavoritesViewer) super.getViewer();
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiPlugin.FAVORITES_LIST_ICON;
		return createIcon(path);
	}

	@Override
	protected String getDescriptionText() {
		String description = connector.getDescription();
		return description;
	}

	@Override
	protected String getNameLabelText() {
		return connector.getListName();
	}

	@Override
	protected String getSublineText() {
		return NLS.bind("by {0}", connector.getOwner());
	}

	@Override
	protected void createButtons(Composite parent) {
		createButton(parent, "Show", "Open this favorite list", 0);
	}

	@Override
	protected void buttonPressed(int id) {
		getViewer().setFavoritesUrl(connector.getFavoriteList().getUrl());
	}
}
