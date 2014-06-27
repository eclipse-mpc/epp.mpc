/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *  Yatta Solutions - news (bug 401721), public API (bug 432803)
 *  JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.service.CatalogBranding;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.util.Util;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard.WizardState;
import org.eclipse.epp.mpc.core.model.ICatalogBranding;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * @author Steffen Pingel
 * @author Carsten Reckord
 */
public class MarketplacePage extends CatalogPage {

	public static final String WIDGET_ID_TAB_SEARCH = "tab:search"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_RECENT = "tab:recent"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_POPULAR = "tab:popular"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_INSTALLED = "tab:installed"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_NEWS = "tab:news"; //$NON-NLS-1$

	public static final String WIDGET_ID_KEY = MarketplacePage.class.getName() + "::part"; //$NON-NLS-1$

	private final MarketplaceCatalogConfiguration configuration;

	private CatalogDescriptor previousCatalogDescriptor;

	private boolean updated;

	private Link selectionLink;

	private TabFolder tabFolder;

	private TabItem searchTabItem;

	private TabItem recentTabItem;

	private TabItem popularTabItem;

	private TabItem newsTabItem;

	private Control tabContent;

	private TabItem installedTabItem;

	private NewsViewer newsViewer;

	private CatalogSwitcher marketplaceSwitcher;

	private ICatalogBranding currentBranding = getDefaultBranding();

	protected boolean disableTabSelection;

	protected CatalogDescriptor lastSelection;

	public MarketplacePage(MarketplaceCatalog catalog, MarketplaceCatalogConfiguration configuration) {
		super(catalog);
		this.configuration = configuration;
		setDescription(Messages.MarketplacePage_selectSolutionsToInstall);
		setTitle(Messages.MarketplacePage_eclipseMarketplaceSolutions);
		updateTitle();
	}

	private void updateTitle() {
		if (configuration.getCatalogDescriptor() != null) {
			setTitle(configuration.getCatalogDescriptor().getLabel());
		}
	}

	@Override
	public void createControl(final Composite parent) {
		currentBranding = getDefaultBranding();
		boolean needSwitchMarketplaceControl = configuration.getCatalogDescriptors().size() > 1;

		Composite pageContent = new Composite(parent, SWT.NULL);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 5).applyTo(pageContent);

