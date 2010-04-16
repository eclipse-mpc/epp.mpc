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

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.epp.internal.mpc.ui.catalog.messages"; //$NON-NLS-1$

	public static String AbstractResourceRunnable_badUri;

	public static String AbstractResourceRunnable_resourceNotFound;

	public static String MarketplaceCatalog_checkingForUpdates;

	public static String MarketplaceCatalog_failedWithError;

	public static String MarketplaceCatalog_queryFailed;

	public static String MarketplaceCatalog_queryingMarketplace;
	public static String MarketplaceDiscoveryStrategy_catalogCategory;

	public static String MarketplaceDiscoveryStrategy_findingInstalled;

	public static String MarketplaceDiscoveryStrategy_loadingMarketplace;

	public static String MarketplaceDiscoveryStrategy_loadingResources;

	public static String MarketplaceDiscoveryStrategy_searchingMarketplace;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
