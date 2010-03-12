/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.net.URL;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * @author David Green
 */
public class MarketplaceNodeCatalogItem extends CatalogItem {

	private URL marketplaceUrl;

	public URL getMarketplaceUrl() {
		return marketplaceUrl;
	}

	public void setMarketplaceUrl(URL marketplaceUrl) {
		this.marketplaceUrl = marketplaceUrl;
	}
}
