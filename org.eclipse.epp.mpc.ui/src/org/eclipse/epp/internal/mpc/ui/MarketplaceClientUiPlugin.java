/*******************************************************************************
 * Copyright (c) 2010, 2019 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API, bug 413871: performance
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.core.util.DebugTraceUtil;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;

/**
 * bundle activator. Prefer {@link MarketplaceClientUi} where possible.
 *
 * @author David Green
 */
public class MarketplaceClientUiPlugin implements BundleActivator {

	/**
	 * image registry key
	 */
	public static final String IU_ICON_UPDATE = "IU_ICON_UPDATE"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_INSTALL = "IU_ICON_INSTALL"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_UNINSTALL = "IU_ICON_UNINSTALL"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_DISABLED = "IU_ICON_DISABLED"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON = "IU_ICON"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_ERROR = "IU_ICON_ERROR"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String NEWS_ICON_UPDATE = "NEWS_ICON_UPDATE"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String NO_ICON_PROVIDED = "NO_ICON_PROVIDED"; //$NON-NLS-1$

	public static final String NO_ICON_PROVIDED_CATALOG = "NO_ICON_PROVIDED_CATALOG"; //$NON-NLS-1$

	public static final String DEFAULT_MARKETPLACE_ICON = "DEFAULT_MARKETPLACE_ICON"; //$NON-NLS-1$

	public static final String ACTION_ICON_FAVORITES = "ACTION_ICON_FAVORITES"; //$NON-NLS-1$

	public static final String ACTION_ICON_LOGIN = "ACTION_ICON_LOGIN"; //$NON-NLS-1$

	public static final String ACTION_ICON_WARNING = "ACTION_ICON_WARNING"; //$NON-NLS-1$

	public static final String ACTION_ICON_UPDATE = "ACTION_ICON_UPDATE"; //$NON-NLS-1$

	public static final String FAVORITES_LIST_ICON = "FAVORITES_LIST_ICON"; //$NON-NLS-1$

	public static final String ITEM_ICON_STAR = "ITEM_ICON_STAR"; //$NON-NLS-1$

	public static final String ITEM_ICON_STAR_SELECTED = "ITEM_ICON_STAR_SELECTED"; //$NON-NLS-1$

	public static final String ITEM_ICON_SHARE = "ITEM_ICON_SHARE"; //$NON-NLS-1$

	public static final String DEBUG_OPTION = "/debug"; //$NON-NLS-1$

	public static final String DROP_ADAPTER_DEBUG_OPTION = DEBUG_OPTION + "/dnd"; //$NON-NLS-1$

	public static boolean DEBUG = false;

	private static DebugTrace debugTrace;

	public MarketplaceClientUiPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Job.getJobManager().cancel(context.getBundle());
		debugTrace = null;
	}

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

	@Component(name = "org.eclipse.epp.mpc.ui.debug.options", property = {
	"listener.symbolic.name=org.eclipse.epp.mpc.ui" })
	public static class DebugOptionsInitializer implements DebugOptionsListener {

		@Override
		public void optionsChanged(DebugOptions options) {
			DebugTrace debugTrace = null;
			boolean debug = options.getBooleanOption(MarketplaceClientUi.BUNDLE_ID + DEBUG_OPTION, false);
			if (debug) {
				debugTrace = options.newDebugTrace(MarketplaceClientUi.BUNDLE_ID);
			}
			DEBUG = debug;
			MarketplaceClientUiPlugin.debugTrace = debugTrace;
		}

	}
}
