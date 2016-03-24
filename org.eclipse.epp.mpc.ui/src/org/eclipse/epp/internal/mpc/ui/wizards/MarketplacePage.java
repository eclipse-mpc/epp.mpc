/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - news (bug 401721), public API (bug 432803), performance (bug 413871),
 * 	                  bug 461603: featured market
 * 	JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.model.CatalogBranding;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.ResourceProvider.ResourceReceiver;
import org.eclipse.epp.internal.mpc.ui.util.Util;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard.WizardState;
import org.eclipse.epp.mpc.core.model.ICatalogBranding;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * @author Steffen Pingel
 * @author Carsten Reckord
 */
public class MarketplacePage extends CatalogPage {

	private static final String WIDGET_ID_TAB_FAVORITES = "tab:favorites"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_SEARCH = "tab:search"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_FEATURED_MARKET = "tab:featured-market"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_RECENT = "tab:recent"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_POPULAR = "tab:popular"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_RELATED = "tab:related"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_INSTALLED = "tab:installed"; //$NON-NLS-1$

	public static final String WIDGET_ID_TAB_NEWS = "tab:news"; //$NON-NLS-1$

	public static final String WIDGET_ID_KEY = MarketplacePage.class.getName() + "::part"; //$NON-NLS-1$

	private static final String CONTENT_TYPE_KEY = ContentType.class.getName();

	static {
		registerSearchControlIcons();
	}

