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

import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;

public interface SolutionUrlHandler extends UrlHandlerStrategy {
	boolean isPotentialSolution(String url);

	SolutionInstallationInfo parse(String url);

	String getMPCState(String url);

	Registry<SolutionUrlHandler> DEFAULT = new Registry<SolutionUrlHandler>() {
		private final SolutionUrlHandler[] handlers = new SolutionUrlHandler[] { new HttpSolutionUrlHandler(),
				new MpcProtocolSolutionUrlHandler() };

		@Override
		protected SolutionUrlHandler[] getUrlHandlers() {
			return handlers;
		}
	};
}