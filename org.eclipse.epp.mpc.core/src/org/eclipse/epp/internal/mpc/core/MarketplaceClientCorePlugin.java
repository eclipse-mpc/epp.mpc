/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import org.eclipse.epp.internal.mpc.core.util.ProxyHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MarketplaceClientCorePlugin implements BundleActivator {

	private static Bundle bundle;

	public void start(BundleContext context) throws Exception {
		bundle = context.getBundle();
		ProxyHelper.acquireProxyService();
	}

	public void stop(BundleContext context) throws Exception {
		ProxyHelper.releaseProxyService();
		bundle = null;
	}

	public static Bundle getBundle() {
		return bundle;
	}
}
