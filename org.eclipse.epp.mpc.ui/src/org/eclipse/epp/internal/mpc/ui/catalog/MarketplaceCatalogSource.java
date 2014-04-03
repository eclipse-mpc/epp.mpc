/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.io.IOException;
import java.net.URL;

import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;

/**
 * @author David Green
 */
public class MarketplaceCatalogSource extends AbstractCatalogSource {

	private final IMarketplaceService marketplaceService;

	private ResourceProvider resourceProvider;

	public MarketplaceCatalogSource(IMarketplaceService marketplaceService) {
		this.marketplaceService = marketplaceService;
		try {
			resourceProvider = new ResourceProvider();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Object getId() {
		return marketplaceService;
	}

	@Override
	public URL getResource(String resourceName) {
		return resourceProvider.getResource(resourceName);
	}

	public ResourceProvider getResourceProvider() {
		return resourceProvider;
	}

	public IMarketplaceService getMarketplaceService() {
		return marketplaceService;
	}

	public void dispose() {
		if (resourceProvider != null) {
			resourceProvider.dispose();
			resourceProvider = null;
		}
	}
}
