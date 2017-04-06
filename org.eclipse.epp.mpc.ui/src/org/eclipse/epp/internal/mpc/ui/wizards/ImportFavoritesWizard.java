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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.swt.widgets.Display;

public class ImportFavoritesWizard extends DiscoveryWizard {

	private final ImportFavoritesPage importFavoritesPage;

	private String initialFavoritesUrl;

	private final MarketplaceWizard parent;

	public ImportFavoritesWizard(MarketplaceCatalog catalog, MarketplaceCatalogConfiguration configuration,
			MarketplaceWizard parent) {
		super(catalog, configuration);
		setWindowTitle(Messages.ImportFavoritesWizard_title);
		this.importFavoritesPage = new ImportFavoritesPage(catalog);
		this.parent = parent;
	}

	@Override
	public MarketplaceCatalogConfiguration getConfiguration() {
		return (MarketplaceCatalogConfiguration) super.getConfiguration();
	}

	@Override
	public MarketplaceCatalog getCatalog() {
		return (MarketplaceCatalog) super.getCatalog();
	}

	@Override
	public void addPages() {
		addPage(importFavoritesPage);
	}

	@Override
	public boolean performFinish() {
		importFavoritesPage.performImport();
		boolean result = importFavoritesPage.getErrorMessage() == null;
		if (result) {
			showFavoritesInMarketplace();
		}
		return result;
	}

	private void showFavoritesInMarketplace() {
		List<MarketplaceNodeCatalogItem> selection = importFavoritesPage.getSelection();
		if (selection.isEmpty()) {
			return;
		}
		if (parent == null) {
			openFavoritesInMarketplace(selection);
		} else {
			selectForInstallation(selection);
		}
	}

	private void selectForInstallation(List<MarketplaceNodeCatalogItem> selection) {
		for (MarketplaceNodeCatalogItem item : selection) {
			parent.getSelectionModel().select(item, Operation.INSTALL);
		}
	}

	private void openFavoritesInMarketplace(List<MarketplaceNodeCatalogItem> selection) {
		final IMarketplaceClientService clientService = MarketplaceClient.getMarketplaceClientService();
		final IMarketplaceClientConfiguration config = clientService.newConfiguration();
		MarketplaceCatalogConfiguration catalogConfiguration = getConfiguration();
		config.setCatalogDescriptors(catalogConfiguration.getCatalogDescriptors());
		config.setCatalogDescriptor(catalogConfiguration.getCatalogDescriptor());
		Map<String, Operation> initialOperations = new HashMap<String, Operation>();
		for (MarketplaceNodeCatalogItem item : selection) {
			initialOperations.put(item.getData().getId(), Operation.INSTALL);
		}
		config.setInitialOperations(initialOperations);
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				clientService.openFavorites(config);
			}
		});
	}

	public ImportFavoritesPage getImportFavoritesPage() {
		return importFavoritesPage;
	}

	public void setInitialFavoritesUrl(String initialFavoritesUrl) {
		this.initialFavoritesUrl = initialFavoritesUrl;
	}

	public String getInitialFavoritesUrl() {
		return initialFavoritesUrl;
	}
}
