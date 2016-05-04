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
import java.util.List;
import java.util.Set;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FavoritesViewer extends CatalogViewer {

	private static final String HTTPS_PREFIX = "https://"; //$NON-NLS-1$

	private static final String HTTP_PREFIX = "http://"; //$NON-NLS-1$

	private static final int MIN_URL_LENGTH = HTTP_PREFIX.length() + 5;

	private MarketplaceDiscoveryResources discoveryResources;

	private final IMarketplaceWebBrowser browser;

	public FavoritesViewer(Catalog catalog, IShellProvider shellProvider, IMarketplaceWebBrowser browser,
			IRunnableContext context, CatalogConfiguration configuration) {
		super(catalog, shellProvider, context, configuration);
		this.browser = browser;
		setAutomaticFind(true);
		setRefreshJobDelay(2500);
	}

	@Override
	protected StructuredViewer doCreateViewer(Composite container) {
		StructuredViewer viewer = super.doCreateViewer(container);
		discoveryResources = new MarketplaceDiscoveryResources(container.getDisplay());
		viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				discoveryResources.dispose();
				discoveryResources = null;
			}
		});
		super.getResources().dispose();
		viewer.setSorter(null);
		return viewer;
	}

	@Override
	protected void doCreateHeaderControls(Composite parent) {
		//header will have two items: the label and the search field
		Control[] children = parent.getChildren();
		Label searchLabel = null;
		Text searchField = null;
		for (Control control : children) {
			if (searchLabel == null && control instanceof Label) {
				searchLabel = (Label) control;
			} else if (searchField == null && control instanceof Text) {
				searchField = (Text) control;
			}
		}
		if (searchLabel != null) {
			searchLabel.setText(Messages.FavoritesViewer_searchLabel);
		}
		if (searchField != null) {
			searchField.setMessage(Messages.FavoritesViewer_searchInputDescription);
		}
	}

	@Override
	protected void modifySelection(CatalogItem connector, boolean selected) {
		super.modifySelection(connector, selected);
	}

	@Override
	public void updateCatalog() {
		List<CatalogItem> checkedItems = getCheckedItems();
		for (CatalogItem catalogItem : checkedItems) {
			modifySelection(catalogItem, false);
		}
		super.updateCatalog();
	}

	@Override
	protected MarketplaceDiscoveryResources getResources() {
		return discoveryResources;
	}

	@Override
	protected ControlListItem<?> doCreateViewerItem(Composite parent, Object element) {
		if (element instanceof MarketplaceNodeCatalogItem) {
			//marketplace entry
			FavoritesDiscoveryItem discoveryItem = createDiscoveryItem(parent, (MarketplaceNodeCatalogItem) element);
			return discoveryItem;
		} else if (element instanceof UserActionCatalogItem) {
			return new InfoViewerItem(parent, getResources(), shellProvider, (UserActionCatalogItem) element, this);
		}
		return super.doCreateViewerItem(parent, element);
	}

	private FavoritesDiscoveryItem createDiscoveryItem(Composite parent, MarketplaceNodeCatalogItem catalogItem) {
		return new FavoritesDiscoveryItem(parent, SWT.NONE, getResources(), browser, catalogItem, this);
	}

	@Override
	protected Set<String> getInstalledFeatures(org.eclipse.core.runtime.IProgressMonitor monitor) {
		return Collections.emptySet();
	}

	@Override
	protected void filterTextChanged() {
		String filterText = getFilterText();
		if (filterText == null) {
			return;
		}
		filterText = filterText.trim();
		if ("".equals(filterText)) { //$NON-NLS-1$
			return;
		}
		if (isUrlReference(filterText)) {
			if (filterText.length() < MIN_URL_LENGTH || filterText.lastIndexOf('/') < MIN_URL_LENGTH) {
				//probably incomplete url
				filterTextChangedWithExtraDelay();
				return;
			}
			try {
				URLUtil.toURL(filterText);
			} catch (Exception ex) {
				//invalid url
				filterTextChangedWithExtraDelay();
				return;
			}
		}
		if (filterText.length() < HTTPS_PREFIX.length()) {
			String lowerCase = filterText.toLowerCase();
			if (HTTP_PREFIX.startsWith(lowerCase) || HTTPS_PREFIX.startsWith(lowerCase)) {
				//probably incomplete url
				filterTextChangedWithExtraDelay();
				return;
			}
		}
		super.filterTextChanged();
	}

	private synchronized void filterTextChangedWithExtraDelay() {
		long refreshJobDelay = getRefreshJobDelay();
		setRefreshJobDelay(1500);
		try {
			super.filterTextChanged();
		} finally {
			setRefreshJobDelay(refreshJobDelay);
		}
	}

	@Override
	protected void doFind(final String text) {
		List<AbstractDiscoveryStrategy> discoveryStrategies = getCatalog().getDiscoveryStrategies();
		for (AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			if (strategy instanceof FavoritesDiscoveryStrategy) {
				FavoritesDiscoveryStrategy favoritesStrategy = (FavoritesDiscoveryStrategy) strategy;
				favoritesStrategy.setFavoritesReference(text);

			}
		}
		updateCatalog();
	}

	@Override
	protected void catalogUpdated(boolean wasCancelled, boolean wasError) {
		super.catalogUpdated(wasCancelled, wasError);
		List<CatalogItem> items = getCatalog().getItems();
		for (CatalogItem catalogItem : items) {
			modifySelection(catalogItem, catalogItem.isSelected());
		}
	}

	public static boolean isUrlReference(String text) {
		if (text == null || "".equals(text)) { //$NON-NLS-1$
			return false;
		}
		text = text.trim().toLowerCase();
		return (text.startsWith(HTTP_PREFIX) || text.startsWith(HTTPS_PREFIX));
	}
}
