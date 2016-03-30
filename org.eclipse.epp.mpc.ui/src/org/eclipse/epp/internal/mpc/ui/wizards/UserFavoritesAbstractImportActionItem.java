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

import java.util.List;

import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;

public abstract class UserFavoritesAbstractImportActionItem extends AbstractUserActionLinksItem {

	private static final String IMPORT_ACTION_ID = "import"; //$NON-NLS-1$

	private final MarketplacePage marketplacePage;

	public UserFavoritesAbstractImportActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, MarketplacePage page) {
		super(parent, resources, shellProvider, element, page.getViewer());
		createContent(
				new ActionLink(IMPORT_ACTION_ID,
						Messages.UserFavoritesAbstractImportActionItem_importFavoritesActionLabel,
						Messages.UserFavoritesAbstractImportActionItem_importFavoritesTooltip),
				createSecondaryActionLink());
		this.marketplacePage = page;
	}

	protected void importFavorites() {
		List<AbstractDiscoveryStrategy> discoveryStrategies = getViewer().getCatalog().getDiscoveryStrategies();
		for (AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			if (strategy instanceof MarketplaceDiscoveryStrategy) {
				MarketplaceDiscoveryStrategy marketplaceStrategy = (MarketplaceDiscoveryStrategy) strategy;
				IMarketplaceService marketplaceService = marketplaceStrategy.getMarketplaceService();
				IUserFavoritesService userFavoritesService = marketplaceService.getUserFavoritesService();
				if (userFavoritesService != null) {
					importFavorites(marketplaceStrategy);
					return;
				}
			}
		}
	}

	private void importFavorites(MarketplaceDiscoveryStrategy marketplaceStrategy) {
		MarketplaceWizard wizard = marketplacePage.getWizard();
		FavoritesCatalog favoritesCatalog = new FavoritesCatalog();
		favoritesCatalog.getDiscoveryStrategies().add(new FavoritesDiscoveryStrategy(marketplaceStrategy));
		ImportFavoritesPage importFavoritesPage = new ImportFavoritesPage(favoritesCatalog);
		importFavoritesPage.setWizard(wizard);
		wizard.getContainer().showPage(importFavoritesPage);
	}

	@Override
	protected final void actionPerformed(Object data) {
		if (IMPORT_ACTION_ID.equals(data)) {
			importFavorites();
		} else {
			secondaryActionPerformed();
		}
	}

	protected abstract void secondaryActionPerformed();

	protected abstract ActionLink createSecondaryActionLink();
}
