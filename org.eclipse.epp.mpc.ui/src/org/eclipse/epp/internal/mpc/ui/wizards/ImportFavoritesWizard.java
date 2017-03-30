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
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ImportFavoritesWizard extends DiscoveryWizard {

	private final ImportFavoritesPage importFavoritesPage;

	private String initialFavoritesUrl;

	public ImportFavoritesWizard(MarketplaceCatalog catalog, MarketplaceCatalogConfiguration configuration, IMarketplaceWebBrowser browser) {
		super(catalog, configuration);
		setWindowTitle(Messages.ImportFavoritesWizard_title);
		this.importFavoritesPage = new ImportFavoritesPage(catalog, browser);
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
		Display display = null;
		Rectangle bounds = null;
		IWizardContainer container = getContainer();
		if (container instanceof WizardDialog) {
			Shell shell = ((WizardDialog) container).getShell();
			if (shell != null && !shell.isDisposed()) {
				bounds = shell.getBounds();
				display = shell.getDisplay();
			}
		}
		showFavoritesInMarketplace(display, bounds);
	}

	private void showFavoritesInMarketplace(Display display, Rectangle bounds) {
		// ignore

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
