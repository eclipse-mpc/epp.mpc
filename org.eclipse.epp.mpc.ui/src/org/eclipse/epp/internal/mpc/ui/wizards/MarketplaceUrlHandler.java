/*******************************************************************************
 * Copyright (c) 2011, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Yatta Solutions - news (bug 401721), public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;

/**
 * @author David Green
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 * @deprecated use {@link org.eclipse.epp.mpc.ui.MarketplaceUrlHandler} instead
 */
@Deprecated
public abstract class MarketplaceUrlHandler extends org.eclipse.epp.mpc.ui.MarketplaceUrlHandler {

	/**
	 * @deprecated use {@link org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo} instead
	 */
	@Deprecated
	public static class SolutionInstallationInfo extends
	org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo {

		@Deprecated
		public SolutionInstallationInfo() {
			super();
		}

		@Deprecated
		protected SolutionInstallationInfo(String installId, String state, CatalogDescriptor catalogDescriptor) {
			super(installId, state, catalogDescriptor);
		}
	}

	@Deprecated
	public static SolutionInstallationInfo createSolutionInstallInfo(String url) {
		org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo solutionInstallInfo = org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.createSolutionInstallInfo(url);

		return wrap(solutionInstallInfo);
	}

	@Deprecated
	private static SolutionInstallationInfo wrap(
			org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo solutionInstallInfo) {
		return new SolutionInstallationInfo(solutionInstallInfo.getInstallId(), solutionInstallInfo.getState(),
				solutionInstallInfo.getCatalogDescriptor());
	}

	@Deprecated
	protected boolean handleInstallRequest(SolutionInstallationInfo installInfo, String url) {
		return false;
	}

	@Override
	@Deprecated
	protected boolean handleInstallRequest(
			org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo installInfo, String url) {
		if (installInfo instanceof SolutionInstallationInfo) {
			return handleInstallRequest((SolutionInstallationInfo) installInfo, url);
		}
		return handleInstallRequest(wrap(installInfo), url);
	}
}
