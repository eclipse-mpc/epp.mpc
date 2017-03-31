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
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.jface.window.Window;

public class ImportFavoritesActionLink extends ActionLink {

	private static final String IMPORT_ACTION_ID = "import"; //$NON-NLS-1$

	private final MarketplacePage marketplacePage;

	public ImportFavoritesActionLink(MarketplacePage page) {
		super(IMPORT_ACTION_ID, Messages.UserFavoritesAbstractImportActionItem_importFavoritesActionLabel,
				Messages.UserFavoritesAbstractImportActionItem_importFavoritesTooltip);
		this.marketplacePage = page;
	}

	@Override
	public void selected() {
		importFavorites();
	}

	protected void importFavorites() {
		MarketplaceDiscoveryStrategy marketplaceStrategy = findMarketplaceDiscoveryStrategy();
		if (marketplaceStrategy != null && marketplaceStrategy.hasUserFavoritesService()) {
			importFavorites(marketplaceStrategy);
		}
	}

	protected MarketplaceDiscoveryStrategy findMarketplaceDiscoveryStrategy() {
		MarketplaceDiscoveryStrategy marketplaceStrategy = null;
		List<AbstractDiscoveryStrategy> discoveryStrategies = marketplacePage.getCatalog().getDiscoveryStrategies();
		for (AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			if (strategy instanceof MarketplaceDiscoveryStrategy) {
				marketplaceStrategy = (MarketplaceDiscoveryStrategy) strategy;
				break;
			}
		}
		return marketplaceStrategy;
	}

	protected void importFavorites(MarketplaceDiscoveryStrategy marketplaceStrategy) {
		MarketplaceWizard wizard = marketplacePage.getWizard();
		FavoritesCatalog favoritesCatalog = new FavoritesCatalog();

		ImportFavoritesWizard importFavoritesWizard = new ImportFavoritesWizard(favoritesCatalog,
				wizard.getConfiguration(), wizard);
		final ImportFavoritesPage importFavoritesPage = importFavoritesWizard.getImportFavoritesPage();
		favoritesCatalog.getDiscoveryStrategies().add(new FavoritesDiscoveryStrategy(marketplaceStrategy) {
			private String discoveryError = null;
			@Override
			protected void preDiscovery() {
				discoveryError = null;
			}

			@Override
			protected void handleDiscoveryError(CoreException ex) throws CoreException {
				discoveryError = ImportFavoritesPage.handleDiscoveryError(getFavoritesReference(), ex);
			}

			@Override
			protected void postDiscovery() {
				final String errorMessage = this.discoveryError;
				this.discoveryError = null;
				importFavoritesPage.setDiscoveryError(errorMessage);
			}
		});
		ImportFavoritesWizardDialog importWizard = new ImportFavoritesWizardDialog(wizard.getShell(), importFavoritesWizard);

		Map<String, Operation> oldOperations = wizard.getSelectionModel().getItemIdToSelectedOperation();
		int result = importWizard.open();
		if (result == Window.OK) {
			MarketplacePage catalogPage = wizard.getCatalogPage();
			catalogPage.setActiveTab(ContentType.FAVORITES);
			catalogPage.reloadCatalog();
			Map<String, Operation> newOperations = wizard.getSelectionModel().getItemIdToSelectedOperation();
			if (!newOperations.equals(oldOperations)) {
				wizard.getCatalogPage().setPageComplete(!newOperations.isEmpty());
			}
		}
	}

}
