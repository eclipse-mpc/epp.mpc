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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;

public class UserFavoritesSignInActionItem extends AbstractUserActionItem {

	public UserFavoritesSignInActionItem(Composite parent, MarketplaceDiscoveryResources resources,
			UserActionCatalogItem connector, CatalogViewer viewer) {
		super(parent, resources, connector, viewer);
	}

	@Override
	protected String getNameLabelText() {
		return Messages.SignInUserActionItem_signInActionLabel;
	}

	@Override
	protected String getDescriptionText() {
		UserActionCatalogItem loginItem = getData();
		if (loginItem != null && loginItem.getData() != null && !"".equals(loginItem.getData())) { //$NON-NLS-1$
			String loginMessage = (String) loginItem.getData();
			loginMessage = loginMessage.trim();
			return NLS.bind(Messages.SignInUserActionItem_retryLoginLabel, loginMessage);
		}

		return Messages.UserFavoritesSignInActionItem_SignInDescription;
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiPlugin.ACTION_ICON_LOGIN;
		return createIcon(path);
	}

	@Override
	protected String getSublineText() {
		return Messages.UserFavoritesSignInActionItem_subline;
	}

	@Override
	protected void createButtons(Composite parent) {
		createButton(parent, Messages.UserFavoritesSignInActionItem_SignInButtonText, null, 0);
	}

	@Override
	protected void buttonPressed(int id) {
		MarketplaceViewer viewer = (MarketplaceViewer) getViewer();
		final MarketplaceCatalog catalog = viewer.getCatalog();
		catalog.userFavorites(true, new NullProgressMonitor());
		viewer.updateContents();
	}
}
