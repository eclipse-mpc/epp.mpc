/*******************************************************************************
 * Copyright (c) 2011 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.ui.statushandlers.StatusManager;

public class MarketplaceUrlHandler {

	public static class SolutionInstallationInfo {
		private String installId;

		private String state;

		private CatalogDescriptor catalogDescriptor;

		public String getInstallId() {
			return installId;
		}

		public String getState() {
			return state;
		}

		public CatalogDescriptor getCatalogDescriptor() {
			return catalogDescriptor;
		}
	}

	private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

	private static final String PARAM_SPLIT_REGEX = "&"; //$NON-NLS-1$

	private static final String EQUALS_REGEX = "="; //$NON-NLS-1$

	private static final String MPC_STATE = "mpc_state"; //$NON-NLS-1$

	private static final String MPC_INSTALL = "mpc_install"; //$NON-NLS-1$

	public static SolutionInstallationInfo createSolutionInstallInfo(String url) {
		String query;
		try {
			query = new URL(url).getQuery();
		} catch (MalformedURLException e) {
			return null;
		}
		if (query == null) {
			return null;
		}
		String[] params = query.split(PARAM_SPLIT_REGEX);
		String installId = null;
		String state = null;
		for (String param : params) {
			String[] keyValue = param.split(EQUALS_REGEX);
			if(keyValue.length == 2) {
				String key = keyValue[0];
				String value = keyValue[1];
				if (key.equals(MPC_INSTALL)) {
					installId = value;
				} else if (key.equals(MPC_STATE)) {
					state = value;
				}
			}
		}
		if (installId != null) {
			CatalogDescriptor descriptor = findCatalogDescriptor(url);
			if (descriptor != null) {
				SolutionInstallationInfo info = new SolutionInstallationInfo();
				info.installId = installId;
				info.state = state;
				info.catalogDescriptor = descriptor;
				return info;
			}
		}
		return null;
	}

	public static boolean isPotentialSolution(String url) {
		return url != null && url.contains(MPC_INSTALL);
	}

	private static CatalogDescriptor findCatalogDescriptor(String url) {
		if (url == null || url.length() == 0) {
			return null;
		}
		List<CatalogDescriptor> catalogDescriptors = CatalogRegistry.getInstance().getCatalogDescriptors();
		for (CatalogDescriptor catalogDescriptor : catalogDescriptors) {
			if (url.startsWith(catalogDescriptor.getUrl().toExternalForm())) {
				return catalogDescriptor;
			}
		}
		return null;
	}

	public static void triggerInstall(SolutionInstallationInfo info) {
		String installId = info.getInstallId();
		String mpcState = info.getState();
		CatalogDescriptor catalogDescriptor = info.getCatalogDescriptor();
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
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
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		}
	}

	private MarketplaceUrlHandler() {
		// no instantiation
	}
}