		tabFolder = new TabFolder(pageContent, SWT.TOP);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tabFolder);

		super.createControl(tabFolder);

		tabContent = getControl();
		searchTabItem = createCatalogTab(-1, WIDGET_ID_TAB_SEARCH, currentBranding.getSearchTabName());
		recentTabItem = createCatalogTab(-1, WIDGET_ID_TAB_RECENT, currentBranding.getRecentTabName());
		popularTabItem = createCatalogTab(-1, WIDGET_ID_TAB_POPULAR, currentBranding.getPopularTabName());
		installedTabItem = createCatalogTab(-1, WIDGET_ID_TAB_INSTALLED, Messages.MarketplacePage_installed);
		updateNewsTab();
		tabFolder.setSelection(searchTabItem);

		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setActiveTab((TabItem) e.item);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		{
			selectionLink = new Link(pageContent, SWT.NULL);//TODO id
			selectionLink.setToolTipText(Messages.MarketplacePage_showSelection);
			selectionLink.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					selectionLinkActivated();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);

				}
			});
			GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(selectionLink);
			computeSelectionLinkText();
		}

		if (needSwitchMarketplaceControl) {
			createMarketplaceSwitcher(pageContent);
		}
		updateBranding();

		// bug 312411: a selection listener so that we can streamline install of single product
		getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			private int previousSelectionSize = 0;

			public void selectionChanged(SelectionChangedEvent event) {
				if (!isCurrentPage()) {
					return;
				}
				SelectionModel selectionModel = getWizard().getSelectionModel();
				int newSelectionSize = selectionModel.getItemToSelectedOperation().size();

				// important: we don't do anything if the selection is empty, since CatalogViewer
				// sets the empty selection whenever the catalog is updated.
				if (!event.getSelection().isEmpty()) {

					if (previousSelectionSize == 0 && newSelectionSize == 1
							&& selectionModel.computeProvisioningOperationViable()) {
						IWizardPage currentPage = getContainer().getCurrentPage();
						if (currentPage.isPageComplete()) {
							IWizardPage nextPage = getWizard().getNextPage(MarketplacePage.this);
							if (nextPage != null && nextPage instanceof WizardPage) {
								((WizardPage) nextPage).setPageComplete(true);
								getContainer().showPage(nextPage);
							}
						}
					}
				}
				previousSelectionSize = newSelectionSize;
			}
		});
		getViewer().addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(MarketplaceViewer.CONTENT_TYPE_PROPERTY) && event.getNewValue() != null) {
					setActiveTab((ContentType) event.getNewValue());
				}
			}
		});
		setControl(pageContent);
		MarketplaceClientUi.setDefaultHelp(tabContent);
	}

	private void setActiveTab(TabItem tab) {
		if (disableTabSelection) {
			return;
		}
		if (tab == newsTabItem) {
			final INews news = getNews();
			boolean wasUpdated = newsViewer.isUpdated(news);
			newsViewer.showNews(news);
			if (wasUpdated) {
				updateBranding();
				TabItem currentTabItem = getSelectedTabItem();
				if (currentTabItem != newsTabItem) {
					tabFolder.setSelection(newsTabItem);
					// required for Mac to not switch back to first tab
					getControl().getDisplay().asyncExec(new Runnable() {
						public void run() {
							tabFolder.setSelection(newsTabItem);
						}
					});
				}
			}
			return;
		}
		ContentType currentContentType = getViewer().getContentType();
		if (currentContentType != null) {
			TabItem tabItem = getTabItem(currentContentType);
			if (tabItem == tab) {
				setActiveTab(currentContentType);
				return;
			}
		}
		for (ContentType contentType : ContentType.values()) {
			if (getTabItem(contentType) == tab) {
				setActiveTab(contentType);
				return;
			}
		}
		throw new IllegalArgumentException();
	}

	private void setActiveTab(ContentType contentType) {
		if (disableTabSelection) {
			return;
		}
		final TabItem tabItem = getTabItem(contentType);
		TabItem currentTabItem = getSelectedTabItem();
		if (currentTabItem != tabItem) {
			tabFolder.setSelection(tabItem);
		}
		getViewer().setContentType(contentType);
	}

	private TabItem getSelectedTabItem() {
		int currentTabIndex = tabFolder.getSelectionIndex();
		TabItem currentTabItem = null;
		if (currentTabIndex != -1) {
			currentTabItem = tabFolder.getItem(currentTabIndex);
		}
		return currentTabItem;
	}

	private TabItem getTabItem(ContentType content) {
		switch (content) {
		case INSTALLED:
			return installedTabItem;
		case POPULAR:
			return popularTabItem;
		case RECENT:
			return recentTabItem;
		case SEARCH:
			return searchTabItem;
		case SELECTION:
			return searchTabItem;
		default:
			throw new IllegalArgumentException();
		}
	}

	private TabItem createCatalogTab(int index, String widgetId, String label) {
		Control tabControl = tabContent;
		return createTab(index, widgetId, label, tabControl);
	}

	private TabItem createTab(int index, String widgetId, String label, Control tabControl) {
		TabItem tabItem;
		if (index == -1) {
			tabItem = new TabItem(tabFolder, SWT.NULL);
		} else {
			tabItem = new TabItem(tabFolder, SWT.NULL, index);
		}
		tabItem.setData(WIDGET_ID_KEY, widgetId);
		tabItem.setText(label);
		tabItem.setControl(tabControl);
		return tabItem;
	}

	private void createNewsTab() {
		newsTabItem = new TabItem(tabFolder, SWT.NULL | SWT.BOLD);
		newsTabItem.setText(Messages.MarketplacePage_DefaultNewsTitle);
		newsTabItem.setData(WIDGET_ID_KEY, WIDGET_ID_TAB_NEWS);

		if (newsViewer == null) {
			createNewsViewer(tabFolder);
		}
		newsTabItem.setControl(newsViewer.getControl());
	}

	private void updateNewsTab() {
		if (newsTabItem == null) {
			createNewsTab();
		}
		INews news = getNews();
		if (news == null) {
			if (!newsTabItem.isDisposed()) {
				newsTabItem.dispose();
			}
			return;
		} else if (newsTabItem.isDisposed()) {
			createNewsTab();
		}
		if (news.getShortTitle() != null && news.getShortTitle().length() > 0) {
			String title = news.getShortTitle();
			String tooltip = title;
			if (title.length() > 40) {
				tooltip = title;
				title = title.substring(0, 39) + '\u2026';
			}
			newsTabItem.setText(title);
			newsTabItem.setToolTipText(tooltip);
		}

		updateNewsStatus();
	}

	private void updateNewsStatus() {
		INews news = getNews();

		Image tabImage = null;
		if (news != null && newsViewer.isUpdated(news)) {
			tabImage = MarketplaceClientUiPlugin.getInstance()
					.getImageRegistry()
					.get(MarketplaceClientUiPlugin.NEWS_ICON_UPDATE);
		}
		newsTabItem.setImage(tabImage);
		newsTabItem.getParent().layout();
	}

	private INews getNews() {
		CatalogDescriptor descriptor = configuration.getCatalogDescriptor();
		INews news = CatalogRegistry.getInstance().getCatalogNews(descriptor);
		return news;
	}

	private void createNewsViewer(Composite parent) {
		newsViewer = new NewsViewer(getWizard());
		newsViewer.createControl(parent);
	}

	private void createMarketplaceSwitcher(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		final CatalogSwitcher switcher = new CatalogSwitcher(composite, SWT.BORDER, configuration);
		switcher.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				CatalogDescriptor descriptor = (CatalogDescriptor) ((IStructuredSelection) event.getSelection()).getFirstElement();
				showMarketplace(descriptor);
			}
		});
		CatalogDescriptor selectedDescriptor = configuration.getCatalogDescriptor();
		if (selectedDescriptor != null) {
			switcher.setSelection(new StructuredSelection(selectedDescriptor));
		}
		marketplaceSwitcher = switcher;
		GridDataFactory.fillDefaults()
		.align(SWT.FILL, SWT.FILL)
		.grab(true, false)
		.minSize(1, SWT.DEFAULT)
		.hint(500, SWT.DEFAULT)
		.applyTo(composite);
	}

	private void computeSelectionLinkText() {
		if (selectionLink != null) {
			final String originalText = selectionLink.getText();

			String text = ""; //$NON-NLS-1$
			int count = getWizard().getSelectionModel().getItemToSelectedOperation().size();
			if (count == 1) {
				text = Messages.MarketplacePage_linkShowSelection_One;
			} else if (count > 0) {
				text = NLS.bind(Messages.MarketplacePage_linkShowSelection_Multiple, Integer.valueOf(count));
			}
			if (!text.equals(originalText)) {
				selectionLink.setText(text);
				selectionLink.getParent().layout(true, false);
			}
		}
	}

	protected void selectionLinkActivated() {
		tabFolder.setSelection(searchTabItem);
		getViewer().showSelected();
	}

	@Override
	public IWizardPage getPreviousPage() {
		return super.getPreviousPage();
	}

	@Override
	public MarketplaceWizard getWizard() {
		return (MarketplaceWizard) super.getWizard();
	}

	@Override
	protected MarketplaceViewer getViewer() {
		return (MarketplaceViewer) super.getViewer();
	}

	@Override
	protected CatalogViewer doCreateViewer(Composite parent) {
		MarketplaceViewer viewer = new MarketplaceViewer(getCatalog(), this, getWizard());
		viewer.setMinimumHeight(MINIMUM_HEIGHT);
		viewer.createControl(parent);
		return viewer;
	}

	@Override
	protected void doUpdateCatalog() {
		if (!updated) {
			updated = true;
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {
					if (!getControl().isDisposed() && isCurrentPage()) {
						getWizard().updateNews();
						getViewer().updateCatalog();
						updateBranding();
					}
				}
			});
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			CatalogDescriptor catalogDescriptor = configuration.getCatalogDescriptor();
			if (catalogDescriptor != null) {
				setTitle(catalogDescriptor.getLabel());
			}
			if (previousCatalogDescriptor == null || !previousCatalogDescriptor.equals(catalogDescriptor)) {
				previousCatalogDescriptor = catalogDescriptor;
				tabFolder.setSelection(searchTabItem);
				getViewer().setContentType(ContentType.SEARCH);
				getWizard().initializeCatalog();
				updated = false;
			}
		}
		super.setVisible(visible);
	}

	@Override
	public void setPageComplete(boolean complete) {
		if (complete) {
			complete = getWizard().getSelectionModel().computeProvisioningOperationViable();
		}
		computeMessages();
		super.setPageComplete(complete);
	}

	private void computeMessages() {
		computeStatusMessage();
		computeSelectionLinkText();
	}

	private void computeStatusMessage() {
		String message = null;
		int messageType = IMessageProvider.NONE;

		if (getWizard() != null) {
			IStatus viability = getWizard().getSelectionModel().computeProvisioningOperationViability();
			if (viability != null) {
				message = viability.getMessage();
				messageType = Util.computeMessageType(viability);
			}
		}

		setMessage(message, messageType);
	}

	@Override
	public void performHelp() {
		getControl().notifyListeners(SWT.Help, new Event());
	}

	private void updateBranding() {
		disableTabSelection = true;
		updateTitle();
		CatalogDescriptor descriptor = configuration.getCatalogDescriptor();
		ICatalogBranding oldBranding = currentBranding;
		ICatalogBranding branding = descriptor == null ? null : descriptor.getCatalogBranding();
		if (branding == null) {
			branding = getDefaultBranding();
		}
		currentBranding = branding;

		TabItem selectedTabItem = getSelectedTabItem();

		int tabIndex = 0;
		boolean hasTab = branding.hasSearchTab();
		searchTabItem = updateTab(searchTabItem, WIDGET_ID_TAB_SEARCH, branding.getSearchTabName(), hasTab,
				oldBranding.hasSearchTab(),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasRecentTab();
		recentTabItem = updateTab(recentTabItem, WIDGET_ID_TAB_RECENT, branding.getRecentTabName(), hasTab,
				oldBranding.hasRecentTab(),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasPopularTab();
		popularTabItem = updateTab(popularTabItem, WIDGET_ID_TAB_POPULAR, branding.getPopularTabName(), hasTab,
				oldBranding.hasPopularTab(),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}

		updateNewsTab();

		if (selectedTabItem == null || selectedTabItem.isDisposed()) {
			tabFolder.setSelection(0);
		}

		try {
			ImageDescriptor wizardIconDescriptor;
			if (branding.getWizardIcon() == null) {
				wizardIconDescriptor = DiscoveryImages.BANNER_DISOVERY;
			} else {
				wizardIconDescriptor = ImageDescriptor.createFromURL(URLUtil.toURL(branding.getWizardIcon()));
			}
			setImageDescriptor(wizardIconDescriptor);
		} catch (MalformedURLException e) {
			MarketplaceClientUi.error(e);
		}
		disableTabSelection = false;
	}

	private TabItem updateTab(TabItem tabItem, String widgetId, String tabLabel, boolean hasTab, boolean hadTab,
			int tabIndex) {
		if (hasTab) {
			if (!hadTab) {
				tabItem = createCatalogTab(tabIndex, widgetId, tabLabel);
			} else {
				tabItem.setText(tabLabel);
			}
		} else if (tabItem != null && !tabItem.isDisposed()) {
			tabItem.dispose();
		}
		return tabItem;
	}

	private ICatalogBranding getDefaultBranding() {
		CatalogBranding branding = new CatalogBranding();
		branding.setHasSearchTab(true);
		branding.setHasPopularTab(true);
		branding.setHasRecentTab(true);
		branding.setSearchTabName(Messages.MarketplacePage_search);
		branding.setPopularTabName(Messages.MarketplacePage_popular);
		branding.setRecentTabName(Messages.MarketplacePage_recent);
		branding.setWizardTitle(Messages.MarketplacePage_eclipseMarketplaceSolutions);
		return branding;
	}

	public IStatus showMarketplace(CatalogDescriptor catalogDescriptor) {
		if (configuration.getCatalogDescriptor() != catalogDescriptor) {
			if (getWizard().getSelectionModel().getSelectedCatalogItems().size() > 0) {
				boolean discardSelection = MessageDialog.openConfirm(getShell(),
						Messages.MarketplacePage_selectionSolutions, Messages.MarketplacePage_discardPendingSolutions);
				if (discardSelection) {
					getWizard().getSelectionModel().clear();
					computeSelectionLinkText();
				} else {
					if (marketplaceSwitcher != null) {
						marketplaceSwitcher.setSelection(new StructuredSelection(lastSelection));
					}
					return Status.CANCEL_STATUS;
				}
			}
			lastSelection = catalogDescriptor;
			configuration.setCatalogDescriptor(catalogDescriptor);
			if (marketplaceSwitcher != null) {
				marketplaceSwitcher.setSelection(new StructuredSelection(catalogDescriptor));
			}
			updateCatalog();
		}
		return Status.OK_STATUS;
	}

	private void updateCatalog() {
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					getWizard().initializeCatalog();
					getWizard().updateNews();
					getViewer().updateCatalog();
					updateBranding();
				}
			});
		} catch (InvocationTargetException e) {
			MarketplaceClientUi.error(e.getCause());
		} catch (InterruptedException e) {
			//cancelled, ignore this
		}
	}

	public void showNews(CatalogDescriptor catalogDescriptor) {
		IStatus proceed = Status.OK_STATUS;
		if (catalogDescriptor != null) {
			proceed = showMarketplace(catalogDescriptor);
		}
		if (proceed.isOK()) {
			setActiveTab(newsTabItem);
		}
	}

	public void show(CatalogDescriptor catalogDescriptor, MarketplaceViewer.ContentType content) {
		IStatus proceed = Status.OK_STATUS;
		if (catalogDescriptor != null) {
			proceed = showMarketplace(catalogDescriptor);
		}
		if (proceed.isOK()) {
			setActiveTab(content);
		}
	}

	public void show(CatalogDescriptor catalogDescriptor, final Set<? extends INode> nodes) {
		IStatus proceed = Status.OK_STATUS;
		if (catalogDescriptor != null) {
			proceed = showMarketplace(catalogDescriptor);
		}
		if (proceed.isOK()) {
			setActiveTab(searchTabItem);
			getViewer().show(nodes);
		}
	}

	public void search(CatalogDescriptor catalogDescriptor, final IMarket searchMarket, final ICategory searchCategory,
			final String searchString) {
		IStatus proceed = Status.OK_STATUS;
		if (catalogDescriptor != null) {
			proceed = showMarketplace(catalogDescriptor);
		}
		if (proceed.isOK()) {
			setActiveTab(searchTabItem);
			getViewer().search(searchMarket, searchCategory, searchString);
		}
	}

	protected void initialize(WizardState initialState) {
		ContentType contentType = initialState.getContentType();
		if (contentType != null && contentType != ContentType.SEARCH) {
			show(configuration.getCatalogDescriptor(), contentType);
		} else if (initialState.getContent() != null && !initialState.getContent().isEmpty()) {
			show(configuration.getCatalogDescriptor(), initialState.getContent());
		} else {
			IMarket market = initialState.getFilterMarket();
			ICategory category = initialState.getFilterCategory();
			String query = initialState.getFilterQuery();
			if (market != null || category != null || query != null) {
				search(configuration.getCatalogDescriptor(), market, category, query);
			}
		}
	}

	@Override
	public void dispose() {
		if (marketplaceSwitcher != null) {
			marketplaceSwitcher.dispose();
		}
		super.dispose();
	}
}
