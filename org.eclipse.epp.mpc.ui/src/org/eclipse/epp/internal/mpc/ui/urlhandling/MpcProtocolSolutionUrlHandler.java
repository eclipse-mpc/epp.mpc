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

import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;

public class MpcProtocolSolutionUrlHandler extends AbstractMpcProtocolUrlHandler implements SolutionUrlHandler {
	private static final String INSTALL_ACTION = "install"; //$NON-NLS-1$

	@Override
	public boolean handles(String url) {
		return super.handles(url);
	}

	@Override
	public boolean isPotentialSolution(String url) {
		return url.contains("/" + INSTALL_ACTION + "/"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public SolutionInstallationInfo parse(String url) {
		Map<String, Object> properties = doParse(url);
		if (properties == null || !INSTALL_ACTION.equals(properties.get(ACTION))) {
			return null;
		}
		IPath itemPath = (IPath) properties.get(PATH_PARAMETERS);
		String installId = itemPath == null ? null : itemPath.toString();
		if (installId != null) {
			CatalogDescriptor descriptor = (CatalogDescriptor) properties.get(MPC_CATALOG);
			String state = (String) properties.get(MarketplaceUrlUtil.MPC_STATE);
			SolutionInstallationInfo info = new SolutionInstallationInfo(installId, state, descriptor);
			info.setRequestUrl(url);
			return info;
		}
		return null;
	}

	@Override
	public String getMPCState(String url) {
		Map<String, Object> properties = doParse(url);
		if (properties == null) {
			return null;
		}
		return (String) properties.get(MarketplaceUrlUtil.MPC_STATE);
	}

}