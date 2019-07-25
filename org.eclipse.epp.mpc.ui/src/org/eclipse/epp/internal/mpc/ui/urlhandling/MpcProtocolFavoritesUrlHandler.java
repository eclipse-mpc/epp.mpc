/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.urlhandling;

import java.net.URL;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.FavoritesDescriptor;

public class MpcProtocolFavoritesUrlHandler extends AbstractMpcProtocolUrlHandler implements FavoritesUrlHandler {

	private static final String IMPORT_FAVORITES_ACTION = "favorites"; //$NON-NLS-1$

	private static final String FAVORITES_URL_PATTERN = "%s/user/%s/favorites"; //$NON-NLS-1$

	@Override
	public boolean isPotentialFavoritesList(String url) {
		return url.contains("/" + IMPORT_FAVORITES_ACTION + "/"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public FavoritesDescriptor parse(String url) {
		Map<String, Object> properties = doParse(url);
		if (properties == null || !IMPORT_FAVORITES_ACTION.equals(properties.get(ACTION))) {
			return null;
		}
		IPath itemPath = (IPath) properties.get(PATH_PARAMETERS);
		String favoriteListId = itemPath == null ? null : itemPath.toString();
		if (favoriteListId != null) {
			String favoritesUrl = String.format(FAVORITES_URL_PATTERN,
					((URL) properties.get(MARKETPLACE_URL)).toString(), favoriteListId);
			CatalogDescriptor descriptor = (CatalogDescriptor) properties.get(MPC_CATALOG);
			return new FavoritesDescriptor(favoritesUrl, descriptor);
		}
		return null;
	}
}