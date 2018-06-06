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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.statushandlers.StatusManager;

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
		try {
			viewer.getWizard().getContainer().run(true, true, monitor -> catalog.userFavorites(true, monitor));
		} catch (InvocationTargetException e) {
			MarketplaceClientUi.handle(e.getCause(), StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		} catch (InterruptedException e) {
			//ignore
		}
		viewer.updateContents();
	}
}
