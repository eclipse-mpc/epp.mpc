/*******************************************************************************
 * Copyright (c) 2010, 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;

/**
 * Provides a means to configure and launch the marketplace client.
 *
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceClient {
	/**
	 * Add a catalog descriptor to those available to be the user when accessing the marketplace.
	 *
	 * @param catalogDescriptor
	 *            the descriptor, must not be null
	 * @see #removeCatalogDescriptor(CatalogDescriptor)
	 */
	public static void addCatalogDescriptor(CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		CatalogRegistry.getInstance().register(catalogDescriptor);
	}

	/**
	 * Remove a catalog descriptor from those available to the user when accessing the marketplace.
	 *
	 * @see #addCatalogDescriptor(CatalogDescriptor)
	 */
	public static void removeCatalogDescriptor(CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		CatalogRegistry.getInstance().unregister(catalogDescriptor);
	}

	/**
	 * Open the marketplace wizard, which will prompt the user to select software to install or uninstall. Must only be
	 * called from the UI thread.
	 * <p>
	 * Upon return of this method the UI will have been displayed, however any provisioning operations instigated by the
	 * user may not have completed.
	 * </p>
	 *
	 * @param catalogDescriptors
	 *            the catalogs to query, or null if the default catalogs should be used.
	 * @throws IllegalArgumentException
	 *             if the given catalogs are not null and either empty or improperly configured
	 * @throws ExecutionException
	 *             if an exception occurs when attempting to present the UI
	 */
	public static void openMarketplaceWizard(List<CatalogDescriptor> catalogDescriptors)
			throws IllegalArgumentException, ExecutionException {
		if (catalogDescriptors != null) {
			if (catalogDescriptors.isEmpty()) {
				throw new IllegalArgumentException();
			}
			for (CatalogDescriptor descriptor : catalogDescriptors) {
				if (descriptor.getUrl() == null) {
					throw new IllegalArgumentException();
				}
				if (descriptor.getLabel() == null) {
					throw new IllegalArgumentException();
				}
			}
		}
		IMarketplaceClientService clientService = getMarketplaceClientService();
		IMarketplaceClientConfiguration config = clientService.newConfiguration();
		if (catalogDescriptors != null) {
			config.setCatalogDescriptors(catalogDescriptors);
		}
		clientService.open(config);
	}

	/**
	 * Convenience method to retrieve a registered {@link IMarketplaceClientService client service} for opening the MPC
	 * wizard dialog.
	 *
	 * @return a client service from the OSGi service registry
	 */
	public static IMarketplaceClientService getMarketplaceClientService() {
		return MarketplaceClientUiPlugin.getInstance().getClientService();
	}
}
