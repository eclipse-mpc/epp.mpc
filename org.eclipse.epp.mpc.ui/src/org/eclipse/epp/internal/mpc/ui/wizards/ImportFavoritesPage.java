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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.service.UserFavoritesService;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.userstorage.util.ConflictException;

public class ImportFavoritesPage extends CatalogPage {

	private final IMarketplaceWebBrowser browser;

	public ImportFavoritesPage(MarketplaceCatalog catalog, IMarketplaceWebBrowser browser) {
		super(catalog);
		this.browser = browser;
		setTitle(Messages.ImportFavoritesPage_Title);
		setDescription(Messages.ImportFavoritesPage_Description);
	}

	public void setDiscoveryError(final String error) {
		Shell shell = getShell();
		Control pageControl = getControl();
		if (shell != null && !shell.isDisposed() && pageControl != null && !pageControl.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					Shell shell = getShell();
					Control pageControl = getControl();
					if (shell != null && !shell.isDisposed() && pageControl != null && !pageControl.isDisposed()) {
						setErrorMessage(error);
					}
				}
			});
		}
	}

	public static String handleDiscoveryError(String favoritesReference, Exception ex) {
		String errorMessage = null;
		if (UserFavoritesService.isInvalidFavoritesListException(ex)) {
			boolean isUrl = (favoritesReference != null && (favoritesReference.toLowerCase().startsWith("http:") //$NON-NLS-1$
					|| favoritesReference.toLowerCase().startsWith("https:"))); //$NON-NLS-1$
			if (isUrl) {
				errorMessage = NLS.bind(Messages.ImportFavoritesActionLink_noFavoritesFoundAtUrl, favoritesReference);
			} else {
				errorMessage = NLS.bind(Messages.ImportFavoritesActionLink_noFavoritesFoundForUser, favoritesReference);
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
			MarketplaceClientCore.error(NLS.bind(Messages.ImportFavoritesActionLink_errorLoadingFavorites, message),
					ex);
		}
		return errorMessage;
	}

	@Override
	protected CatalogViewer doCreateViewer(Composite parent) {
		ImportFavoritesWizard wizard = getWizard();
		FavoritesViewer viewer = new FavoritesViewer(getCatalog(), this, browser,
				wizard.getConfiguration());
		viewer.setMinimumHeight(MINIMUM_HEIGHT);
		viewer.createControl(parent);
		String initialFavoritesUrl = wizard.getInitialFavoritesUrl();
		viewer.setFilterText(initialFavoritesUrl == null ? "" : initialFavoritesUrl); //$NON-NLS-1$
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

	@Override
	public ImportFavoritesWizard getWizard() {
		return (ImportFavoritesWizard) super.getWizard();
	}
}
