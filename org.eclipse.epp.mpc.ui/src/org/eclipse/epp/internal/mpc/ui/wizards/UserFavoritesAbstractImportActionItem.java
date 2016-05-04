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
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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
				if (marketplaceStrategy.hasUserFavoritesService()) {
					importFavorites(marketplaceStrategy);
					return;
				}
			}
		}
	}

	private void importFavorites(MarketplaceDiscoveryStrategy marketplaceStrategy) {
		MarketplaceWizard wizard = marketplacePage.getWizard();
		FavoritesCatalog favoritesCatalog = new FavoritesCatalog();
		final ImportFavoritesPage importFavoritesPage = new ImportFavoritesPage(favoritesCatalog);
		favoritesCatalog.getDiscoveryStrategies().add(new FavoritesDiscoveryStrategy(marketplaceStrategy) {
			private String errorMessage;
			@Override
			protected void preDiscovery() {
				errorMessage = null;
			}

			@Override
			protected void handleDiscoveryError(CoreException ex) throws CoreException {
				if (UserFavoritesService.isInvalidFavoritesListException(ex)) {
					String favoritesReference = getFavoritesReference();
					boolean isUrl = (favoritesReference != null && (favoritesReference.toLowerCase().startsWith("http:")
							|| favoritesReference.toLowerCase().startsWith("https:")));
					if (isUrl) {
						errorMessage = NLS.bind("No favorites list found at {0}", favoritesReference);
					} else {
						errorMessage = NLS.bind("No favorites list found for {0}", favoritesReference);
					}
				} else {
					String message = null;
					IStatus statusCause = MarketplaceClientCore.computeWellknownProblemStatus(ex);
					if (statusCause != null) {
						message = statusCause.getMessage();
					} else if (ex.getMessage() != null && !"".equals(ex.getMessage())) {
						message = ex.getMessage();
					} else {
						message = ex.getClass().getSimpleName();
					}
					errorMessage = NLS.bind("Failed to load favorites list: {0}", message);
					MarketplaceClientCore.error(NLS.bind("Failed to load favorites list: {0}", message), ex);
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
