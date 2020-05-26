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
 * 	The Eclipse Foundation - initial API and implementation
 *  Yatta Solutions - news (bug 401721), public API (bug 432803)
 *  JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import static org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.UTF_8;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.model.News;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.internal.mpc.ui.operations.AbstractProvisioningOperation;
import org.eclipse.epp.internal.mpc.ui.operations.ProfileChangeOperationComputer;
import org.eclipse.epp.internal.mpc.ui.operations.ProfileChangeOperationComputer.OperationType;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.CatalogItemEntry;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.FeatureEntry;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A wizard for interacting with a marketplace service.
 *
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceWizard extends DiscoveryWizard implements InstallProfile, IMarketplaceWebBrowser {

	private static final String WELCOME_TRIGGER_ID = "welcome"; //$NON-NLS-1$

	private static final String OPEN_FAVORITES_NOTIFICATION_PREFERENCE = "showOpenFavoritesNotification"; //$NON-NLS-1$

	private static final String IGNORED_UPDATES_PREFERENCE = "ignoredUpdates"; //$NON-NLS-1$

	private static final String PREF_DEFAULT_CATALOG = CatalogDescriptor.class.getSimpleName();

	private static final String DEBUG_NEWS_FLAG = MarketplaceClientUi.BUNDLE_ID + "/news/debug"; //$NON-NLS-1$

	private static final String DEBUG_NEWS_TITLE = MarketplaceClientUi.BUNDLE_ID + "/news/title"; //$NON-NLS-1$

	private static final String DEBUG_NEWS_URL = MarketplaceClientUi.BUNDLE_ID + "/news/url"; //$NON-NLS-1$

	public static class WizardState {
		private ContentType contentType;

		private Set<INode> content;

		private IMarket filterMarket;

		private ICategory filterCategory;

		private String filterQuery;

		private Boolean proceedWithInstallation;

		public ContentType getContentType() {
			return contentType;
		}

		public void setContentType(ContentType contentType) {
			this.contentType = contentType;
		}

		public Set<INode> getContent() {
			return content;
		}

		public void setContent(Set<INode> content) {
			this.content = content;
		}

		public IMarket getFilterMarket() {
			return filterMarket;
		}

		public void setFilterMarket(IMarket filterMarket) {
			this.filterMarket = filterMarket;
		}

		public ICategory getFilterCategory() {
			return filterCategory;
		}

		public void setFilterCategory(ICategory filterCategory) {
			this.filterCategory = filterCategory;
		}

		public String getFilterQuery() {
			return filterQuery;
		}

		public void setFilterQuery(String filterQuery) {
			this.filterQuery = filterQuery;
		}

		public Boolean getProceedWithInstallation() {
			return proceedWithInstallation;
		}

		public void setProceedWithInstallation(Boolean proceedWithInstallation) {
			this.proceedWithInstallation = proceedWithInstallation;
		}
	}

	private Set<String> installedFeatures;

	private SelectionModel selectionModel;

	private MarketplaceBrowserIntegration browserListener;

	private ProfileChangeOperation profileChangeOperation;

	private IProvisioningPlan currentJREPlan;

	private FeatureSelectionWizardPage featureSelectionWizardPage;

	private AcceptLicensesWizardPage acceptLicensesPage;

	private IInstallableUnit[] operationIUs;

	private Set<CatalogItem> operationNewInstallItems;

	private boolean initialSelectionInitialized;

	private Set<URI> addedRepositoryLocations;

	private boolean withRemediation;

	private String trigger;

	private String errorMessage;

	private WizardState initialState;

	private boolean openFavoritesBannerShown;

	public String getErrorMessage() {
		return errorMessage;
	}

	public MarketplaceWizard(MarketplaceCatalog catalog, MarketplaceCatalogConfiguration configuration) {
		super(catalog, configuration);
		setWindowTitle(Messages.MarketplaceWizard_eclipseSolutionCatalogs);
		createSelectionModel();
		withRemediation = true;
		String withRemediationOverride = System.getProperty("marketplace.remediation.enabled"); //$NON-NLS-1$
		if (withRemediationOverride != null) {
			withRemediation = Boolean.parseBoolean(withRemediationOverride);
		}
	}

	private void createSelectionModel() {
		selectionModel = new SelectionModel(this) {
			@Override
			public void selectionChanged() {
				super.selectionChanged();
				profileChangeOperation = null;
			}
		};
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
	protected MarketplacePage doCreateCatalogPage() {
		return new MarketplacePage(getCatalog(), getConfiguration());
	}

	public ProfileChangeOperation getProfileChangeOperation() {
		return profileChangeOperation;
	}

	public void resetProfileChangeOperation() {
		profileChangeOperation = null;
		currentJREPlan = null;
	}

	void initializeInitialSelection() throws CoreException {
		if (!wantInitializeInitialSelection()) {
			throw new IllegalStateException();
		}
		initialSelectionInitialized = true;
		initializeCatalog();
		if (getConfiguration().getInitialState() != null || getConfiguration().getInitialOperations() != null) {
			SelectionModelStateSerializer serializer = new SelectionModelStateSerializer(getCatalog(),
					getSelectionModel());
			try {
				getContainer().run(true, true,
						monitor -> serializer.deserialize(getConfiguration().getInitialState(),
								getConfiguration().getInitialOperations(), monitor));
			} catch (InvocationTargetException e) {
				throw new CoreException(MarketplaceClientCore.computeStatus(e, Messages.MarketplaceViewer_unexpectedException));
			} catch (InterruptedException e) {
				// user canceled
				throw new CoreException(Status.CANCEL_STATUS);
			}
			updateSelection(serializer);
		}
	}

	protected void updateSelection() {
		updateSelection(null);
	}

	private void updateSelection(SelectionModelStateSerializer serializer) {
		MarketplacePage marketplacePage = getCatalogPage();
		MarketplaceViewer viewer = marketplacePage == null ? null : marketplacePage.getViewer();
		if (marketplacePage != null && viewer != null && !viewer.getControl().isDisposed()) {
			viewer.setSelection(new StructuredSelection(viewer.getCheckedItems()));
			marketplacePage.setPageComplete(viewer.isComplete());
		}
		if (serializer != null && serializer.hasUnavailableItems()) {
			notifyNonInstallableItems(serializer.getUnavailableItems());
		}
	}

	protected void notifyNonInstallableItems(final List<? extends CatalogItem> noninstallableItems) {
		MessageDialog dialog = new MessageDialog(getShell(), Messages.MarketplaceWizard_UnableToInstallSolutions, null, Messages.MarketplaceWizard_IncompatibleSolutionsMessage, MessageDialog.ERROR,
				new String[] { IDialogConstants.OK_LABEL }, 0) {
			@Override
			protected Control createCustomArea(Composite parent) {
				parent.setLayout(new GridLayout());
				TableViewer tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
						| SWT.BORDER);
				tableViewer.setLabelProvider(createLabelProvider());
				tableViewer.setContentProvider(createContentProvider());
				tableViewer.setInput(noninstallableItems);
				Control tableControl = tableViewer.getControl();
				GridDataFactory.fillDefaults()
				.grab(true, true)
				.align(SWT.FILL, SWT.FILL)
				.hint(SWT.DEFAULT,
						Math.max(40,
								Math.min(120, tableControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)))
				.applyTo(tableControl);
				return tableControl;
			}

			private LabelProvider createLabelProvider() {
				return new LabelProvider() {
					@Override
					public String getText(Object element) {
						//TODO it would be great to know the compatible version range
						//we could show that with an IStyledLabelProvider behind the name
						return ((CatalogItem) element).getName();
					}

					@Override
					public Image getImage(Object element) {
						return MarketplaceClientUiPlugin.getInstance()
								.getImageRegistry()
								.get(MarketplaceClientUiPlugin.IU_ICON_ERROR);
					}
				};
			}

			private IStructuredContentProvider createContentProvider() {
				return new IStructuredContentProvider() {

					@Override
					public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					}

					@Override
					public void dispose() {
					}

					@Override
					public Object[] getElements(Object inputElement) {
						return ((List<?>) inputElement).toArray();
					}
				};
			}
		};
		dialog.open();
	}

	boolean wantInitializeInitialSelection() {
		if (initialSelectionInitialized) {
			return false;
		} else if (getConfiguration().getInitialState() != null || getConfiguration().getInitialOperations() != null) {
			return true;
		} else if (initialState != null) {
			WizardState initialState = getInitialState();
			if ((initialState.getContent() != null && !initialState.getContent().isEmpty())
					|| (initialState.getContentType() != null && initialState.getContentType() != ContentType.SEARCH)
					|| (initialState.getFilterCategory() != null || initialState.getFilterMarket() != null || initialState.getFilterQuery() != null)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canFinish() {
		if (computeMustCheckLicenseAcceptance()) {
			if (acceptLicensesPage == null && getContainer().getCurrentPage() == getFeatureSelectionWizardPage()) {
				getNextPage(getFeatureSelectionWizardPage(), false);
			}
			if (acceptLicensesPage == null || !acceptLicensesPage.isPageComplete()) {
				return false;
			}
		}
		if (profileChangeOperation != null) {
			IStatus resolutionResult = profileChangeOperation.getResolutionResult();
			switch (resolutionResult.getSeverity()) {
			case IStatus.OK:
			case IStatus.WARNING:
			case IStatus.INFO:
				return currentJREPlan == null || currentJREPlan.getStatus().getSeverity() != IStatus.ERROR;
			}
		}
		return false;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		return getNextPage(page, true);
	}

	IWizardPage getNextPage(IWizardPage page, boolean nextpressed) {
		boolean skipFeatureSelection = false;
		MarketplacePage catalogPage = getCatalogPage();
		if (page == catalogPage && nextpressed) {
			profileChangeOperation = null;
			featureSelectionWizardPage.updateMessage();
			if (catalogPage.canSkipFeatureSelection()) {
				skipFeatureSelection = true;
			}
		}
		if (page == featureSelectionWizardPage || (page == catalogPage && skipFeatureSelection)) {
			IWizardContainer container = getContainer();
			if (nextpressed && profileChangeOperation != null
					&& profileChangeOperation instanceof RemediationOperation) {

				try {
					container.run(true, false, monitor -> {
						((RemediationOperation) profileChangeOperation)
						.setCurrentRemedy(featureSelectionWizardPage.getRemediationGroup().getCurrentRemedy());
						profileChangeOperation.resolveModal(monitor);
					});
					if (ProvisioningUI.getDefaultUI().getPolicy().getCheckAgainstCurrentExecutionEnvironment()) {
						container.run(true, false,
								monitor -> currentJREPlan = ProvUI
								.toCompabilityWithCurrentJREProvisioningPlan(profileChangeOperation, monitor));
					}
				} catch (InterruptedException e) {
					// Nothing to report if thread was interrupted
				} catch (InvocationTargetException e) {
					ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
				}
			}
			IWizardPage nextPage = null;
			boolean operationUpdated = false;
			if (profileChangeOperation == null) {
				if (nextpressed) {
					updateProfileChangeOperation();
					operationUpdated = true;
				}
				if (profileChangeOperation == null) {
					// can't compute a change operation, so there must be some kind of error
					// we show these on the the feature selection wizard page
					nextPage = featureSelectionWizardPage;
				} else if (profileChangeOperation instanceof UninstallOperation) {
					// next button was used to resolve errors on an uninstall.
					// by returning the same page the finish button will be enabled, allowing the user to finish.
					nextPage = featureSelectionWizardPage;
				} else if (profileChangeOperation instanceof RemediationOperation) {
					nextPage = featureSelectionWizardPage;
				}
			}
			if (nextPage == null && (!profileChangeOperation.getResolutionResult().isOK()
					|| (currentJREPlan != null && !currentJREPlan.getStatus().isOK()))) {
				nextPage = featureSelectionWizardPage;
			}
			if (nextPage == null && nextpressed && profileChangeOperation instanceof RemediationOperation
					&& !featureSelectionWizardPage.isInRemediationMode()) {
				featureSelectionWizardPage.flipToRemediationComposite();
				nextPage = featureSelectionWizardPage;
			}
			if (nextPage == null && computeMustCheckLicenseAcceptance()) {
				if (acceptLicensesPage == null) {
					acceptLicensesPage = new AcceptLicensesWizardPage(
							ProvisioningUI.getDefaultUI().getLicenseManager(), operationIUs, profileChangeOperation);
					addPage(acceptLicensesPage);
				} else {
					acceptLicensesPage.update(operationIUs, profileChangeOperation);
				}
				if (nextpressed || acceptLicensesPage.hasLicensesToAccept()
						|| profileChangeOperation instanceof RemediationOperation) {
					nextPage = acceptLicensesPage;
				}
			}
			if (nextPage == null && page == catalogPage) {
				nextPage = featureSelectionWizardPage;
			}
			if (operationUpdated && nextPage == container.getCurrentPage()) {
				container.updateButtons();
			}
			return nextPage;
		}
		if (page instanceof ImportFavoritesPage) {
			return catalogPage;
		}
		return getNextPageInList(page);
	}

	protected IWizardPage getNextPageInList(IWizardPage page) {
		return super.getNextPage(page);
	}

	public boolean computeMustCheckLicenseAcceptance() {
		return profileChangeOperation != null && !(profileChangeOperation instanceof UninstallOperation);
	}

	@Override
	public void addPages() {
		doDefaultCatalogSelection();
		super.addPages();
		featureSelectionWizardPage = new FeatureSelectionWizardPage();
		addPage(featureSelectionWizardPage);
	}

	FeatureSelectionWizardPage getFeatureSelectionWizardPage() {
		return featureSelectionWizardPage;
	}


	@Override
	public IWizardPage getStartingPage() {
		if (getConfiguration().getCatalogDescriptor() != null) {
			if (wantInitializeInitialSelection()) {
				WizardState initialState = getInitialState();
				if (initialState == null || !Boolean.FALSE.equals(initialState.getProceedWithInstallation())) {
					return getFeatureSelectionWizardPage();
				}
			}
			return getCatalogPage();
		}
		return super.getStartingPage();
	}

	private void doDefaultCatalogSelection() {
		if (getConfiguration().getCatalogDescriptor() == null) {
			String defaultCatalogUrl = MarketplaceClientUiPlugin.getInstance()
					.getPreferenceStore()
					.getString(PREF_DEFAULT_CATALOG);
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
			// if no catalog is selected, pick one
			if (getConfiguration().getCatalogDescriptor() == null
					&& getConfiguration().getCatalogDescriptors().size() > 0) {
				// fall back to first catalog
				getConfiguration().setCatalogDescriptor(getConfiguration().getCatalogDescriptors().get(0));
			}
		}
	}

	@Override
	public void dispose() {
		removeAddedRepositoryLocations();
		if (getConfiguration().getCatalogDescriptor() != null) {
			// remember the catalog for next time.
			try {
				MarketplaceClientUiPlugin.getInstance()
				.getPreferenceStore()
				.setValue(PREF_DEFAULT_CATALOG,
						getConfiguration().getCatalogDescriptor().getUrl().toURI().toString());
			} catch (URISyntaxException e) {
				// ignore
			}
		}
		if (getCatalog() != null) {
			getCatalog().dispose();
		}
		super.dispose();
	}

	@Override
	public boolean performFinish() {
		if (profileChangeOperation != null
				&& profileChangeOperation.getResolutionResult().getSeverity() != IStatus.ERROR) {
			if (computeMustCheckLicenseAcceptance()) {
				if (acceptLicensesPage != null && acceptLicensesPage.isPageComplete()) {
					acceptLicensesPage.performFinish();
				}
			}
			ProvisioningJob provisioningJob = profileChangeOperation.getProvisioningJob(null);
			if (provisioningJob != null) {
				if (!operationNewInstallItems.isEmpty()) {
					provisioningJob.addJobChangeListener(new ProvisioningJobListener(operationNewInstallItems));
				}
				ProvisioningUI.getDefaultUI().schedule(provisioningJob, StatusManager.SHOW | StatusManager.LOG);
				addedRepositoryLocations = null;
				return true;
			}
		}
		return false;
	}

	@Override
	public MarketplacePage getCatalogPage() {
		return (MarketplacePage) super.getCatalogPage();
	}

	@Override
	public synchronized Set<String> getInstalledFeatures() {
		if (installedFeatures == null) {
			try {
				if (Display.getCurrent() != null) {
					getContainer().run(true, false,
							monitor -> installedFeatures = MarketplaceClientUi.computeInstalledFeatures(monitor));
				} else {
					installedFeatures = MarketplaceClientUi.computeInstalledFeatures(new NullProgressMonitor());
				}
			} catch (InvocationTargetException e) {
				MarketplaceClientUi.error(e.getCause());
				installedFeatures = Collections.emptySet();
			} catch (InterruptedException e) {
				// should never happen (not cancelable)
				throw new IllegalStateException(e);
			}
		}
		return installedFeatures;
	}

	public SelectionModel getSelectionModel() {
		return selectionModel;
	}

	@Override
	public void openUrl(String url) {
		String catalogUrl = getCatalogUrl();
		if (PlatformUI.getWorkbench().getBrowserSupport().isInternalWebBrowserAvailable()
				&& url.toLowerCase().startsWith(catalogUrl.toLowerCase())) {
			int style = IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR
					| IWorkbenchBrowserSupport.NAVIGATION_BAR;
			String browserId = "MPC-" + catalogUrl.replaceAll("[^a-zA-Z0-9_-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			try {
				CatalogDescriptor catalogDescriptor = getConfiguration().getCatalogDescriptor();
				IWebBrowser browser = PlatformUI.getWorkbench()
						.getBrowserSupport()
						.createBrowser(style, browserId,
								catalogDescriptor.getLabel(), catalogDescriptor.getDescription());
				final String originalUrl = url;
				url = appendWizardState(url);
				browser.openURL(new URL(url)); // ORDER DEPENDENCY //don't encode/validate URL - browser can be quite lenient
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setActive();
				if (!hookLocationListener(browser)) { // ORDER DEPENDENCY
					browser.openURL(new URL(originalUrl));
				}
			} catch (PartInitException e) {
				MarketplaceClientUi.handle(e.getStatus(),
						StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			} catch (MalformedURLException e) {
				IStatus status = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, NLS.bind(
						Messages.MarketplaceWizard_cannotOpenUrl, new Object[] { url, e.getMessage() }), e);
				MarketplaceClientUi.handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			}
		} else {
			WorkbenchUtil.openUrl(url, IWorkbenchBrowserSupport.AS_EXTERNAL);
		}
	}

	protected String getCatalogUrl() {
		CatalogDescriptor catalogDescriptor = getConfiguration().getCatalogDescriptor();
		URL catalogUrl = catalogDescriptor.getUrl();
		URI catalogUri;
		try {
			catalogUri = catalogUrl.toURI();
		} catch (URISyntaxException e) {
			// should never happen
			throw new IllegalStateException(e);
		}
		return catalogUri.toString();
	}

	private String appendWizardState(String url) {
		try {
			if (url.indexOf('?') == -1) {
				url += '?';
			} else {
				url += '&';
			}
			String state = new SelectionModelStateSerializer(getCatalog(), getSelectionModel()).serialize();
			url += "mpc=true&mpc_state=" + URLEncoder.encode(state, "UTF-8"); //$NON-NLS-1$//$NON-NLS-2$
			return url;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e); // should never happen
		}
	}

	private boolean hookLocationListener(IWebBrowser webBrowser) {
		try {
			Field partField = findField(webBrowser.getClass(), "part", IWorkbenchPart.class); //$NON-NLS-1$
			if (partField != null) {
				partField.setAccessible(true);
				IWorkbenchPart part = (IWorkbenchPart) partField.get(webBrowser);
				if (part != null) {
					Field browserViewerField = findField(part.getClass(), "webBrowser", null); //$NON-NLS-1$
					if (browserViewerField != null) {
						browserViewerField.setAccessible(true);
						Object browserViewer = browserViewerField.get(part);
						if (browserViewer != null) {
							Field browserField = findField(browserViewer.getClass(), "browser", Browser.class); //$NON-NLS-1$
							if (browserField != null) {
								browserField.setAccessible(true);
								Browser browser = (Browser) browserField.get(browserViewer);
								if (browser != null) {
									// only hook the listener once
									if (browser.getData(MarketplaceBrowserIntegration.class.getName()) == null) {
										if (browserListener == null) {
											browserListener = new MarketplaceBrowserIntegration();
										}
										browser.setData(MarketplaceBrowserIntegration.class.getName(), browserListener);
										// hook in listeners
										browser.addLocationListener(browserListener);
										browser.addOpenWindowListener(browserListener);
									}
									return true;
								}
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			// ignore
		}
		return false;
	}

	private Field findField(Class<?> clazz, String fieldName, Class<?> fieldClass) {
		while (clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(fieldName)
						&& (fieldClass == null || fieldClass.isAssignableFrom(field.getType()))) {
					return field;
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public void updateProfileChangeOperation() {
		removeAddedRepositoryLocations();
		addedRepositoryLocations = null;
		profileChangeOperation = null;
		currentJREPlan = null;
		operationIUs = null;
		IWizardContainer wizardContainer = getContainer();
		if (getSelectionModel().computeProvisioningOperationViable()) {
			ProfileChangeOperationComputer provisioningOperation = null;
			try {
				final Map<CatalogItem, Operation> itemToOperation = getSelectionModel().getItemToSelectedOperation();
				final Set<CatalogItem> selectedItems = getSelectionModel().getSelectedCatalogItems();
				OperationType operationType = null;
				for (Map.Entry<CatalogItem, Operation> entry : itemToOperation.entrySet()) {
					if (!selectedItems.contains(entry.getKey())) {
						continue;
					}
					OperationType entryOperationType = OperationType.map(entry.getValue());
					if (entryOperationType != null) {
						if (operationType == null || operationType == OperationType.UPDATE || entryOperationType == OperationType.CHANGE) {
							operationType = entryOperationType;
						}
					}
				}
				Map<FeatureEntry, Operation> featureEntries = getSelectionModel().getFeatureEntryToOperation(false,
						false);
				if (operationType == OperationType.CHANGE || operationType == OperationType.UPDATE) {
					Set<OperationType> featureOperations = EnumSet.noneOf(OperationType.class);
					for (Entry<FeatureEntry, Operation> entry : featureEntries.entrySet()) {
						OperationType operation = OperationType.map(entry.getValue());
						if (operation != null) {
							featureOperations.add(operation);
						}
					}
					if (featureOperations.contains(OperationType.INSTALL)
							&& featureOperations.contains(OperationType.UPDATE)) {
						//just perform install instead, which covers update
						featureOperations.remove(OperationType.UPDATE);
					}
					if (featureOperations.size() == 1) {
						operationType = featureOperations.iterator().next();
					}
				}
				URI dependenciesRepository = null;
				if (getConfiguration().getCatalogDescriptor().getDependenciesRepository() != null) {
					try {
						dependenciesRepository = getConfiguration().getCatalogDescriptor()
								.getDependenciesRepository()
								.toURI();
					} catch (URISyntaxException e) {
						throw new InvocationTargetException(e);
					}
				}
				provisioningOperation = new ProfileChangeOperationComputer(
						operationType,
						selectedItems,
						featureEntries.keySet(),
						dependenciesRepository,
						getConfiguration().getCatalogDescriptor().isInstallFromAllRepositories() ? ProfileChangeOperationComputer.ResolutionStrategy.FALLBACK_STRATEGY
								: ProfileChangeOperationComputer.ResolutionStrategy.SELECTED_REPOSITORIES,
								withRemediation);
				wizardContainer.run(true, true, provisioningOperation);

				profileChangeOperation = provisioningOperation.getOperation();
				operationIUs = provisioningOperation.getIus();
				addedRepositoryLocations = provisioningOperation.getAddedRepositoryLocations();
				operationNewInstallItems = computeNewInstallCatalogItems();
				errorMessage = provisioningOperation.getErrorMessage();

				final IStatus result = profileChangeOperation.getResolutionResult();
				if (result != null && operationIUs != null && operationIUs.length > 0
						&& operationType == OperationType.INSTALL) {
					switch (result.getSeverity()) {
					case IStatus.ERROR:
						Job job = new Job(Messages.MarketplaceWizard_errorNotificationJob) {
							IStatus r = result;

							Set<CatalogItem> items = new HashSet<>(itemToOperation.keySet());

							IInstallableUnit[] ius = operationIUs;

							String details = profileChangeOperation.getResolutionDetails();
							{
								setSystem(false);
								setUser(false);
								setPriority(LONG);
							}

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								getCatalog().installErrorReport(monitor, r, items, ius, details);
								return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
							}
						};
						job.schedule();
					}
				}
				if (ProvisioningUI.getDefaultUI().getPolicy().getCheckAgainstCurrentExecutionEnvironment()) {
					wizardContainer.run(true, false, monitor -> currentJREPlan = ProvUI
							.toCompabilityWithCurrentJREProvisioningPlan(profileChangeOperation, monitor));
				}
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				IStatus status;
				if (cause instanceof CoreException) {
					status = ((CoreException) cause).getStatus();
				} else {
					status = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, NLS.bind(
							Messages.MarketplaceWizard_problemsPerformingProvisioningOperation,
							new Object[] { cause.getMessage() }), cause);
				}
				MarketplaceClientUi.handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			} catch (InterruptedException e) {
				// canceled
				MarketplaceClientUi.log(IStatus.CANCEL, MarketplaceClientUi.BUNDLE_ID,
						Messages.MarketplaceWizard_ProvisioningOperationCancelled, e);
			} finally {
				if (provisioningOperation != null) {
					addedRepositoryLocations = provisioningOperation.getAddedRepositoryLocations();
				}
			}
		}
		//re-get the container - in case the wizard was closed in the meantime, this will be null...
		wizardContainer = getContainer();
		if (wizardContainer != null && wizardContainer.getCurrentPage() == featureSelectionWizardPage) {
			featureSelectionWizardPage.updateMessage();
		}
	}

	private void removeAddedRepositoryLocations() {
		AbstractProvisioningOperation.removeRepositoryLocations(addedRepositoryLocations);
		addedRepositoryLocations = null;
	}

	private Set<CatalogItem> computeNewInstallCatalogItems() {
		Set<CatalogItem> items = new HashSet<>();
		Map<CatalogItem, Collection<String>> iusByCatalogItem = new HashMap<>();
		for (CatalogItemEntry entry : getSelectionModel().getCatalogItemEntries()) {
			List<FeatureEntry> features = entry.getChildren();
			Collection<String> featureIds = new ArrayList<>(features.size());
			for (FeatureEntry feature : features) {
				if (feature.computeChangeOperation() == Operation.INSTALL) {
					featureIds.add(feature.getFeatureDescriptor().getId());
				}
			}
			if (!featureIds.isEmpty()) {
				iusByCatalogItem.put(entry.getItem(), featureIds);
			}
		}
		for (IInstallableUnit unit : operationIUs) {
			for (Entry<CatalogItem, Collection<String>> entry : iusByCatalogItem.entrySet()) {
				if (entry.getValue().contains(unit.getId())) {
					items.add(entry.getKey());
				}
			}
		}
		return items;
	}

	void initializeCatalog() {
		final MarketplaceCatalog catalog = getCatalog();
		synchronized (catalog) {
			List<AbstractDiscoveryStrategy> discoveryStrategies = catalog.getDiscoveryStrategies();
			for (AbstractDiscoveryStrategy strategy : discoveryStrategies) {
				strategy.dispose();
			}
			discoveryStrategies.clear();
			if (getConfiguration().getCatalogDescriptor() != null) {
				MarketplaceDiscoveryStrategy discoveryStrategy = new MarketplaceDiscoveryStrategy(
						getConfiguration().getCatalogDescriptor());
				discoveryStrategy.setShellProvider(this);
				discoveryStrategies.add(discoveryStrategy);

			}
		}
	}

	protected void updateNews() {
		CatalogDescriptor catalogDescriptor = getConfiguration().getCatalogDescriptor();
		INews news = null;
		if (Boolean.parseBoolean(Platform.getDebugOption(DEBUG_NEWS_FLAG))) {
			// use debug override values
			String debugNewsUrl = Platform.getDebugOption(DEBUG_NEWS_URL);
			if (debugNewsUrl != null && debugNewsUrl.length() > 0) {
				News debugNews = new News();
				news = debugNews;
				debugNews.setUrl(debugNewsUrl);
				String debugNewsTitle = Platform.getDebugOption(DEBUG_NEWS_TITLE);
				if (debugNewsTitle == null || debugNewsTitle.length() == 0) {
					debugNews.setShortTitle("Debug News"); //$NON-NLS-1$
				} else {
					debugNews.setShortTitle(debugNewsTitle);
				}
				debugNews.setTimestamp(System.currentTimeMillis());
			}
		}
		if (news == null) {
			// try requesting news from marketplace
			try {
				final INews[] result = new INews[1];
				getContainer().run(true, true, monitor -> {
					IStatus status = getCatalog().performNewsDiscovery(monitor);
					if (!status.isOK() && status.getSeverity() != IStatus.CANCEL) {
						// don't bother user with missing news
						MarketplaceClientUi.handle(status, StatusManager.LOG);
					}
					result[0] = getCatalog().getNews();
				});
				if (result[0] != null) {
					news = result[0];
				}
			} catch (InvocationTargetException e) {
				final IStatus status = MarketplaceClientCore.computeStatus(e, Messages.MarketplaceViewer_unexpectedException);
				MarketplaceClientUi.handle(status, StatusManager.LOG);
			} catch (InterruptedException e) {
				// cancelled by user
			}
		}
		if (news == null) {
			// use news from catalog
			news = CatalogRegistry.getInstance().getCatalogNews(catalogDescriptor);
		}
		CatalogRegistry.getInstance().addCatalogNews(catalogDescriptor, news);
	}

	protected boolean handleInstallRequest(final SolutionInstallationInfo installInfo, String url) {
		final String installId = installInfo.getInstallId();
		if (installId == null) {
			return false;
		}
		try {
			final MarketplacePage catalogPage = getCatalogPage();
			IStatus showingDescriptor = catalogPage.showMarketplace(installInfo.getCatalogDescriptor());
			if (!showingDescriptor.isOK()) {
				return true;
			}
			final SelectionModel selectionModel = getSelectionModel();
			final Map<String, Operation> nodeIdToOperation = new HashMap<>();
			nodeIdToOperation.putAll(getSelectionModel().getItemIdToSelectedOperation());
			try {
				nodeIdToOperation.put(URLDecoder.decode(installId, UTF_8), Operation.INSTALL);
			} catch (UnsupportedEncodingException e) {
				//should be unreachable
				throw new IllegalStateException();
			}

			SelectionModelStateSerializer stateSerializer = new SelectionModelStateSerializer(getCatalog(),
					selectionModel);
			getContainer().run(true, true, monitor -> {
				stateSerializer.deserialize(installInfo.getState(), nodeIdToOperation, monitor);
			});

			boolean hasAvailableItems = selectionModel.getItemToSelectedOperation().size() > 0;
			boolean hasUnavailableItems = stateSerializer.hasUnavailableItems();
			if (hasAvailableItems || hasUnavailableItems) {
				Display display = getShell().getDisplay();
				if (!display.isDisposed()) {
					display.asyncExec(() -> {
						if (getShell().isDisposed()) {
							return;
						}

						MarketplacePage catalogPage1 = getCatalogPage();
						FeatureSelectionWizardPage featurePage = getFeatureSelectionWizardPage();
						IWizardPage currentPage = getContainer().getCurrentPage();

						updateSelection(stateSerializer);
						if (catalogPage1 == currentPage && hasAvailableItems) {
							catalogPage1.show(installInfo.getCatalogDescriptor(), ContentType.SELECTION);
							IWizardPage nextPage = getNextPage(catalogPage1);
							if (nextPage != null && catalogPage1.isPageComplete()) {
								getContainer().showPage(nextPage);
							}
						} else if (hasAvailableItems) {
							IWizardPage nextPage = getNextPage(catalogPage);
							if (nextPage == featurePage && currentPage == featurePage) {
								featurePage.flipToDefaultComposite();
								featurePage.updateFeatures();
							} else {
								getContainer().showPage(nextPage);
							}
						}
					});
				}
			}
			return true;
		} catch (InvocationTargetException e) {
			IStatus status = MarketplaceClientCore.computeStatus(e, Messages.MarketplaceViewer_unexpectedException);
			MarketplaceClientUi.handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		} catch (InterruptedException e) {
			// action canceled, but this still counts as handled
			return true;
		}
		return false;
	}

	protected boolean shouldShowOpenFavoritesBanner() {
		if (openFavoritesBannerShown) {
			return false;
		}
		if (alwaysShowForTrigger()) {
			return true;
		}
		boolean show = isFirstTimeInInstallation();
		return show;
	}

	protected boolean isFirstTimeInInstallation() {
		return ConfigurationScope.INSTANCE.getNode(MarketplaceClientUi.BUNDLE_ID)
				.getBoolean(OPEN_FAVORITES_NOTIFICATION_PREFERENCE, true);
	}

	protected boolean alwaysShowForTrigger() {
		return trigger != null && WELCOME_TRIGGER_ID.equals(trigger);
	}

	protected void didShowOpenFavoritesBanner() {
		openFavoritesBannerShown = true;
		IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(MarketplaceClientUi.BUNDLE_ID);
		if (node.getBoolean(OPEN_FAVORITES_NOTIFICATION_PREFERENCE, true)) {
			node.putBoolean(OPEN_FAVORITES_NOTIFICATION_PREFERENCE, false);
			try {
				node.flush();
			} catch (BackingStoreException e) {
				MarketplaceClientUi.error(e);
			}
		}
	}

	public void importFavorites(String url) {
		MarketplaceDiscoveryStrategy marketplaceStrategy = findMarketplaceDiscoveryStrategy();
		if (marketplaceStrategy != null && marketplaceStrategy.hasUserFavoritesService()) {
			importFavorites(marketplaceStrategy, url);
		}
	}

	protected MarketplaceDiscoveryStrategy findMarketplaceDiscoveryStrategy() {
		MarketplaceDiscoveryStrategy marketplaceStrategy = null;
		List<AbstractDiscoveryStrategy> discoveryStrategies = getCatalog().getDiscoveryStrategies();
		for (AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			if (strategy instanceof MarketplaceDiscoveryStrategy) {
				marketplaceStrategy = (MarketplaceDiscoveryStrategy) strategy;
				break;
			}
		}
		return marketplaceStrategy;
	}

	protected void importFavorites(MarketplaceDiscoveryStrategy marketplaceStrategy, String url) {
		FavoritesCatalog favoritesCatalog = new FavoritesCatalog();

		ImportFavoritesWizard importFavoritesWizard = new ImportFavoritesWizard(favoritesCatalog, getConfiguration(),
				this);
		importFavoritesWizard.setInitialFavoritesUrl(url);
		final ImportFavoritesPage importFavoritesPage = importFavoritesWizard.getImportFavoritesPage();
		favoritesCatalog.getDiscoveryStrategies().add(new FavoritesDiscoveryStrategy(marketplaceStrategy) {
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
		ImportFavoritesWizardDialog importWizard = new ImportFavoritesWizardDialog(getShell(), importFavoritesWizard);

		Map<String, Operation> oldOperations = getSelectionModel().getItemIdToSelectedOperation();
		int result = importWizard.open();
		if (result == Window.OK) {
			MarketplacePage catalogPage = getCatalogPage();
			catalogPage.setActiveTab(ContentType.FAVORITES);
			catalogPage.reloadCatalog();
			Map<String, Operation> newOperations = getSelectionModel().getItemIdToSelectedOperation();
			if (!newOperations.equals(oldOperations)) {
				updateSelection();
			}
		}
	}

	public boolean shouldShowUpdateBanner(String availableUpdatesKey) {
		IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(MarketplaceClientUi.BUNDLE_ID);
		String ignoredUpdates = node.get(IGNORED_UPDATES_PREFERENCE, null);
		if (ignoredUpdates == null) {
			return true;
		}
		if (ignoredUpdates.equals(availableUpdatesKey)) {
			return false;
		}
		String[] ignoredNodes = ignoredUpdates.split(","); //$NON-NLS-1$
		String[] availableNodes = availableUpdatesKey.split(","); //$NON-NLS-1$
		Set<String> availableNodesSet = new HashSet<>(Arrays.asList(availableNodes));
		availableNodesSet.removeAll(Arrays.asList(ignoredNodes));
		return !availableNodesSet.isEmpty();
	}

	public void dismissUpdateBanner(String availableUpdatesKey) {
		IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(MarketplaceClientUi.BUNDLE_ID);
		node.put(IGNORED_UPDATES_PREFERENCE, availableUpdatesKey);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			MarketplaceClientUi.error(e);
		}
	}

	public Object suspendWizard() {
		String catalogUrl = getCatalogUrl();
		String key = appendWizardState(catalogUrl);
		getContainer().getShell().close();
		return key;
	}

	public void setInitialState(WizardState initialState) {
		this.initialState = initialState;
	}

	public WizardState getInitialState() {
		return initialState;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}

	public static void resumeWizard(Display display, Object state, boolean proceedWithInstall) {
		String catalogUrl = (String) state;
		if (proceedWithInstall) {
			org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo installInfo = MarketplaceUrlHandler.createSolutionInstallInfo(catalogUrl);
			if (installInfo != null) {
				MarketplaceUrlHandler.triggerInstall(installInfo);
				return;
			}
		}
		CatalogDescriptor descriptor = catalogUrl == null ? null : CatalogRegistry.getInstance().findCatalogDescriptor(
				catalogUrl);
		final MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		if (descriptor != null) {
			descriptor = new CatalogDescriptor(descriptor);
			descriptor.setLabel(MarketplaceUrlHandler.DESCRIPTOR_HINT);
			command.setSelectedCatalogDescriptor(descriptor);
		}
		String mpcState = MarketplaceUrlHandler.getMPCState(catalogUrl);
		if (mpcState != null && mpcState.length() > 0) {
			try {
				command.setWizardState(URLDecoder.decode(mpcState, "UTF-8")); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e); // should never happen
			}
			if (!proceedWithInstall) {
				WizardState wizardState = new WizardState();
				wizardState.setProceedWithInstallation(false);
				command.setWizardDialogState(wizardState);
			}
		}
		display.asyncExec(() -> {
			try {
				command.execute(new ExecutionEvent());
			} catch (ExecutionException e) {
				IStatus status = MarketplaceClientCore.computeStatus(e,
						Messages.MarketplaceBrowserIntegration_cannotOpenMarketplaceWizard);
				MarketplaceClientUi.handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			}
		});
	}

	IProvisioningPlan getAdditionalVerificationPlan() {
		return this.currentJREPlan;
	}
}
