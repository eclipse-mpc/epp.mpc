/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import org.eclipse.epp.internal.mpc.core.util.DebugTraceUtil;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.service.component.annotations.Component;

public class MarketplaceClientCorePlugin {

	public static final String DEBUG_OPTION = "/debug"; //$NON-NLS-1$

	public static final String DEBUG_FAKE_CLIENT_OPTION = "/client/fakeVersion"; //$NON-NLS-1$

	public static final String DEBUG_CLIENT_OPTIONS_PATH = MarketplaceClientCore.BUNDLE_ID + "/client/"; //$NON-NLS-1$

	public static final String DEBUG_CLIENT_REMOVE_OPTION = "xxx"; //$NON-NLS-1$

	public static boolean DEBUG = false;

	public static boolean DEBUG_FAKE_CLIENT = false;

	private static DebugTrace debugTrace;

	public static void trace(String option, String message) {
		final DebugTrace trace = debugTrace;
		if (DEBUG && trace != null) {
			trace.trace(option, message);
		}
	}

	public static void trace(String option, String message, Object... parameters) {
		final DebugTrace trace = debugTrace;
		if (DEBUG && trace != null) {
			DebugTraceUtil.trace(trace, option, message, parameters);
		}
	}

	@Component(name = "org.eclipse.epp.mpc.core.debug.options", property = {
	"listener.symbolic.name=org.eclipse.epp.mpc.core" })
	public static class DebugOptionsInitializer implements DebugOptionsListener {

		@Override
		public void optionsChanged(DebugOptions options) {
			DebugTrace debugTrace = null;
			boolean debug = options.getBooleanOption(MarketplaceClientCore.BUNDLE_ID + DEBUG_OPTION, false);
			boolean fakeClient = false;
			if (debug) {
				debugTrace = options.newDebugTrace(MarketplaceClientCore.BUNDLE_ID);
				fakeClient = options.getBooleanOption(MarketplaceClientCore.BUNDLE_ID + DEBUG_FAKE_CLIENT_OPTION,
						false);
			}
			DEBUG = debug;
			DEBUG_FAKE_CLIENT = fakeClient;
			MarketplaceClientCorePlugin.debugTrace = debugTrace;
		}

	}
}