	/**
	 * Workaround for bug 484487
	 */
	private static void registerSearchControlIcons() {
		String clearIconKey = "org.eclipse.ui.internal.dialogs.CLEAR_ICON"; //$NON-NLS-1$
		String findIconKey = "org.eclipse.ui.internal.dialogs.FIND_ICON"; //$NON-NLS-1$
		ImageDescriptor clearDescriptor = JFaceResources.getImageRegistry().getDescriptor(clearIconKey);
		ImageDescriptor findDescriptor = JFaceResources.getImageRegistry().getDescriptor(findIconKey);
		if (clearDescriptor == null || findDescriptor == null) {
			try {
				Class.forName(
						"org.eclipse.equinox.internal.p2.ui.discovery.util.TextSearchControl", true, //$NON-NLS-1$
						MarketplacePage.class.getClassLoader());
				clearDescriptor = JFaceResources.getImageRegistry().getDescriptor(clearIconKey);
				findDescriptor = JFaceResources.getImageRegistry().getDescriptor(findIconKey);
			} catch (ClassNotFoundException e) {
				//ignore
			}
		}
		if (clearDescriptor == null) {
			clearDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
					"$nl$/icons/full/etool16/clear_co.png"); //$NON-NLS-1$
			if (clearDescriptor == null) {
				clearDescriptor = ImageDescriptor.getMissingImageDescriptor();
			}
			JFaceResources.getImageRegistry().put(clearIconKey, clearDescriptor);
		}
		if (findDescriptor == null) {
			findDescriptor = ImageDescriptor.getMissingImageDescriptor();
			JFaceResources.getImageRegistry().put(findIconKey, findDescriptor);
		}
	}

	private final MarketplaceCatalogConfiguration configuration;

	private CatalogDescriptor previousCatalogDescriptor;

	private boolean updated;

	private Link selectionLink;

	private TabFolder tabFolder;

	private TabItem searchTabItem;

	private TabItem recentTabItem;

	private TabItem popularTabItem;

	private TabItem favoritedTabItem;

	private TabItem featuredMarketTabItem;

	private TabItem relatedTabItem;

	private TabItem newsTabItem;

	private Control tabContent;

	private TabItem installedTabItem;

	private NewsViewer newsViewer;

	private CatalogSwitcher marketplaceSwitcher;

	private ICatalogBranding currentBranding = getDefaultBranding();

	protected boolean disableTabSelection;

	protected CatalogDescriptor lastSelection;

	private ContentType currentContentType;

	private ContentType previousContentType;

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
		searchTabItem = createCatalogTab(-1, ContentType.SEARCH, WIDGET_ID_TAB_SEARCH,
				currentBranding.getSearchTabName());
		recentTabItem = createCatalogTab(-1, ContentType.RECENT, WIDGET_ID_TAB_RECENT,
				currentBranding.getRecentTabName());
		popularTabItem = createCatalogTab(-1, ContentType.POPULAR, WIDGET_ID_TAB_POPULAR,
				currentBranding.getPopularTabName());
		favoritedTabItem = createCatalogTab(-1, ContentType.FAVORITES, WIDGET_ID_TAB_FAVORITES,
				getFavoritedTabName(currentBranding));
		installedTabItem = createCatalogTab(-1, ContentType.INSTALLED, WIDGET_ID_TAB_INSTALLED,
				Messages.MarketplacePage_installed);
		updateNewsTab();

		searchTabItem.setControl(tabContent);
		tabFolder.setSelection(searchTabItem);

		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (e.item.isDisposed()) {
					return;
				}
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
							&& selectionModel.computeProvisioningOperationViableForFeatureSelection()) {
						showNextPage();
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
		if (!tabContent.isDisposed()) {
			// bug 473031 - no clue how this can happen during createControl...
			MarketplaceClientUi.setDefaultHelp(tabContent);
		}
	}

	protected void showNextPage() {
		IWizardContainer container = getContainer();
		if (container == null) {
			return;
		}
		IWizardPage currentPage = container.getCurrentPage();
		if (currentPage == MarketplacePage.this && currentPage.isPageComplete()) {
			IWizardPage nextPage = getWizard().getNextPage(MarketplacePage.this);
			if (nextPage != null && nextPage instanceof WizardPage) {
				((WizardPage) nextPage).setPageComplete(true);
				container.showPage(nextPage);
			}
		}
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

	public void setPreviouslyActiveTab() {
		if (previousContentType == null) {
			setActiveTab(tabFolder.getItem(0));
		} else {
			setActiveTab(previousContentType);
		}
	}

	public void setActiveTab(ContentType contentType) {
		if (disableTabSelection) {
			return;
		}
		if (contentType != currentContentType) {
			previousContentType = currentContentType;
			currentContentType = contentType;
		}
		final TabItem tabItem = getTabItem(contentType);
		TabItem currentTabItem = getSelectedTabItem();
		if (currentTabItem != tabItem) {
			if (currentTabItem.getControl() == tabContent) {
				currentTabItem.setControl(null);
			}
			tabFolder.setSelection(tabItem);
		}
		tabItem.setControl(null);
		if (tabContent != null && !tabContent.isDisposed()) {
			tabItem.setControl(tabContent);
		}
		getViewer().setContentType(contentType);
	}

	public ContentType getActiveTab() {
		TabItem selectedTabItem = getSelectedTabItem();
		return selectedTabItem == null ? null : getContentType(selectedTabItem);
	}

	private ContentType getContentType(TabItem tabItem) {
		if (tabItem != null && !tabItem.isDisposed()) {
			Object data = tabItem.getData(CONTENT_TYPE_KEY);
			if (data instanceof ContentType) {
				return (ContentType) data;
			}
		}
		return null;
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
		case FEATURED_MARKET:
			return featuredMarketTabItem;
		case POPULAR:
			return popularTabItem;
		case RECENT:
			return recentTabItem;
		case SEARCH:
			return searchTabItem;
		case RELATED:
			return relatedTabItem;
		case SELECTION:
			return searchTabItem;
		case FAVORITES:
			return favoritedTabItem;
		default:
			throw new IllegalArgumentException();
		}
	}

	private TabItem createCatalogTab(int index, ContentType contentType, String widgetId, String label) {
		return createTab(index, contentType, widgetId, label, null);
	}

	private TabItem createTab(int index, ContentType contentType, String widgetId, String label, Control tabControl) {
		TabItem tabItem;
		if (index == -1) {
			tabItem = new TabItem(tabFolder, SWT.NULL);
		} else {
			tabItem = new TabItem(tabFolder, SWT.NULL, index);
		}
		tabItem.setData(WIDGET_ID_KEY, widgetId);
		tabItem.setData(CONTENT_TYPE_KEY, contentType);
		tabItem.setText(label);
		tabItem.setControl(tabControl);
		return tabItem;
	}

	private void createNewsTab() {
		newsTabItem = new TabItem(tabFolder, SWT.NULL | SWT.BOLD);
		newsTabItem.setText(Messages.MarketplacePage_DefaultNewsTitle);
		newsTabItem.setData(WIDGET_ID_KEY, WIDGET_ID_TAB_NEWS);

		if (newsViewer == null || newsViewer.getControl().isDisposed()) {
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
			lastSelection = selectedDescriptor;
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
		MarketplaceWizard wizard = getWizard();
		if (wizard != null) {
			if (complete) {
				complete = wizard.getSelectionModel().computeProvisioningOperationViableForFeatureSelection();
			}
			if (wizard.getContainer() != null) {
				computeMessages();
			}
		}
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
		searchTabItem = updateTab(searchTabItem, ContentType.SEARCH, WIDGET_ID_TAB_SEARCH, branding.getSearchTabName(),
				hasTab,
				oldBranding.hasSearchTab(),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = hasFeaturedMarketTab(branding);
		featuredMarketTabItem = updateTab(featuredMarketTabItem, ContentType.SEARCH, WIDGET_ID_TAB_FEATURED_MARKET,
				branding.getFeaturedMarketTabName(), hasTab, hasFeaturedMarketTab(oldBranding), tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasRecentTab();
		recentTabItem = updateTab(recentTabItem, ContentType.SEARCH, WIDGET_ID_TAB_RECENT, branding.getRecentTabName(),
				hasTab,
				oldBranding.hasRecentTab(),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasPopularTab();
		popularTabItem = updateTab(popularTabItem, ContentType.SEARCH, WIDGET_ID_TAB_POPULAR,
				branding.getPopularTabName(), hasTab,
				oldBranding.hasPopularTab(),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasRelatedTab();
		relatedTabItem = updateTab(relatedTabItem, ContentType.SEARCH, WIDGET_ID_TAB_RELATED,
				branding.getRelatedTabName(), hasTab,
				oldBranding.hasRelatedTab(), tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = hasFavoritedTab(branding);
		favoritedTabItem = updateTab(favoritedTabItem, ContentType.SEARCH, WIDGET_ID_TAB_FAVORITES,
				getFavoritedTabName(branding), hasTab, hasFavoritedTab(oldBranding),
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}

		updateNewsTab();

		if (selectedTabItem == null || selectedTabItem.isDisposed()) {
			tabFolder.setSelection(0);
		}

		final ImageDescriptor defaultWizardIconDescriptor = DiscoveryImages.BANNER_DISOVERY;
		if (branding.getWizardIcon() == null) {
			setImageDescriptor(defaultWizardIconDescriptor);
		} else {
			final Display display = Display.getCurrent();
			MarketplaceClientUiPlugin.getInstance()
			.getResourceProvider()
			.provideResource(new ResourceReceiver<ImageDescriptor>() {

				public ImageDescriptor processResource(URL resource) {
					return ImageDescriptor.createFromURL(resource);
				}

				public void setResource(final ImageDescriptor resource) {
					display.asyncExec(new Runnable() {

						public void run() {
							try {
								setImageDescriptor(resource);
							} catch (SWTException ex) {
								// broken image
								setImageDescriptor(defaultWizardIconDescriptor);
							}
						}
					});
				}
			}, branding.getWizardIcon(), defaultWizardIconDescriptor);
		}
		disableTabSelection = false;
	}

	private boolean hasFavoritedTab(ICatalogBranding branding) {
		if (branding.hasFavoritesTab()) {
			return true;
		}
		CatalogDescriptor catalogDescriptor = this.configuration.getCatalogDescriptor();
		if (catalogDescriptor == null) {
			return false;
		}
		URL url = catalogDescriptor.getUrl();
		IUserFavoritesService favoritesService = url == null ? null
				: ServiceHelper.getMarketplaceServiceLocator().getFavoritesService(url.toString());
		return favoritesService != null;
	}

	private String getFavoritedTabName(ICatalogBranding branding) {
		String favoritesTabName = branding.getFavoritesTabName();
		if (favoritesTabName == null) {
			return Messages.MarketplacePage_favorites;
		}
		return favoritesTabName;
	}

	private boolean hasFeaturedMarketTab(ICatalogBranding branding) {
		if (branding.hasFeaturedMarketTab()) {
			String marketName = branding.getFeaturedMarketTabName();
			if (marketName != null && marketName.length() > 0) {
				List<IMarket> markets = getCatalog().getMarkets();
				for (IMarket market : markets) {
					if (marketName.equals(market.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private TabItem updateTab(TabItem tabItem, ContentType contentType, String widgetId, String tabLabel,
			boolean hasTab, boolean hadTab,
			int tabIndex) {
		if (hasTab) {
			if (!hadTab) {
				tabItem = createCatalogTab(tabIndex, contentType, widgetId, tabLabel);
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
		branding.setHasRelatedTab(false);
		branding.setHasFavoritesTab(false);
		branding.setHasFeaturedMarketTab(false);
		branding.setSearchTabName(Messages.MarketplacePage_search);
		branding.setPopularTabName(Messages.MarketplacePage_popular);
		branding.setRecentTabName(Messages.MarketplacePage_recent);
		branding.setRelatedTabName(Messages.MarketplacePage_related);
		branding.setFeaturedMarketTabName(Messages.MarketplacePage_featuredMarket);
		branding.setFavoritesTabName(Messages.MarketplacePage_favorites);
		branding.setWizardTitle(Messages.MarketplacePage_eclipseMarketplaceSolutions);
		branding.setWizardIcon(null);
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
						if (lastSelection == null) {
							lastSelection = configuration.getCatalogDescriptor();
						}
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
					if (getViewer().getControl().isDisposed()) {
						return;
					}
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
	public MarketplaceCatalog getCatalog() {
		return (MarketplaceCatalog) super.getCatalog();
	}

	@Override
	public void dispose() {
		if (marketplaceSwitcher != null) {
			marketplaceSwitcher.dispose();
		}
		super.dispose();
	}
}
