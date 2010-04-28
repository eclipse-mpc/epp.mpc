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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Browser integration for the marketplace that intercepts calls to install buttons and causes them to open the
 * marketplace wizard.
 * 
 * @author dgreen
 */
public class MarketplaceBrowserIntegration implements LocationListener, OpenWindowListener {

	private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

	private static final String PARAM_SPLIT_REGEX = "&"; //$NON-NLS-1$

	private static final String MPC_INSTALL_URI = "/mpc/install?"; //$NON-NLS-1$

	private static final String EQUALS_REGEX = "="; //$NON-NLS-1$

	private static final String MPC_STATE = "mpc_state"; //$NON-NLS-1$

	private static final String MPC_INSTALL = "mpc_install"; //$NON-NLS-1$

	private final List<CatalogDescriptor> catalogDescriptors;

	private final CatalogDescriptor catalogDescriptor;

	public MarketplaceBrowserIntegration(List<CatalogDescriptor> catalogDescriptors, CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptors == null || catalogDescriptors.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		this.catalogDescriptors = new ArrayList<CatalogDescriptor>(catalogDescriptors);
		this.catalogDescriptor = catalogDescriptor;
	}

	public void open(WindowEvent event) {
		// if the user shift-clicks the button this can happen
	}

	public void changing(LocationEvent event) {
		if (!event.doit) {
			return;
		}
		URL url = catalogDescriptor.getUrl();
		String catalogLocation;
		try {
			catalogLocation = url.toURI().toString();
		} catch (URISyntaxException e) {
			return;
		}
		if (catalogLocation.endsWith("/")) { //$NON-NLS-1$
			catalogLocation = catalogLocation.substring(0, catalogLocation.length() - 1);
		}
		if (event.location.startsWith(catalogLocation)) {
			String suffix = event.location.substring(catalogLocation.length());
			if (suffix.startsWith(MPC_INSTALL_URI) && suffix.length() > MPC_INSTALL_URI.length()) {
				String[] args = suffix.substring(MPC_INSTALL_URI.length()).split(PARAM_SPLIT_REGEX);
				String installId = null;
				String mpcState = null;
				for (String arg : args) {
					String[] parts = arg.split(EQUALS_REGEX);
					if (parts.length == 2) {
						String key = parts[0];
						if (MPC_INSTALL.equals(key)) {
							installId = parts[1];
						} else if (MPC_STATE.equals(key)) {
							mpcState = parts[1];
						}
					}
				}
				if (installId != null) {
					event.doit = false;

					MarketplaceWizardCommand command = new MarketplaceWizardCommand();
					command.setCatalogDescriptors(catalogDescriptors);
					command.setSelectedCatalogDescriptor(catalogDescriptor);
					try {
						if (mpcState != null) {
							command.setWizardState(URLDecoder.decode(mpcState, UTF_8));
						}
						Map<String, Operation> nodeIdToOperation = new HashMap<String, Operation>();
						nodeIdToOperation.put(URLDecoder.decode(installId, UTF_8), Operation.INSTALL);
						command.setOperationByNodeId(nodeIdToOperation);
					} catch (UnsupportedEncodingException e1) {
						throw new IllegalStateException(e1);
					}
					try {
						command.execute(new ExecutionEvent());
					} catch (ExecutionException e) {
						IStatus status = MarketplaceClientUi.computeStatus(new InvocationTargetException(e),
								Messages.MarketplaceBrowserIntegration_cannotOpenMarketplaceWizard);
						StatusManager.getManager().handle(status,
								StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
					}
				}
			}
		}
	}

	public void changed(LocationEvent event) {
		// nothing to do.
	}

}
