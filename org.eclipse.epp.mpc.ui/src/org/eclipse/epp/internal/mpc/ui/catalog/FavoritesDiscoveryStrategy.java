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
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory.Contents;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem.UserAction;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class FavoritesDiscoveryStrategy extends MarketplaceDiscoveryStrategy {

	private String favoritesReference;

	public FavoritesDiscoveryStrategy(CatalogDescriptor catalogDescriptor) {
		super(catalogDescriptor);
	}

	public FavoritesDiscoveryStrategy(MarketplaceDiscoveryStrategy marketplaceDiscoveryStrategy) {
		super(marketplaceDiscoveryStrategy.catalogDescriptor);
	}

	public void setFavoritesReference(String favoritesReference) {
		this.favoritesReference = favoritesReference;
	}

	public String getFavoritesReference() {
		return favoritesReference;
	}

	@Override
	public void maybeAddCatalogItem(MarketplaceCategory catalogCategory) {
		//do nothing
	}

	@Override
	protected Map<String, IInstallableUnit> computeInstalledIUs(IProgressMonitor monitor) {
		return Collections.emptyMap();
	}

	@Override
	protected void handleDiscoveryCategory(MarketplaceCategory catalogCategory) {
		catalogCategory.setContents(Contents.USER_FAVORITES);
	}

	@Override
	protected ISearchResult doPerformDiscovery(IProgressMonitor monitor) throws CoreException {
		preDiscovery();
		if (favoritesReference == null) {
			return null;
		}
		try {
			URI uri = URLUtil.toURI(favoritesReference);
			return marketplaceService.userFavorites(uri, monitor);
		} catch (CoreException ex) {
			//if we don't want an error dialog to pop up for discovery errors, we have
			//to handle errors here...
			handleDiscoveryError(ex);
			return null;
		} catch (URISyntaxException e) {
			IStatus error = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
					NLS.bind(Messages.FavoritesDiscoveryStrategy_invalidUrl, favoritesReference, e), e);
			handleDiscoveryError(new CoreException(error));
			return null;
		} finally {
			postDiscovery();
		}
	}

	protected void postDiscovery() {
		// ignore
	}

	protected void preDiscovery() {
		// ignore
	}

	protected void handleDiscoveryError(CoreException ex) throws CoreException {
		//by default just rethrow - but this gives subclasses the chance to override
		throw ex;
	}

	@Override
	protected void handleSearchResult(MarketplaceCategory catalogCategory, ISearchResult result,
			IProgressMonitor monitor) {
		if (result == null) {
			addInstructionInfoItem(catalogCategory);
			addFavoriteListEntries(catalogCategory, monitor);
		} else if (result.getNodes().isEmpty()) {
			addEmptyInfoItem(catalogCategory);
			addFavoriteListEntries(catalogCategory, monitor);
		} else {
			super.handleSearchResult(catalogCategory, result, monitor);
			for (CatalogItem catalogItem : items) {
				if (catalogItem instanceof MarketplaceNodeCatalogItem) {
					catalogItem.setSelected(true);
				}
			}
		}
	}

	private void addFavoriteListEntries(MarketplaceCategory catalogCategory, IProgressMonitor monitor) {
		try {
			List<IFavoriteList> userFavoriteLists = marketplaceService.userFavoriteLists(monitor);
			addFavoriteListEntries(catalogCategory, userFavoriteLists);
		} catch (CoreException ex) {
			//if we don't want an error dialog to pop up for discovery errors, we have
			//to handle errors here...
			try {
				handleDiscoveryError(ex);
			} catch (CoreException ex1) {
				MarketplaceClientUi.error(ex1);
			}
		}
	}

	private void addFavoriteListEntries(MarketplaceCategory catalogCategory, List<IFavoriteList> userFavoriteLists) {
		if (userFavoriteLists == null || userFavoriteLists.isEmpty()) {
			return;
		}
		catalogCategory.setContents(Contents.FAVORITE_LISTS);
		catalogCategory.setName(Messages.FavoritesDiscoveryStrategy_favoritesCategoryTitle);
		MarketplaceCatalogSource source = this.getCatalogSource();
		int maxCount = Math.min(userFavoriteLists.size(), 5);
		for (int i = 0; i < maxCount; i++) {
			IFavoriteList favoriteList = userFavoriteLists.get(i);
			FavoriteListCatalogItem item = new FavoriteListCatalogItem();
			item.setFavoriteList(favoriteList);
			item.setId(favoriteList.getId());
			item.setName(favoriteList.getName());
			item.setProvider(favoriteList.getOwner());

			String iconUrl = favoriteList.getIcon();
			if (iconUrl != null) {
				if (!source.getResourceProvider().containsResource(iconUrl)) {
					cacheResource(source.getResourceProvider(), item, iconUrl);
				}
				createIcon(item, favoriteList);
			}
			addItem(catalogCategory, item);
		}
	}

	private static void createIcon(CatalogItem catalogItem, final IFavoriteList list) {
		Icon icon = new Icon();
		// don't know the size
		icon.setImage32(list.getIcon());
		icon.setImage48(list.getIcon());
		icon.setImage64(list.getIcon());
		icon.setImage128(list.getIcon());
		catalogItem.setIcon(icon);
	}

	private void addEmptyInfoItem(CatalogCategory catalogCategory) {
		addInfoItem(catalogCategory, Messages.FavoritesDiscoveryStrategy_noFavoritesTitle,
				Messages.FavoritesDiscoveryStrategy_noFavoritesMessage);
	}

	private void addInstructionInfoItem(CatalogCategory catalogCategory) {
		addInfoItem(catalogCategory, Messages.FavoritesDiscoveryStrategy_enterFavoritesUrlTitle,
				Messages.FavoritesDiscoveryStrategy_enterFavoritesUrlMessage);
	}

	private void addInfoItem(CatalogCategory catalogCategory, String title, String description) {
		MarketplaceCatalogSource source = getCatalogSource();
		String id = catalogDescriptor.getUrl().toString() + "#info:" + title; //$NON-NLS-1$
		for (ListIterator<CatalogItem> i = items.listIterator(items.size()); i.hasPrevious();) {
			CatalogItem item = i.previous();
			if (item.getSource() == source
					&& (item.getCategory() == catalogCategory || catalogCategory.getId().equals(item.getCategoryId()))
					&& item instanceof UserActionCatalogItem) {
				UserActionCatalogItem actionItem = (UserActionCatalogItem) item;
				if (actionItem.getUserAction() == UserAction.INFO && id.equals(actionItem.getId())) {
					return;
				}
			}
		}
		UserActionCatalogItem infoItem = new UserActionCatalogItem();
		infoItem.setUserAction(UserAction.INFO);
		infoItem.setDescription(description);
		infoItem.setName(title);
		infoItem.setId(id);
		addItem(catalogCategory, infoItem);
	}

	private void addItem(CatalogCategory catalogCategory, CatalogItem item) {
		MarketplaceCatalogSource catalogSource = getCatalogSource();
		item.setSource(catalogSource);
		item.setCategoryId(catalogCategory.getId());
		items.add(item);
	}
}
