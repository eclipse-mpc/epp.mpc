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

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;

public class ImportFavoritesWizard extends DiscoveryWizard {

	private final ImportFavoritesPage importFavoritesPage;

	private String initialFavoritesUrl;

	public ImportFavoritesWizard(MarketplaceCatalog catalog, MarketplaceCatalogConfiguration configuration, IMarketplaceWebBrowser browser) {
		super(catalog, configuration);
		setWindowTitle("Import Favorites List");
		this.importFavoritesPage = new ImportFavoritesPage(catalog, browser);
	}

	@Override
	public void addPages() {
		addPage(importFavoritesPage);
	}

	@Override
	public boolean performFinish() {
		importFavoritesPage.performImport();
		return importFavoritesPage.getErrorMessage() == null;
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
