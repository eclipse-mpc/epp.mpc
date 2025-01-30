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
package org.eclipse.epp.internal.mpc.ui.urlhandling;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientDebug;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.urischeme.IUriSchemeHandler;

public class MarketplaceUriSchemeHandler implements IUriSchemeHandler {

	public static final String ECLIPSE_MPC_SCHEME = "eclipse+mpc"; //$NON-NLS-1$

	private final MarketplaceUrlHandler urlHandler = new MarketplaceUrlHandler() {

		@Override
		protected boolean handleInstallRequest(SolutionInstallationInfo installInfo, String url) {
			return triggerInstall(installInfo);
		}
	};

	public MarketplaceUriSchemeHandler() {
		// ignore
	}

	@Override
	public void handle(String mpcUri) {
		Display display = Display.getDefault();
		if (accept(mpcUri)) {
			display.asyncExec(() -> proceed(mpcUri));
		} else {
			traceInvalidUrl(mpcUri);
		}
	}

	private boolean accept(String mpcUri) {
		//eclipse+mpc://marketplace.eclipse.org/install/1640500
		//or eclipse+mpc://marketplace.eclipse.org/favorites/someuser
		return MarketplaceUrlHandler.isPotentialSolution(mpcUri);
	}

	private void proceed(String mpcUri) {
		urlHandler.handleUri(mpcUri);
	}

	private void traceInvalidUrl(String url) {
		if (MarketplaceClientDebug.DEBUG) {
			MarketplaceClientDebug.trace(MarketplaceClientDebug.DROP_ADAPTER_DEBUG_OPTION,
					"URL handler: Data is not a solution url: {0}", url, new Throwable()); //$NON-NLS-1$
		}
	}

}
