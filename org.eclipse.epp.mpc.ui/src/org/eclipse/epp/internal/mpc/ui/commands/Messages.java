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
 *     Yatta Solutions - error handling (bug 374105)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.commands;

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.epp.internal.mpc.ui.commands.messages"; //$NON-NLS-1$

	public static String MarketplaceWizardCommand_allCategories;

	public static String MarketplaceWizardCommand_allMarkets;

	public static String MarketplaceWizardCommand_CannotInstallRemoteLocations;

	public static String MarketplaceWizardCommand_cannotOpenMarketplace;

	public static String MarketplaceWizardCommand_CouldNotFindMarketplaceForSolution;

	public static String MarketplaceWizardCommand_eclipseMarketplace;

	public static String MarketplaceWizardCommand_FailedRetrievingCatalogImage;

	public static String MarketplaceWizardCommand_FailedRetrievingCatalogWizardIcon;

	public static String MarketplaceWizardCommand_noRemoteCatalogs;

	public static String MarketplaceWizardCommand_requestCatalog;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
