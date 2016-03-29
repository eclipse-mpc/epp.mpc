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

import java.util.Collections;
import java.util.Set;

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;

public class ImportFavoritesPage extends CatalogPage {

	public ImportFavoritesPage(MarketplaceCatalog catalog) {
		super(catalog);
	}

	@Override
	public IWizardPage getNextPage() {
		return getPreviousPage();
	}

	@Override
	protected void doUpdateCatalog() {
		// ignore
		super.doUpdateCatalog();
	}

	@Override
	protected CatalogViewer doCreateViewer(Composite parent) {
		CatalogViewer viewer = new CatalogViewer(getCatalog(), this, getContainer(), getWizard().getConfiguration()) {
			@Override
			protected Set<String> getInstalledFeatures(org.eclipse.core.runtime.IProgressMonitor monitor) {
				return Collections.emptySet();
			}
		};
		viewer.setMinimumHeight(MINIMUM_HEIGHT);
		viewer.createControl(parent);
		return viewer;
	}

	private boolean visible = false;

	@Override
	public void setVisible(boolean visible) {
		boolean oldVisible = this.visible;
		super.setVisible(visible);
		this.visible = visible;
		if (!visible && oldVisible) {
			this.getWizard().getContainer().showPage(getNextPage());
		}
	}
}
