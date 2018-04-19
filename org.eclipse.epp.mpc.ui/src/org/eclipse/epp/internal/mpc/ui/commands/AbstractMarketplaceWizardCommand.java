/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - category filtering (bug 314936), error handling (bug 374105),
 *                      multiselect hints (bug 337774), public API (bug 432803),
 *                      performance (bug 413871), featured market (bug 461603)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.commands;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.ResourceProvider;
import org.eclipse.epp.internal.mpc.ui.wizards.AbstractMarketplaceWizardDialog;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceCatalogConfiguration;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public abstract class AbstractMarketplaceWizardCommand extends AbstractHandler implements IHandler {

	private List<CatalogDescriptor> catalogDescriptors;

	private CatalogDescriptor selectedCatalogDescriptor;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final MarketplaceCatalog catalog = createCatalog();
		if (catalog == null)
		{
			return null;//errors have already been logged, just return
		}
		MarketplaceCatalogConfiguration configuration = createConfiguration(catalog, event);
		if (configuration == null)
		{
			return null;//errors have already been logged, just return
		}
		DiscoveryWizard wizard = createWizard(catalog, configuration, event);
		openWizardDialog(wizard, event);

		return null;
	}

	protected MarketplaceCatalog createCatalog() {
		final MarketplaceCatalog catalog = new MarketplaceCatalog();

		catalog.setEnvironment(DiscoveryCore.createEnvironment());
		catalog.setVerifyUpdateSiteAvailability(false);
		return catalog;
	}

	protected MarketplaceCatalogConfiguration createConfiguration(final MarketplaceCatalog catalog,
			ExecutionEvent event) {
		MarketplaceCatalogConfiguration configuration = new MarketplaceCatalogConfiguration();
		configuration.setVerifyUpdateSiteAvailability(false);

		if (catalogDescriptors == null || catalogDescriptors.isEmpty()) {
			final IStatus remoteCatalogStatus = installRemoteCatalogs();
			configuration.getCatalogDescriptors().addAll(CatalogRegistry.getInstance().getCatalogDescriptors());
			if (configuration.getCatalogDescriptors().isEmpty()) {
				// doesn't make much sense to continue without catalogs.
				// nothing will work and no way to recover later
				IStatus cause;
				if (!remoteCatalogStatus.isOK()) {
					cause = remoteCatalogStatus;
				} else {
					cause = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
							Messages.MarketplaceWizardCommand_noRemoteCatalogs);
				}
				IStatus exitStatus = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, cause.getCode(),
						Messages.MarketplaceWizardCommand_cannotOpenMarketplace, new CoreException(cause));
				try {
					MarketplaceClientUi.handle(exitStatus,
							StatusManager.SHOW | StatusManager.BLOCK
							| (exitStatus.getSeverity() == IStatus.CANCEL ? 0 : StatusManager.LOG));
				} catch (Exception ex) {
					// HOTFIX for bug 477269 - Display might get disposed during call to handle due to workspace shutdown or similar.
					// In that case, just log...
					MarketplaceClientUi.getLog().log(exitStatus);
				}
				return null;
			} else if (!remoteCatalogStatus.isOK()) {
				MarketplaceClientUi.handle(remoteCatalogStatus, StatusManager.LOG);
			}
		} else {
			configuration.getCatalogDescriptors().addAll(catalogDescriptors);
		}
		if (selectedCatalogDescriptor != null) {
			if (selectedCatalogDescriptor.getLabel().equals("org.eclipse.epp.mpc.descriptorHint")) { //$NON-NLS-1$
				CatalogDescriptor resolvedDescriptor = CatalogRegistry.getInstance().findCatalogDescriptor(
						selectedCatalogDescriptor.getUrl().toExternalForm());
				if (resolvedDescriptor == null) {
					IStatus status = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
							Messages.MarketplaceWizardCommand_CouldNotFindMarketplaceForSolution, new ExecutionException(
									selectedCatalogDescriptor.getUrl().toExternalForm()));
					MarketplaceClientUi.handle(status,
							StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
					return null;
				} else {
					configuration.setCatalogDescriptor(resolvedDescriptor);
				}
			} else {
				configuration.setCatalogDescriptor(selectedCatalogDescriptor);
			}
		}

		return configuration;
	}

	protected void openWizardDialog(DiscoveryWizard wizard, ExecutionEvent event) {
		WizardDialog dialog = createWizardDialog(wizard, event);
		dialog.open();
	}

	protected abstract AbstractMarketplaceWizardDialog createWizardDialog(DiscoveryWizard wizard, ExecutionEvent event);

	protected abstract DiscoveryWizard createWizard(final MarketplaceCatalog catalog,
			MarketplaceCatalogConfiguration configuration, ExecutionEvent event);

	public void setCatalogDescriptors(List<CatalogDescriptor> catalogDescriptors) {
		this.catalogDescriptors = catalogDescriptors;
	}

	public void setSelectedCatalogDescriptor(CatalogDescriptor selectedCatalogDescriptor) {
		this.selectedCatalogDescriptor = selectedCatalogDescriptor;
	}

	public IStatus installRemoteCatalogs() {
		try {
			final AtomicReference<List<? extends ICatalog>> result = new AtomicReference<List<? extends ICatalog>>();

			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					ICatalogService catalogService = ServiceHelper.getMarketplaceServiceLocator().getCatalogService();
					final List<? extends ICatalog> catalogs = catalogService.listCatalogs(monitor);
					result.set(catalogs);
				} catch (CoreException e) {
					if (e.getStatus().getSeverity() == IStatus.CANCEL) {
						throw new InterruptedException();
					}
					throw new InvocationTargetException(e);
				}
			});

			List<? extends ICatalog> catalogs = result.get();
			for (ICatalog catalog : catalogs) {
				ResourceProvider resourceProvider = MarketplaceClientUiPlugin.getInstance().getResourceProvider();
				String catalogName = catalog.getName();
				String requestSource = NLS.bind(Messages.MarketplaceWizardCommand_requestCatalog, catalogName,
						catalog.getId());
				String catalogImageUrl = catalog.getImageUrl();
				if (catalogImageUrl != null) {
					try {
						resourceProvider.retrieveResource(requestSource, catalogImageUrl);
					} catch (Exception e) {
						MarketplaceClientUi.log(IStatus.WARNING,
								Messages.MarketplaceWizardCommand_FailedRetrievingCatalogImage, catalogName,
								catalogImageUrl, e);
					}
				}
				if (catalog.getBranding() != null && catalog.getBranding().getWizardIcon() != null) {
					String wizardIconUrl = catalog.getBranding().getWizardIcon();
					try {
						resourceProvider.retrieveResource(requestSource, wizardIconUrl);
					} catch (Exception e) {
						MarketplaceClientUi.log(IStatus.WARNING,
								Messages.MarketplaceWizardCommand_FailedRetrievingCatalogWizardIcon, catalogName,
								wizardIconUrl, e);
					}
				}
				CatalogDescriptor descriptor = new CatalogDescriptor(catalog);
				registerOrOverrideCatalog(descriptor);
			}
		} catch (InterruptedException ie) {
			if (ie.getMessage() == null || "".equals(ie.getMessage())) {
				InterruptedException ie1 = new InterruptedException("Operation cancelled");
				ie1.setStackTrace(ie.getStackTrace());
				if (ie.getCause() != null) {
					ie1.initCause(ie.getCause());
				}
				ie = ie1;
			}
			IStatus errorStatus = MarketplaceClientCore.computeStatus(ie,
					Messages.MarketplaceWizardCommand_CannotInstallRemoteLocations);
			return new Status(IStatus.CANCEL, MarketplaceClientCore.BUNDLE_ID, errorStatus.getMessage(), ie);
		} catch (Exception e) {
			IStatus status = MarketplaceClientCore.computeStatus(e, Messages.MarketplaceWizardCommand_CannotInstallRemoteLocations);
			return status;
		}
		return Status.OK_STATUS;
	}

	private void registerOrOverrideCatalog(CatalogDescriptor descriptor) {
		CatalogRegistry catalogRegistry = CatalogRegistry.getInstance();
		List<CatalogDescriptor> descriptors = catalogRegistry.getCatalogDescriptors();
		for (CatalogDescriptor catalogDescriptor : descriptors) {
			if (catalogDescriptor.getUrl().toExternalForm().equals(descriptor.getUrl().toExternalForm())) {
				catalogRegistry.unregister(catalogDescriptor);
			}
		}
		catalogRegistry.register(descriptor);
	}

	public void setConfiguration(IMarketplaceClientConfiguration configuration) {
		setCatalogDescriptors(configuration.getCatalogDescriptors());
		setSelectedCatalogDescriptor(configuration.getCatalogDescriptor());
	}
}
