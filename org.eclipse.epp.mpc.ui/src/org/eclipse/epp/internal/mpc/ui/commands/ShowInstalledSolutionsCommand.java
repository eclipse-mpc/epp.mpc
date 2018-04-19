/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;

public class ShowInstalledSolutionsCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IMarketplaceClientService marketplaceClientService = MarketplaceClient.getMarketplaceClientService();
		IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
		marketplaceClientService.openInstalled(config);
		return null;
	}

}
