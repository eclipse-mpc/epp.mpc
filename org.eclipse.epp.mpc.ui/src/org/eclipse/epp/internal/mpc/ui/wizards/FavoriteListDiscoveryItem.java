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
import org.eclipse.epp.internal.mpc.ui.catalog.FavoriteListCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class FavoriteListDiscoveryItem extends AbstractSimpleDiscoveryItem<FavoriteListCatalogItem> {
	private static final Icon DEFAULT_LIST_ICON = createIcon(
			REGISTRY_SCHEME + MarketplaceClientUiResources.FAVORITES_LIST_ICON);

	public FavoriteListDiscoveryItem(Composite parent, MarketplaceDiscoveryResources resources,
			FavoriteListCatalogItem connector, FavoritesViewer viewer) {
		super(parent, SWT.NONE, resources, null, connector, viewer);
	}

	@Override
	protected FavoritesViewer getViewer() {
		return (FavoritesViewer) super.getViewer();
	}

	@Override
	protected String getItemClass() {
		return "FavoriteListItem";
	}

	@Override
	protected String getItemId() {
		return "favorite-" + connector.getId();
	}

	@Override
	protected Icon getIcon() {
		Icon icon = connector.getIcon();
		if (icon != null) {
			return icon;
		}
		return DEFAULT_LIST_ICON;
	}

	@Override
	protected String getDefaultIconResourceId() {
		return MarketplaceClientUiResources.FAVORITES_LIST_ICON;
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

	@Override
	protected StyledText createSublineLabel(Composite parent) {
		StyledText subline = super.createSublineLabel(parent);
		if (connector.getOwnerProfileUrl() != null) {
			StyleRange range = new StyleRange(0, subline.getText().length(), subline.getForeground(), null, SWT.NONE);
			subline.setStyleRange(range);//reset styling
			configureProviderLink(subline, "by {0}", connector.getOwner(), connector.getOwnerProfileUrl(),
					new LinkListener() {

				@Override
				protected void selected(Object href, TypedEvent event) {
					WorkbenchUtil.openUrl((String) href, IWorkbenchBrowserSupport.AS_EXTERNAL);
				}
			});
		}
		return subline;
	}
}
