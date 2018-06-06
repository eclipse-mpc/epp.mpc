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
package org.eclipse.epp.internal.mpc.ui.catalog;

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.epp.internal.mpc.ui.catalog.messages"; //$NON-NLS-1$

	public static String FavoriteListCatalogItem_defaultListName;

	public static String FavoritesDiscoveryStrategy_enterFavoritesUrlMessage;

	public static String FavoritesDiscoveryStrategy_enterFavoritesUrlTitle;

	public static String FavoritesDiscoveryStrategy_favoritesCategoryTitle;

	public static String FavoritesDiscoveryStrategy_invalidUrl;

	public static String FavoritesDiscoveryStrategy_noFavoritesMessage;

	public static String FavoritesDiscoveryStrategy_noFavoritesTitle;

	public static String MarketplaceCatalog_addedNullEntry;

	public static String MarketplaceCatalog_Checking_News;

	public static String MarketplaceCatalog_checkingForUpdates;

	public static String MarketplaceCatalog_Discovery_Error;

	public static String MarketplaceCatalog_ErrorReadingRepository;

	public static String MarketplaceCatalog_failedWithError;

	public static String MarketplaceCatalog_InvalidRepositoryUrl;

	public static String MarketplaceCatalog_queryFailed;

	public static String MarketplaceCatalog_queryingMarketplace;

	public static String MarketplaceDiscoveryStrategy_badUri;

	public static String MarketplaceDiscoveryStrategy_catalogCategory;

	public static String MarketplaceDiscoveryStrategy_ComputingInstalled;

	public static String MarketplaceDiscoveryStrategy_downloadError;

	public static String MarketplaceDiscoveryStrategy_failedToSaveMarketplaceInfo;

	public static String MarketplaceDiscoveryStrategy_FavoritesRefreshing;

	public static String MarketplaceDiscoveryStrategy_FavoritesRetrieve;

	public static String MarketplaceDiscoveryStrategy_FavoritesRetrieveError;

	public static String MarketplaceDiscoveryStrategy_findingInstalled;

	public static String MarketplaceDiscoveryStrategy_invalidFilter;

	public static String MarketplaceDiscoveryStrategy_loadingMarketplace;

	public static String MarketplaceDiscoveryStrategy_loadingResources;

	public static String MarketplaceDiscoveryStrategy_Name_and_Version;

	public static String MarketplaceDiscoveryStrategy_noNameMatch;

	public static String MarketplaceDiscoveryStrategy_noUrlMatch;

	public static String MarketplaceDiscoveryStrategy_ParseError;

	public static String MarketplaceDiscoveryStrategy_requestSource;

	public static String MarketplaceDiscoveryStrategy_saveMarketplaceInfoJobName;

	public static String MarketplaceDiscoveryStrategy_searchingMarketplace;

	public static String MarketplaceDiscoveryStrategy_sendingErrorNotification;

	public static String MarketplaceDiscoveryStrategy_unidentifiableItem;

	public static String MarketplaceDiscoveryStrategy_unknownFilter;

	public static String MarketplaceInfo_LoadError;

	public static String MarketplaceNodeCatalogItem_changeSupportAccessError;

	public static String MarketplaceNodeCatalogItem_changeSupportError;
	
	public static String ResourceProvider_downloadError;

	public static String ResourceProvider_FailedCreatingTempDir;

	public static String ResourceProvider_retrievingResource;

	public static String ResourceProvider_waitingForDownload;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
