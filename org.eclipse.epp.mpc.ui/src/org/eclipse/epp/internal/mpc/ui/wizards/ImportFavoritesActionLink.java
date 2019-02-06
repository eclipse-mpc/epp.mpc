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
package org.eclipse.epp.internal.mpc.ui.wizards;

public class ImportFavoritesActionLink extends ActionLink {

	public static final String IMPORT_ACTION_ID = "import"; //$NON-NLS-1$

	private final MarketplacePage marketplacePage;

	public ImportFavoritesActionLink(MarketplacePage page) {
		super(IMPORT_ACTION_ID, Messages.UserFavoritesAbstractImportActionItem_importFavoritesActionLabel,
				Messages.UserFavoritesAbstractImportActionItem_importFavoritesTooltip);
		this.marketplacePage = page;
	}

	@Override
	public void selected() {
		marketplacePage.getWizard().importFavorites(null);
	}
}
