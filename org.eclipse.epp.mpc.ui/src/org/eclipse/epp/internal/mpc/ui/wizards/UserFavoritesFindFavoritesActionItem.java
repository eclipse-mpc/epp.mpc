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

public class UserFavoritesFindFavoritesActionItem extends UserFavoritesAbstractImportActionItem {

	private static final String BROWSE_ACTION_ID = "browse"; //$NON-NLS-1$
	private final MarketplacePage marketplacePage;

	public UserFavoritesFindFavoritesActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, MarketplacePage marketplacePage) {
		super(parent, resources, shellProvider, element, marketplacePage);
		this.marketplacePage = marketplacePage;
	}

	@Override
	protected String getDescriptionText() {
		return Messages.UserFavoritesFindFavoritesActionItem_noFavoritesYetMessage;
	}

	@Override
	protected ActionLink createSecondaryActionLink() {
		return new ActionLink(BROWSE_ACTION_ID,
				Messages.UserFavoritesFindFavoritesActionItem_browseMarketplaceActionLabel,
				Messages.UserFavoritesFindFavoritesActionItem_browseMarketplaceTooltip);
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
