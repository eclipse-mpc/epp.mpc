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

import java.util.List;

import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;

public class MarketplaceCategory extends CatalogCategory {

	private List<Market> markets;

	public void setMarkets(List<Market> markets) {
		this.markets = markets;
	}

	public List<Market> getMarkets() {
		return markets;
	}

}
