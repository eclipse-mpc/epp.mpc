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
package org.eclipse.epp.mpc.ui;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;

/**
 * Provides a means to configure and launch the marketplace client.
 * 
 * @author David Green
 */
public class MarketplaceClientUI {
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
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setCatalogDescriptors(catalogDescriptors);

		command.execute(new ExecutionEvent());
	}
}
