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

import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.osgi.util.NLS;

public class FavoriteListCatalogItem extends CatalogItem {

	public FavoriteListCatalogItem() {
		super();
	}

	@Override
	public void setData(Object data) {
		setFavoriteList((IFavoriteList) data);
	}

	@Override
	public IFavoriteList getData() {
		return (IFavoriteList) super.getData();
	}

	public void setFavoriteList(IFavoriteList favoriteList) {
		super.setData(favoriteList);
	}

	public IFavoriteList getFavoriteList() {
		return getData();
	}

	public String getListName() {
		String name = getFavoriteList().getName();
		if (name == null) {
			name = NLS.bind(Messages.FavoriteListCatalogItem_defaultListName, getOwner());
		}
		return name;
	}

	public String getListUrl() {
		return getFavoriteList().getUrl();
	}

	public String getOwner() {
		return getFavoriteList().getOwner();
	}
}
