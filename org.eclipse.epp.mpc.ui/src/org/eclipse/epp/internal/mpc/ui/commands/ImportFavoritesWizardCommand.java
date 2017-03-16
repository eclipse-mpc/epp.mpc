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
package org.eclipse.epp.internal.mpc.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.wizards.ImportFavoritesPage;
import org.eclipse.epp.internal.mpc.ui.wizards.ImportFavoritesWizard;
import org.eclipse.epp.internal.mpc.ui.wizards.ImportFavoritesWizardDialog;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceCatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;

public class ImportFavoritesWizardCommand extends AbstractMarketplaceWizardCommand {

	private static final String FAVORITES_URL_PARAMETER = "favoritesUrl"; //$NON-NLS-1$

	private String favoritesUrl;

	@Override
	protected ImportFavoritesWizardDialog createWizardDialog(DiscoveryWizard wizard, ExecutionEvent event) {
		return new ImportFavoritesWizardDialog(WorkbenchUtil.getShell(), wizard);
	}

	@Override
	protected ImportFavoritesWizard createWizard(MarketplaceCatalog catalog,
			MarketplaceCatalogConfiguration configuration, ExecutionEvent event) {
		String favoritesUrl = event.getParameter(FAVORITES_URL_PARAMETER);
		if (favoritesUrl == null) {
			favoritesUrl = this.favoritesUrl;
		}

		FavoritesCatalog favoritesCatalog = new FavoritesCatalog();

		ImportFavoritesWizard wizard = new ImportFavoritesWizard(favoritesCatalog, configuration, null);
		wizard.setInitialFavoritesUrl(favoritesUrl);
		final ImportFavoritesPage importFavoritesPage = wizard.getImportFavoritesPage();

		favoritesCatalog.getDiscoveryStrategies()
		.add(new FavoritesDiscoveryStrategy(configuration.getCatalogDescriptor()) {
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
		return wizard;
	}

	public void setFavoritesUrl(String favoritesUrl) {
		this.favoritesUrl = favoritesUrl;
	}

	public String getFavoritesUrl() {
		return favoritesUrl;
	}

}
