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
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.util;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

import java.net.URI;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class ProxyConfigurationTest {

	private BundleContext bundleContext;

	private ServiceReference<IProxyService> proxyServiceReference;

	private IProxyService proxyService;

	@Before
	public void getProxyService() {
		bundleContext = FrameworkUtil.getBundle(MarketplaceClientCore.class).getBundleContext();
		proxyServiceReference = bundleContext.getServiceReference(IProxyService.class);
		proxyService = bundleContext.getService(proxyServiceReference);
	}

	@After
	public void ungetProxyService() {
		proxyService = null;
		if (proxyServiceReference != null) {
			bundleContext.ungetService(proxyServiceReference);
			proxyServiceReference = null;
		}
	}

	@Test
	public void testConsistentHttpProxyConfiguration() {
		assertConsistentProxyConfiguration("http");
	}

	@Test
	public void testConsistentHttpsProxyConfiguration() {
		assertConsistentProxyConfiguration("https");
	}

	private void assertConsistentProxyConfiguration(String protocol) {
		String proxyHost = System.getProperty(protocol + ".proxyHost");
		String proxyPort = System.getProperty(protocol + ".proxyPort");
		assumeNotNull(proxyHost, proxyPort);

		IProxyData[] proxyDatas = proxyService.select(URI.create(protocol + "://github.com"));

		assertNotNull(proxyDatas);
		assertEquals(1, proxyDatas.length);

		IProxyData proxyData = proxyDatas[0];

		assertEquals(proxyHost, proxyData.getHost());
		assertNotNull(proxyPort);
		assertEquals(proxyPort, String.valueOf(proxyData.getPort()));
	}
}
