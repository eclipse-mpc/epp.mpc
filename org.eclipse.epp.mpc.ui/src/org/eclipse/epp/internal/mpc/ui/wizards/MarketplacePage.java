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
 * 	Yatta Solutions - news (bug 401721), public API (bug 432803), performance (bug 413871),
 * 	                  bug 461603: featured market
 * 	JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.model.CatalogBranding;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiResources;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeInstallableUnitItem;
import org.eclipse.epp.internal.mpc.ui.catalog.ResourceProvider.ResourceReceiver;
import org.eclipse.epp.internal.mpc.ui.css.StyleHelper;
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
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

/**
 * @author Steffen Pingel
 * @author Carsten Reckord
 */
public class MarketplacePage extends CatalogPage implements IWizardButtonLabelProvider {

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

	private final MarketplaceCatalogConfiguration configuration;

	private CatalogDescriptor previousCatalogDescriptor;

	private boolean updated;

	private Link contentListLinks;

	private ActionLink selectionLink;

	private ActionLink deselectLink;

	private CTabFolder tabFolder;

	private CTabItem searchTabItem;

	private CTabItem recentTabItem;

	private CTabItem popularTabItem;

	private CTabItem favoritedTabItem;

	private CTabItem featuredMarketTabItem;

	private CTabItem relatedTabItem;

	private CTabItem newsTabItem;

	private Control tabContent;

	private CTabItem installedTabItem;

	private NewsViewer newsViewer;

	private CatalogSwitcher marketplaceSwitcher;

	private ICatalogBranding currentBranding = getDefaultBranding();

	protected boolean disableTabSelection;

	protected CatalogDescriptor lastSelection;

	private ContentType currentContentType;

	private ContentType previousContentType;

	private final List<ActionLink> actionLinks = new ArrayList<>();

	private final Map<String, ActionLink> actions = new HashMap<>();

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

		StyleHelper styleHelper = new StyleHelper();

