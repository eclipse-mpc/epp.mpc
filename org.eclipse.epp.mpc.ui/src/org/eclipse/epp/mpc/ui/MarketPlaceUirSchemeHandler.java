/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.urischeme.IUriSchemeHandler;

public class MarketPlaceUirSchemeHandler implements IUriSchemeHandler {

	public MarketPlaceUirSchemeHandler() {
		// ignore
	}

	@Override
	public void handle(String mpcUri) {
		String uri = mpcUri.replaceFirst("eclipse-mpc://", "https://"); //$NON-NLS-1$ //$NON-NLS-2$

		Display display = Display.getDefault();
		if (MarketplaceUrlHandler.isPotentialSolution(uri)) {
			//https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1640500
			display.asyncExec(() -> proceedInstallation(uri));
		} else if (MarketplaceUrlHandler.isPotentialFavoritesList(uri)) {
			//https://marketplace.eclipse.org/user/xxx/favorites
			display.asyncExec(() -> proceedFavorites(uri));
		} else {
			traceInvalidUrl(uri);
		}
	}

	private void proceedInstallation(String url) {
		SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(url);
		if (info != null) {
			MarketplaceUrlHandler.triggerInstall(info);
		}
	}

	private void proceedFavorites(String url) {
		MarketplaceUrlHandler.triggerFavorites(url);
	}

	private void traceInvalidUrl(String url) {
		if (MarketplaceClientUiPlugin.DEBUG) {
			MarketplaceClientUiPlugin.trace(MarketplaceClientUiPlugin.DROP_ADAPTER_DEBUG_OPTION,
					"Drop event: Data is not a solution url: {0}", url, new Throwable()); //$NON-NLS-1$
		}
	}

}
