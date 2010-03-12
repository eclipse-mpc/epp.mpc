/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUIPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * @author David Greenx
 */
public class MarketplaceWizard extends DiscoveryWizard {

	private static final String PREF_DEFAULT_CATALOG = CatalogDescriptor.class.getSimpleName();

	private CatalogSelectionPage catalogSelectionPage;

	public MarketplaceWizard(MarketplaceCatalog catalog, MarketplaceCatalogConfiguration configuration) {
		super(catalog, configuration);
		setWindowTitle("Eclipse Solution Catalogs");
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
	protected CatalogPage doCreateCatalogPage() {
		return new MarketplacePage(getCatalog(), getConfiguration());
	}

	@Override
	public void addPages() {
		doDefaultCatalogSelection();
		if (getConfiguration().getCatalogDescriptors().size() > 1) {
			addPage(getCatalogSelectionPage());
		}
		super.addPages();
	}

	public CatalogSelectionPage getCatalogSelectionPage() {
		if (catalogSelectionPage == null) {
			catalogSelectionPage = new CatalogSelectionPage(getConfiguration());
		}
		return catalogSelectionPage;
	}

	@Override
	public IWizardPage getStartingPage() {
		if (getConfiguration().getCatalogDescriptor() != null) {
			return getCatalogPage();
		}
		return super.getStartingPage();
	}

	private void doDefaultCatalogSelection() {
		if (getConfiguration().getCatalogDescriptor() == null) {
			String defaultCatalogUrl = MarketplaceClientUIPlugin.getInstance().getPreferenceStore().getString(
					PREF_DEFAULT_CATALOG);
			// if a preferences was set, we default to that catalog descriptor
			if (defaultCatalogUrl != null && defaultCatalogUrl.length() > 0) {
				for (CatalogDescriptor descriptor : getConfiguration().getCatalogDescriptors()) {
					URL url = descriptor.getUrl();
					try {
						if (url.toURI().toString().equals(defaultCatalogUrl)) {
							getConfiguration().setCatalogDescriptor(descriptor);
							break;
						}
					} catch (URISyntaxException e) {
						// ignore
					}
				}
			}
			// if no preference was set or we could not find the descriptor then we default to eclipse.org
			if (getConfiguration().getCatalogDescriptor() == null) {
				for (CatalogDescriptor descriptor : getConfiguration().getCatalogDescriptors()) {
					URL url = descriptor.getUrl();

					String host = url.getHost();
					if (host.endsWith(".eclipse.org")) {
						getConfiguration().setCatalogDescriptor(descriptor);
						break;
					}
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (getConfiguration().getCatalogDescriptor() != null) {
			// remember the catalog for next time.
			try {
				MarketplaceClientUIPlugin.getInstance().getPreferenceStore().setValue(PREF_DEFAULT_CATALOG,
						getConfiguration().getCatalogDescriptor().getUrl().toURI().toString());
			} catch (URISyntaxException e) {
				// ignore
			}
		}
		super.dispose();
	}
}
