/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - error handling (bug 374105), news (bug 401721), public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;

/**
 * Browser integration for the marketplace that intercepts calls to install buttons and causes them to open the
 * marketplace wizard.
 *
 * @author dgreen
 * @author Carsten Reckord
 */
public class MarketplaceBrowserIntegration extends MarketplaceUrlHandler implements LocationListener,
OpenWindowListener {

	public void open(WindowEvent event) {
		// if the user shift-clicks the button this can happen
	}

	public void changing(LocationEvent event) {
		if (!event.doit) {
			return;
		}
		if (handleUri(event.location)) {
			event.doit = false;
		}
	}

	@Override
	protected boolean handleInstallRequest(SolutionInstallationInfo installInfo, String url) {
		org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.triggerInstall(installInfo);
		return true;
	}

	public void changed(LocationEvent event) {
		// nothing to do.
	}

}
