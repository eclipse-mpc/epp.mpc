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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.userstorage.util.ConflictException;

public class ImportFavoritesPage extends CatalogPage {

	private final IMarketplaceWebBrowser browser;

	public ImportFavoritesPage(MarketplaceCatalog catalog, IMarketplaceWebBrowser browser) {
		super(catalog);
		this.browser = browser;
		setTitle(Messages.ImportFavoritesPage_Title);
		setDescription(Messages.ImportFavoritesPage_Description);
	}

	@Override
	protected CatalogViewer doCreateViewer(Composite parent) {
		DiscoveryWizard wizard = getWizard();
		CatalogViewer viewer = new FavoritesViewer(getCatalog(), this, browser, wizard.getContainer(),
				wizard.getConfiguration());
		viewer.setMinimumHeight(MINIMUM_HEIGHT);
		viewer.createControl(parent);
		return viewer;
	}

	public void performImport() {
		setErrorMessage(null);
		IStructuredSelection selection = getViewer().getSelection();
		@SuppressWarnings("unchecked")
		List<MarketplaceNodeCatalogItem> importFavorites = selection.toList();
		if (importFavorites.isEmpty()) {
			return;
		}
		final IUserFavoritesService userFavoritesService = findUserFavoritesService();
		if (userFavoritesService == null) {
			return;
		}
		final List<INode> importNodes = new ArrayList<INode>();
		for (MarketplaceNodeCatalogItem item : importFavorites) {
			importNodes.add(item.getData());
		}
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						userFavoritesService.getStorageService().runWithLogin(new Callable<Void>() {
							public Void call() throws Exception {
								try {
									userFavoritesService.addFavorites(importNodes, monitor);
								} catch (NotAuthorizedException e) {
									setErrorMessage(Messages.ImportFavoritesPage_unauthorizedErrorMessage);
								} catch (ConflictException e) {
									setErrorMessage(
											Messages.ImportFavoritesPage_conflictErrorMessage);
								}
								return null;
							}
						});
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			MarketplaceClientCore.error(cause);
			setErrorMessage(NLS.bind(Messages.ImportFavoritesPage_unknownErrorMessage,
					cause));
		} catch (InterruptedException e) {
			//ignore
		}
	}

	private IUserFavoritesService findUserFavoritesService() {
		IUserFavoritesService userFavoritesService = null;
		for (AbstractDiscoveryStrategy strategy : getCatalog().getDiscoveryStrategies()) {
			if (strategy instanceof FavoritesDiscoveryStrategy) {
				FavoritesDiscoveryStrategy favoritesStrategy = (FavoritesDiscoveryStrategy) strategy;
				userFavoritesService = favoritesStrategy.getMarketplaceService().getUserFavoritesService();
			}
		}
		return userFavoritesService;
	}
}