		Composite pageContent = new Composite(parent, SWT.NULL);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 5).applyTo(pageContent);
		styleHelper.on(pageContent).setId("MarketplacePage");

		tabFolder = new CTabFolder(pageContent, SWT.TOP | SWT.BORDER | SWT.FLAT);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tabFolder);
		setTabFolderAlwaysHighlight(tabFolder);

		super.createControl(tabFolder);

		tabContent = getControl();
		styleHelper.on(tabContent).setId("MarketplaceContent");
		final Color selectionBackground = tabFolder.getSelectionBackground();
		tabContent.setBackground(selectionBackground);
		tabContent.addPaintListener(e -> {
			Control control = (Control) e.widget;
			Color currentSelectionBackground = tabFolder.getSelectionBackground();
			Color currentBackground = control.getBackground();
			if (currentBackground != currentSelectionBackground
					&& !currentBackground.equals(currentSelectionBackground)) {
				control.setBackground(currentSelectionBackground);
			}
		});

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

		int defaultTabHeight = tabFolder.getTabHeight();
		tabFolder.setTabHeight(defaultTabHeight + 4);

		tabFolder.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.item.isDisposed()) {
					return;
				}
				setActiveTab((CTabItem) e.item);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		{
			contentListLinks = new Link(pageContent, SWT.NULL);//TODO id
			contentListLinks.setToolTipText(Messages.MarketplacePage_showSelection);
			contentListLinks.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String actionId = e.text;
					ActionLink actionLink = actions.get(actionId);
					if (actionLink != null) {
						actionLink.selected();
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(contentListLinks);
			updateSelectionLink();
		}

		if (needSwitchMarketplaceControl) {
			createMarketplaceSwitcher(pageContent);
		}
		updateBranding();

		// bug 312411: a selection listener so that we can streamline install of single product
		getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			private int previousSelectionSize = 0;

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!isCurrentPage()) {
					return;
				}
				SelectionModel selectionModel = getWizard().getSelectionModel();
				int newSelectionSize = selectionModel.getItemToSelectedOperation().size();

				// important: we don't do anything if the selection is empty, since CatalogViewer
				// sets the empty selection whenever the catalog is updated.
				if (!event.getSelection().isEmpty()) {

					if (previousSelectionSize == 0 && newSelectionSize > 0
							&& selectionModel.computeProvisioningOperationViableForFeatureSelection()
							&& getWizard().canProceedInstallation()) {
						showNextPage();
					}
				}
				previousSelectionSize = newSelectionSize;
			}
		});
		getViewer().addPropertyChangeListener(event -> {
			if (event.getProperty().equals(MarketplaceViewer.CONTENT_TYPE_PROPERTY) && event.getNewValue() != null) {
				setActiveTab((ContentType) event.getNewValue());
			}
		});
		setControl(pageContent);
		if (!tabContent.isDisposed()) {
			// bug 473031 - no clue how this can happen during createControl...
			MarketplaceClientUi.setDefaultHelp(tabContent);
		}
		styleHelper.on(pageContent).applyStyles();
	}

	private static void setTabFolderAlwaysHighlight(final CTabFolder tabFolder) {
		final Field highlightField;
		try {
			highlightField = CTabFolder.class.getDeclaredField("highlight");
			highlightField.setAccessible(true);
		} catch (Exception ex) {
			// ignore - we just won't be able to keep highlight
			return;
		}
		Listener listener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				try {
					Object wasHighlight = highlightField.get(tabFolder);
					highlightField.set(tabFolder, true);
					if (!Boolean.TRUE.equals(wasHighlight)) {
						tabFolder.redraw();
					}
				} catch (Exception ex) {
					// ignore and disable listener to avoid future errors
					tabFolder.removeListener(SWT.Deactivate, this);
				}
			}
		};
		tabFolder.addListener(SWT.Deactivate, listener);
	}

	protected void showNextPage() {
		IWizardContainer container = getContainer();
		if (container == null) {
			return;
		}
		IWizardPage currentPage = container.getCurrentPage();
		if (currentPage == MarketplacePage.this && currentPage.isPageComplete()) {
			IWizardPage nextPage = getNextPage();
			if (nextPage != null && nextPage instanceof FeatureSelectionWizardPage) {
				//FIXME do we need this at all? Pages should be responsible for their completion
				//state themselves...
				((FeatureSelectionWizardPage) nextPage).setPageComplete(true);
			}
			if (nextPage != null && nextPage.getControl() != null && nextPage.getControl().isDisposed()) {
				// page already disposed?
				return;
			}
			container.showPage(nextPage);
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		if (isPageComplete()) {
			MarketplaceWizard wizard = getWizard();
			if (wizard != null) {
				return wizard.getNextPageInList(this) != null;
			}
		}
		return false;
	}

	protected boolean canSkipFeatureSelection() {
		SelectionModel selectionModel = getWizard().getSelectionModel();
		Map<CatalogItem, Operation> selectedOperations = selectionModel.getItemToSelectedOperation();
		Set<Entry<CatalogItem, Operation>> entrySet = selectedOperations.entrySet();
		Operation mode = null;
		for (Entry<CatalogItem, Operation> entry : entrySet) {
			if (!(entry.getKey() instanceof MarketplaceNodeCatalogItem)) {
				return false;
			}
			MarketplaceNodeCatalogItem item = (MarketplaceNodeCatalogItem) entry.getKey();
			Operation value = entry.getValue();
			switch (value) {
			case NONE:
				continue;
			case INSTALL:
			case UPDATE:
				if (mode == null) {
					mode = Operation.INSTALL;
				} else if (mode == Operation.UNINSTALL) {
					return false;
				}
				if (hasOptionalFeatures(item)) {
					return false;
				}
				break;
			case UNINSTALL:
				if (mode == null) {
					mode = Operation.UNINSTALL;
				} else if (mode == Operation.INSTALL) {
					return false;
				}
				if (hasOptionalFeatures(item)) {
					return false;
				}
				break;
			case CHANGE:
				return false;
			}
		}
		if (mode == null) {
			return false;
		}
		return true;
	}

	private boolean hasOptionalFeatures(MarketplaceNodeCatalogItem item) {
		List<MarketplaceNodeInstallableUnitItem> installableUnitItems = item.getInstallableUnitItems();
		if (installableUnitItems.size() > 1) {
			for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
				if (iuItem.isOptional()) {
					return true;
				}
			}
		}
		return false;
	}

	private void setActiveTab(CTabItem tab) {
		if (disableTabSelection) {
			return;
		}
		if (tab == newsTabItem) {
			final INews news = getNews();
			if (news == null) {
				setActiveTab(currentContentType != null ? currentContentType
						: previousContentType != null ? previousContentType : ContentType.SEARCH);
				updateNewsTab();
				return;
			}
			boolean wasUpdated = newsViewer.isUpdated(news);
			newsViewer.showNews(news);
			if (wasUpdated) {
				updateBranding();
				CTabItem currentTabItem = getSelectedTabItem();
				if (currentTabItem != newsTabItem) {
					tabFolder.setSelection(newsTabItem);
					// required for Mac to not switch back to first tab
					getControl().getDisplay().asyncExec(() -> tabFolder.setSelection(newsTabItem));
				}
			}
			return;
		}
		ContentType currentContentType = getViewer().getContentType();
		if (currentContentType != null) {
			CTabItem tabItem = getTabItem(currentContentType);
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
		throw new IllegalArgumentException(tab.getText());
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
		boolean showSelected = false;
		if (contentType == ContentType.SELECTION) {
			showSelected = true;
			contentType = ContentType.SEARCH;
		}

		if (contentType != currentContentType) {
			previousContentType = currentContentType;
			currentContentType = contentType;
		}
		final CTabItem tabItem = getTabItem(contentType);
		CTabItem currentTabItem = getSelectedTabItem();
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
		if (previousContentType != contentType) {
			contentTypeChanged(previousContentType, contentType);
		}
		if (showSelected) {
			getViewer().showSelected();
		} else {
			getViewer().setContentType(contentType);
		}
	}

	private void contentTypeChanged(ContentType previousContentType, ContentType contentType) {
		actionLinks.clear();
		actions.clear();
		if (selectionLink != null) {
			doAddActionLink(0, selectionLink);
		}
		if (contentType == ContentType.FAVORITES) {
			doAddActionLink(-1, new ImportFavoritesActionLink(this));
			doAddActionLink(-1, new InstallAllActionLink(this));
		}
		updateContentListLinks();
	}

	public ContentType getActiveTab() {
		CTabItem selectedTabItem = getSelectedTabItem();
		return selectedTabItem == null ? null : getContentType(selectedTabItem);
	}

	private ContentType getContentType(CTabItem tabItem) {
		if (tabItem != null && !tabItem.isDisposed()) {
			Object data = tabItem.getData(CONTENT_TYPE_KEY);
			if (data instanceof ContentType) {
				return (ContentType) data;
			}
		}
		return null;
	}

	private CTabItem getSelectedTabItem() {
		int currentTabIndex = tabFolder.getSelectionIndex();
		CTabItem currentTabItem = null;
		if (currentTabIndex != -1) {
			currentTabItem = tabFolder.getItem(currentTabIndex);
		}
		return currentTabItem;
	}

	private CTabItem getTabItem(ContentType content) {
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

	private CTabItem createCatalogTab(int index, ContentType contentType, String widgetId, String label) {
		return createTab(index, contentType, widgetId, label, null);
	}

	private CTabItem createTab(int index, ContentType contentType, String widgetId, String label, Control tabControl) {
		CTabItem tabItem;
		if (index == -1) {
			tabItem = new CTabItem(tabFolder, SWT.NULL);
		} else {
			tabItem = new CTabItem(tabFolder, SWT.NULL, index);
		}
		tabItem.setData(WIDGET_ID_KEY, widgetId);
		tabItem.setData(CONTENT_TYPE_KEY, contentType);
		tabItem.setText(label);
		tabItem.setControl(tabControl);
		return tabItem;
	}

	private void createNewsTab() {
		newsTabItem = new CTabItem(tabFolder, SWT.NULL | SWT.BOLD);
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
			String title = news.getShortTitle().replace("&", "&&"); //$NON-NLS-1$//$NON-NLS-2$
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
			tabImage = MarketplaceClientUiResources.getInstance().getImageRegistry()
					.get(MarketplaceClientUiResources.NEWS_ICON_UPDATE);
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
		composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
		composite.setLayout(new FillLayout());
		composite.setData("CSS_SUPPORTS_BORDERS", true);
		StyleHelper styleHelper = new StyleHelper();
		styleHelper.on(composite).setId("switcher-parent");

		final CatalogSwitcher switcher = new CatalogSwitcher(composite,
				MarketplaceClientUi.useNativeBorders() ? SWT.BORDER : SWT.None,
						configuration);
		switcher.addSelectionChangedListener(event -> {
			CatalogDescriptor descriptor = (CatalogDescriptor) ((IStructuredSelection) event.getSelection())
					.getFirstElement();
			showMarketplace(descriptor);
		});
		CatalogDescriptor selectedDescriptor = configuration.getCatalogDescriptor();
		if (selectedDescriptor != null) {
			switcher.setSelection(new StructuredSelection(selectedDescriptor));
			lastSelection = selectedDescriptor;
		}
		styleHelper.on(switcher).setId("MarketplaceSwitcher");

		marketplaceSwitcher = switcher;
		GridDataFactory.fillDefaults()
		.align(SWT.FILL, SWT.FILL)
		.grab(true, false)
		.minSize(1, SWT.DEFAULT)
		.hint(500, SWT.DEFAULT)
		.applyTo(composite);
	}

	private void updateContentListLinks() {
		if (contentListLinks != null) {
			final String originalText = contentListLinks.getText();

			StringBuilder bldr = new StringBuilder();
			boolean first = true;
			for (ActionLink actionLink : actionLinks) {
				if (first) {
					first = false;
				} else {
					bldr.append(" | "); //$NON-NLS-1$
				}
				bldr.append(NLS.bind("<a href=\"{0}\">{1}</a>", actionLink.getId(), actionLink.getLabel())); //$NON-NLS-1$
			}
			String text = bldr.toString();
			if (!text.equals(originalText)) {
				contentListLinks.setText(text);
				contentListLinks.getParent().layout(true, false);
			}
		}
	}

	private void updateSelectionLink() {
		if (contentListLinks != null) {
			final String originalText = selectionLink == null ? "" : selectionLink.getLabel(); //$NON-NLS-1$

			String text = ""; //$NON-NLS-1$
			int count = getWizard().getSelectionModel().getItemToSelectedOperation().size();
			if (count == 1) {
				text = Messages.MarketplacePage_linkShowSelection_One;
			} else if (count > 0) {
				text = NLS.bind(Messages.MarketplacePage_linkShowSelection_Multiple, Integer.valueOf(count));
			}
			if (!text.equals(originalText)) {
				if (text.equals("")) { //$NON-NLS-1$
					if (selectionLink != null) {
						removeActionLink(selectionLink);
						selectionLink = null;
						removeActionLink(deselectLink);
						deselectLink = null;
					}
				} else {
					ActionLink newSelectionLink = createSelectionLink(text);
					if (selectionLink != null) {
						updateActionLink(selectionLink, newSelectionLink);
					} else {
						addActionLink(0, newSelectionLink);
						deselectLink = createDeselectionLink();
						addActionLink(1, deselectLink);
					}
					selectionLink = newSelectionLink;
				}
			}
		}
	}

	private ActionLink createDeselectionLink() {
		return new ActionLink("clearSelection", Messages.MarketplacePage_DeselectAll, Messages.MarketplacePage_DeselectAllTooltip) { //$NON-NLS-1$

			@Override
			public void selected() {
				deselectionLinkActivated();
			}
		};
	}

	private ActionLink createSelectionLink(String text) {
		return new ActionLink("showSelection", text, Messages.MarketplacePage_showSelection) { //$NON-NLS-1$

			@Override
			public void selected() {
				selectionLinkActivated();
			}
		};
	}

	protected void selectionLinkActivated() {
//		tabFolder.setSelection(searchTabItem);
//		getViewer().showSelected();
		setActiveTab(ContentType.SELECTION);
	}

	protected void deselectionLinkActivated() {
		SelectionModel selectionModel = getWizard().getSelectionModel();
		selectionModel.clear();
		getWizard().updateSelection();
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
		Composite viewerPanel = (Composite) viewer.getControl();
		viewerPanel.setBackgroundMode(SWT.INHERIT_DEFAULT);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2).applyTo(viewerPanel);
		return viewer;
	}

	@Override
	protected void doUpdateCatalog() {
		if (!updated) {
			updated = true;
			Display.getCurrent().asyncExec(() -> {
				if (!getControl().isDisposed() && isCurrentPage()) {
					safeUpdateCatalog();
				}
			});
		}
	}

	private void safeUpdateCatalog() {
		try {
			getWizard().updateNews();
			if (getControl().isDisposed()) {
				return;
			}
			getViewer().updateCatalog();
			if (getControl().isDisposed()) {
				return;
			}
			updateBranding();
		} catch (SWTException ex) {
			if (ex.code == SWT.ERROR_WIDGET_DISPOSED) {
				//ignore - this happens if the wizard is closed during the update
				return;
			}
			throw ex;
		} catch (IllegalArgumentException ex) {
			if (getControl().isDisposed()) {
				//ignore - this happens if the wizard is closed during the update
				return;
			}
			throw ex;
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
		updateSelectionLink();
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

		CTabItem selectedTabItem = getSelectedTabItem();

		int tabIndex = 0;
		boolean hasTab = branding.hasSearchTab();
		searchTabItem = updateTab(searchTabItem, ContentType.SEARCH, WIDGET_ID_TAB_SEARCH, branding.getSearchTabName(),
				hasTab,
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = hasFeaturedMarketTab(branding);
		featuredMarketTabItem = updateTab(featuredMarketTabItem, ContentType.FEATURED_MARKET,
				WIDGET_ID_TAB_FEATURED_MARKET,
				branding.getFeaturedMarketTabName(), hasTab, tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasRecentTab();
		recentTabItem = updateTab(recentTabItem, ContentType.RECENT, WIDGET_ID_TAB_RECENT, branding.getRecentTabName(),
				hasTab,
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasPopularTab();
		popularTabItem = updateTab(popularTabItem, ContentType.POPULAR, WIDGET_ID_TAB_POPULAR,
				branding.getPopularTabName(), hasTab,
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = branding.hasRelatedTab();
		relatedTabItem = updateTab(relatedTabItem, ContentType.RELATED, WIDGET_ID_TAB_RELATED,
				branding.getRelatedTabName(), hasTab,
				tabIndex);
		if (hasTab) {
			tabIndex++;
		}
		hasTab = hasFavoritedTab(branding);
		favoritedTabItem = updateTab(favoritedTabItem, ContentType.FAVORITES, WIDGET_ID_TAB_FAVORITES,
				getFavoritedTabName(branding), hasTab, tabIndex);
		if (hasTab) {
			tabIndex++;
		}

		updateNewsTab();

		if (selectedTabItem == null || selectedTabItem.isDisposed()) {
			tabFolder.setSelection(0);
		}
		tabFolder.setFocus();

		final ImageDescriptor defaultWizardIconDescriptor = DiscoveryImages.BANNER_DISOVERY;
		if (branding.getWizardIcon() == null) {
			setImageDescriptor(defaultWizardIconDescriptor);
		} else {
			final Display display = Display.getCurrent();
			MarketplaceClientUiResources.getInstance()
			.getResourceProvider()
			.provideResource(new ResourceReceiver<ImageDescriptor>() {

				@Override
				public ImageDescriptor processResource(URL resource) {
					return ImageDescriptor.createFromURL(resource);
				}

				@Override
				public void setResource(final ImageDescriptor resource) {
					display.asyncExec(() -> {
						try {
							setImageDescriptor(resource);
						} catch (SWTException ex) {
							// broken image
							setImageDescriptor(defaultWizardIconDescriptor);
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
		return hasFavoritesService();
	}

	private boolean hasFavoritesService() {
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

	private CTabItem updateTab(CTabItem tabItem, ContentType contentType, String widgetId, String tabLabel,
			boolean hasTab, int tabIndex) {
		if (hasTab) {
			if (tabItem == null || tabItem.isDisposed() || getContentType(tabItem) != contentType) {
				tabItem = createCatalogTab(tabIndex, contentType, widgetId, tabLabel);
			} else {
				tabItem.setText(tabLabel);
			}
		} else if (tabItem != null && !tabItem.isDisposed()) {
			tabItem.dispose();
			tabItem = null;
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
					updateSelectionLink();
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
			getContainer().run(false, true, monitor -> {
				if (getViewer().getControl().isDisposed()) {
					return;
				}
				getWizard().initializeCatalog();
				safeUpdateCatalog();
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

	protected ActionLink getActionLink(String actionId) {
		return actions.get(actionId);
	}

	protected void addActionLink(int pos, ActionLink actionLink) {
		doAddActionLink(pos, actionLink);
		updateContentListLinks();
	}

	private void doAddActionLink(int pos, ActionLink actionLink) {
		ActionLink existingAction = actions.get(actionLink.getId());
		if (existingAction != null && existingAction != actionLink) {
			throw new IllegalArgumentException();
		}
		int insertIndex = pos == -1 || pos > actionLinks.size() ? actionLinks.size() : pos;
		actionLinks.add(insertIndex, actionLink);
		actions.put(actionLink.getId(), actionLink);
	}

	protected void removeActionLink(ActionLink actionLink) {
		doRemoveActionLink(actionLink);
		updateContentListLinks();
	}

	private void doRemoveActionLink(ActionLink actionLink) {
		actionLinks.remove(actionLink);
		if (actions.get(actionLink.getId()) == actionLink) {
			actions.remove(actionLink.getId());
		}
	}

	protected void updateActionLink(ActionLink oldLink, ActionLink newLink) {
		if (oldLink == newLink) {
			return;
		}
		doUpdateActionLink(oldLink, newLink);
		updateContentListLinks();
	}

	private void doUpdateActionLink(ActionLink oldLink, ActionLink newLink) {
		int index = actionLinks.indexOf(oldLink);
		if (index != -1) {
			String id = oldLink.getId();
			if (actions.get(id) != oldLink) {
				throw new IllegalArgumentException();
			}
			if (!id.equals(newLink.getId())) {
				throw new IllegalArgumentException();
			}
			actionLinks.set(index, newLink);
			actions.put(id, newLink);
		} else {
			throw new NoSuchElementException();
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

	public void reloadCatalog() {
		getViewer().reload();
	}

	@Override
	public String getNextButtonLabel() {
		return Messages.MarketplaceWizardDialog_Install_Now;
	}

	@Override
	public String getBackButtonLabel() {
		return null;
	}
}
