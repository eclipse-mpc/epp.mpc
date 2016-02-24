/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ide.IUnassociatedEditorStrategy;

/**
 * For a given file, search entries on marketplace that would match the search "fileExtension_${extension}". MarketPlace
 * entry can declare support for some extension by adding these terms as tags.
 *
 * @author mistria
 */
public class AskMarketPlaceForFileSupportStrategy implements IUnassociatedEditorStrategy {

	public AskMarketPlaceForFileSupportStrategy() {
	}

	public IEditorDescriptor getEditorDescriptor(String fileName, IEditorRegistry editorRegistry)
			throws CoreException, OperationCanceledException {
		IMarketplaceClientService marketplaceClientService = MarketplaceClient.getMarketplaceClientService();
		IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
		String[] split = fileName.split("\\."); //$NON-NLS-1$
		String query = "fileExtension_" + split[split.length - 1]; //$NON-NLS-1$
		marketplaceClientService.openSearch(config, null, null, query);
		// just after openSearch, we cannot immediately resolve to an editor. This require a restart.
		// Assuming necessary extension points are dynamic, we could:
		// 1. wait for install operation completed
		// 2. apply change to update editor bundles and extension registry
		// 3. ask registry about best editor
		return editorRegistry.getDefaultEditor(fileName);
	}

}
