/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;

/**
 * Browser integration for the marketplace that intercepts calls to install buttons and causes them to open the
 * marketplace wizard.
 * 
 * @author dgreen
 */
public class MarketplaceBrowserIntegration implements LocationListener, OpenWindowListener {


	private static final String MPC_INSTALL_URI = "/mpc/install?"; //$NON-NLS-1$

	public void open(WindowEvent event) {
		// if the user shift-clicks the button this can happen
	}

	public void changing(LocationEvent event) {
		if (!event.doit) {
			return;
		}
		if (isPotenialLocation(event)) {
			SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(event.location);
			if (info != null) {
				event.doit = false;
				MarketplaceUrlHandler.triggerInstall(info);
			}
		}
	}

	private boolean isPotenialLocation(LocationEvent event) {
		String url = event.location;
		return url.contains(MPC_INSTALL_URI) && MarketplaceUrlHandler.isPotentialSolution(url);
	}

	public void changed(LocationEvent event) {
		// nothing to do.
	}

}
