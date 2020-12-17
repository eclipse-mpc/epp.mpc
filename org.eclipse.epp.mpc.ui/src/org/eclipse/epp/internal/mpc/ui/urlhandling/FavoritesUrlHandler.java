/*******************************************************************************
 * Copyright (c) 2018, 2020 The Eclipse Foundation and others.
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

import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.FavoritesDescriptor;

public interface FavoritesUrlHandler extends UrlHandlerStrategy {

	boolean isPotentialFavoritesList(String url);

	FavoritesDescriptor parse(String url);

	Registry<FavoritesUrlHandler> DEFAULT = new Registry<>() {
		private final FavoritesUrlHandler[] handlers = new FavoritesUrlHandler[] { new HttpFavoritesUrlHandler(),
				new MpcProtocolFavoritesUrlHandler() };

		@Override
		protected FavoritesUrlHandler[] getUrlHandlers() {
			return handlers;
		}
	};
}