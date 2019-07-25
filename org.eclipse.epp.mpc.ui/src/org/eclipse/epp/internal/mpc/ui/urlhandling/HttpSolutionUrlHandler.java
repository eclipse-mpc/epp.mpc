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

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;

public class HttpSolutionUrlHandler implements SolutionUrlHandler {

	@Override
	public boolean handles(String url) {
		return url != null && url.toLowerCase().startsWith("http"); //$NON-NLS-1$
	}

	@Override
	public boolean isPotentialSolution(String url) {
		return url.contains(MarketplaceUrlUtil.MPC_INSTALL);
	}

	@Override
	public SolutionInstallationInfo parse(String url) {
		String installId = null;
		String state = null;
		Map<String, String> query = MarketplaceUrlUtil.parseQuery(url);
		if (query != null) {
			installId = MarketplaceUrlUtil.getInstallId(query);
			state = MarketplaceUrlUtil.getMpcState(query);
		}
		if (installId != null) {
			CatalogDescriptor descriptor = MarketplaceUrlUtil.findCatalogDescriptor(url, true);
			SolutionInstallationInfo info = new SolutionInstallationInfo(installId, state, descriptor);
			info.setRequestUrl(url);
			return info;
		}
		return null;
	}

	@Override
	public String getMPCState(String url) {
		Map<String, String> query = MarketplaceUrlUtil.parseQuery(url);
		return query == null ? null : MarketplaceUrlUtil.getMpcState(query);
	}
}