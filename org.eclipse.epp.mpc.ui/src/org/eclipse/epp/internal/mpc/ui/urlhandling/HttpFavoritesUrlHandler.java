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

import java.util.regex.Pattern;

import org.eclipse.epp.internal.mpc.core.service.UserFavoritesService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.FavoritesDescriptor;

public class HttpFavoritesUrlHandler implements FavoritesUrlHandler {

	private static final Pattern FAVORITES_URL_PATTERN = UserFavoritesService.FAVORITES_URL_PATTERN;

	private static final Pattern FAVORITES_API_URL_PATTERN = Pattern
			.compile("(?:^|/)marketplace/favorites/?(?:\\?(?:[^#]*&)?name=.*)?$"); //$NON-NLS-1$

	@Override
	public boolean handles(String url) {
		return url != null && url.toLowerCase().startsWith("http"); //$NON-NLS-1$
	}

	@Override
	public FavoritesDescriptor parse(String url) {
		CatalogDescriptor catalogDescriptor = MarketplaceUrlUtil.findCatalogDescriptor(url, true);
		return new FavoritesDescriptor(url, catalogDescriptor);
	}

	@Override
	public boolean isPotentialFavoritesList(String url) {
		return FAVORITES_URL_PATTERN.matcher(url).find() || FAVORITES_API_URL_PATTERN.matcher(url).find();
	}
}