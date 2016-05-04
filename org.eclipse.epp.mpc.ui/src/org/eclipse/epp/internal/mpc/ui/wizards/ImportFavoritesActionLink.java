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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.UserFavoritesService;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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
			private String errorMessage;

			@Override
			protected void preDiscovery() {
				errorMessage = null;
			}

			@Override
			protected void handleDiscoveryError(CoreException ex) throws CoreException {
				String favoritesReference = getFavoritesReference();
				if (UserFavoritesService.isInvalidFavoritesListException(ex)) {
					boolean isUrl = (favoritesReference != null && (favoritesReference.toLowerCase().startsWith("http:") //$NON-NLS-1$
							|| favoritesReference.toLowerCase().startsWith("https:"))); //$NON-NLS-1$
					if (isUrl) {
						errorMessage = NLS.bind(Messages.ImportFavoritesActionLink_noFavoritesFoundAtUrl,
								favoritesReference);
					} else {
						errorMessage = NLS.bind(Messages.ImportFavoritesActionLink_noFavoritesFoundForUser,
								favoritesReference);
					}
				} else if (UserFavoritesService.isInvalidUrlException(ex)) {
					errorMessage = NLS.bind(Messages.ImportFavoritesActionLink_invalidUrl, favoritesReference);
				} else {
					String message = null;
					IStatus statusCause = MarketplaceClientCore.computeWellknownProblemStatus(ex);
					if (statusCause != null) {
						message = statusCause.getMessage();
					} else if (ex.getMessage() != null && !"".equals(ex.getMessage())) { //$NON-NLS-1$
						message = ex.getMessage();
					} else {
						message = ex.getClass().getSimpleName();
					}
					errorMessage = NLS.bind(Messages.ImportFavoritesActionLink_errorLoadingFavorites, message);
					MarketplaceClientCore
							.error(NLS.bind(Messages.ImportFavoritesActionLink_errorLoadingFavorites, message), ex);
				}
			}

			@Override
			protected void postDiscovery() {
				final String errorMessage = this.errorMessage;
				this.errorMessage = null;
				Shell shell = importFavoritesPage.getShell();
				Control pageControl = importFavoritesPage.getControl();
				if (shell != null && !shell.isDisposed() && pageControl != null && !pageControl.isDisposed()) {
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							Shell shell = importFavoritesPage.getShell();
							Control pageControl = importFavoritesPage.getControl();
							if (shell != null && !shell.isDisposed() && pageControl != null
									&& !pageControl.isDisposed()) {
								importFavoritesPage.setErrorMessage(errorMessage);
							}
						}
					});
				}
			}
		});
		int result = new ImportFavoritesWizardDialog(wizard.getShell(), importFavoritesWizard).open();
		if (result == Window.OK) {
			MarketplacePage catalogPage = wizard.getCatalogPage();
			catalogPage.setActiveTab(ContentType.FAVORITES);
			catalogPage.reloadCatalog();
		}
	}

}
