/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;


import org.eclipse.epp.internal.mpc.core.service.Favorites;
import org.eclipse.epp.internal.mpc.core.service.Marketplace;


/**
 * @author David Green
 */
public class FavoritesContentHandler extends NodeListingContentHandler<Favorites> {

	public FavoritesContentHandler() {
		super("favorites"); //$NON-NLS-1$
	}

	@Override
	protected Favorites createModel() {
		return new Favorites();
	}

	@Override
	protected void setMarketplaceResult(Marketplace marketplace, Favorites model) {
		marketplace.setFavorites(model);
	}
}
