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
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class UserFavoritesFindFavoritesActionItem extends UserFavoritesAbstractImportActionItem {

	private final MarketplacePage marketplacePage;

	public UserFavoritesFindFavoritesActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, MarketplacePage marketplacePage) {
		super(parent, resources, shellProvider, element, marketplacePage.getViewer());
		this.marketplacePage = marketplacePage;
		createContent();
	}

	@Override
	protected String getDescriptionText() {
		return "You don't have any favorites yet. Choose some favorite on the marketplace to see them here.";
	}

	@Override
	protected Control createSecondaryActionLink(Composite parent) {
		return createActionLink(parent, "<a href=\"browse\">Browse Marketplace</a>",
				"Find some interesting entries in the Marketplace");
	}

	@Override
	protected void secondaryActionPerformed() {
		MarketplaceWizard wizard = marketplacePage.getWizard();
		IWizardPage currentPage = wizard.getContainer().getCurrentPage();
		if (currentPage == marketplacePage && viewer.getContentType() == ContentType.FAVORITES) {
			marketplacePage.setActiveTab(ContentType.SEARCH);
		}
	}
}
