/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.service.UserFavoritesService;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.css.StyleHelper;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.userstorage.util.ConflictException;

public class ImportFavoritesPage extends CatalogPage {
	private static final String INSTALL_SELECTED_SETTING = "installSelected"; //$NON-NLS-1$

	public ImportFavoritesPage(MarketplaceCatalog catalog) {
		super(catalog);
		setTitle(Messages.ImportFavoritesPage_Title);
		setDescription(Messages.ImportFavoritesPage_Description);
	}

	public void setDiscoveryError(final String error) {
		Shell shell = getShell();
		Control pageControl = getControl();
		if (shell != null && !shell.isDisposed() && pageControl != null && !pageControl.isDisposed()) {
			shell.getDisplay().asyncExec(() -> {
				Shell shell1 = getShell();
				Control pageControl1 = getControl();
				if (shell1 != null && !shell1.isDisposed() && pageControl1 != null && !pageControl1.isDisposed()) {
					setErrorMessage(error);
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
	protected FavoritesViewer getViewer() {
		return (FavoritesViewer) super.getViewer();
	}

	@Override
	public void createControl(Composite parent) {
		new StyleHelper().on(parent).setId("ImportFavoritesPage");
		super.createControl(parent);
	}

	@Override
	protected CatalogViewer doCreateViewer(Composite parent) {
		ImportFavoritesWizard wizard = getWizard();
		FavoritesViewer viewer = new FavoritesViewer(getCatalog(), this, wizard.getConfiguration());
		viewer.setMinimumHeight(MINIMUM_HEIGHT);
		viewer.createControl(parent);
		boolean installSelected = true;
		IDialogSettings section = getDialogSettings(false);
		if (section != null) {
			installSelected = section.getBoolean(INSTALL_SELECTED_SETTING);
		}
		viewer.setInstallSelected(installSelected);

		String initialFavoritesUrl = wizard.getInitialFavoritesUrl();
		setFavoritesUrl(viewer, initialFavoritesUrl);
		return viewer;
	}

	private IDialogSettings getDialogSettings(boolean create) {
		IDialogSettings dialogSettings = MarketplaceClientUiPlugin.getInstance().getDialogSettings();
		String sectionName = this.getClass().getName();
		IDialogSettings section = dialogSettings.getSection(sectionName);
		if (create && section == null) {
			section = dialogSettings.addNewSection(sectionName);
		}
		return section;
	}

	public void setFavoritesUrl(String url) {
		FavoritesViewer viewer = getViewer();
		setFavoritesUrl(viewer, url);
	}

	private void setFavoritesUrl(FavoritesViewer viewer, String url) {
		viewer.setFavoritesUrl(url == null ? "" : url.trim()); //$NON-NLS-1$
	}

	public void performImport() {
		setErrorMessage(null);
		saveInstallSelected();
		List<MarketplaceNodeCatalogItem> importFavorites = getSelection();
		if (importFavorites.isEmpty()) {
			return;
		}
		final IUserFavoritesService userFavoritesService = findUserFavoritesService();
		if (userFavoritesService == null) {
			return;
		}
		final List<INode> importNodes = new ArrayList<>();
		for (MarketplaceNodeCatalogItem item : importFavorites) {
			importNodes.add(item.getData());
		}
		try {
			getContainer().run(true, false, monitor -> {
				try {
					userFavoritesService.getStorageService().runWithLogin(() -> {
						try {
							userFavoritesService.addFavorites(importNodes, monitor);
						} catch (NotAuthorizedException e1) {
							setErrorMessage(Messages.ImportFavoritesPage_unauthorizedErrorMessage);
						} catch (ConflictException e2) {
							setErrorMessage(Messages.ImportFavoritesPage_conflictErrorMessage);
						}
						return null;
					});
				} catch (Exception e) {
					throw new InvocationTargetException(e);
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

	public List<MarketplaceNodeCatalogItem> getSelection() {
		IStructuredSelection selection = getViewer().getSelection();
		@SuppressWarnings("unchecked")
		List<MarketplaceNodeCatalogItem> importFavorites = selection.toList();
		return new ArrayList<>(importFavorites);
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

	@Override
	public void dispose() {
		saveInstallSelected();
		super.dispose();
	}

	public boolean isInstallSelected() {
		return getViewer().isInstallSelected();
	}

	private void saveInstallSelected() {
		FavoritesViewer viewer = getViewer();
		if (viewer != null) {
			boolean installSelected = viewer.isInstallSelected();
			IDialogSettings dialogSettings = getDialogSettings(true);
			dialogSettings.put(INSTALL_SELECTED_SETTING, installSelected);
		}
	}
}
