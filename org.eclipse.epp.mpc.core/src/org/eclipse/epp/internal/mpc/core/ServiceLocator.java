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
package org.eclipse.epp.internal.mpc.core;

import org.eclipse.epp.internal.mpc.core.service.CatalogService;
import org.eclipse.epp.internal.mpc.core.service.DefaultCatalogService;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.MarketplaceService;

/**
 * A service locator for obtaining {@link MarketplaceService} instances.
 * 
 * @author David Green
 */
public class ServiceLocator {

	private static ServiceLocator instance = new ServiceLocator();

	public MarketplaceService getMarketplaceService() {
		return new DefaultMarketplaceService();
	}

	public CatalogService getCatalogService() {
		return new DefaultCatalogService();
	}

	/**
	 * for testing purposes
	 */
	public static synchronized void setInstance(ServiceLocator instance) {
		if (instance == null) {
			throw new IllegalArgumentException();
		}
		ServiceLocator.instance = instance;
	}

	public static synchronized ServiceLocator getInstance() {
		return instance;
	}

}
