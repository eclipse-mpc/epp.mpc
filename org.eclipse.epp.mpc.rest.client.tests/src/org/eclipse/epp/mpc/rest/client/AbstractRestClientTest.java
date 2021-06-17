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
package org.eclipse.epp.mpc.rest.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public abstract class AbstractRestClientTest {

	protected IRestClientFactory restClientFactory;
	private ServiceReference<IRestClientFactory> restClientFactoryServiceReference;

	@BeforeEach
	void setUp() {
		BundleContext bundleContext = FrameworkUtil.getBundle(IRestClientFactory.class).getBundleContext();
		restClientFactoryServiceReference = bundleContext.getServiceReference(IRestClientFactory.class);
		restClientFactory = bundleContext.getService(restClientFactoryServiceReference);
	}

	@AfterEach
	void tearDown() {
		if (restClientFactory != null) {
			BundleContext bundleContext = FrameworkUtil.getBundle(IRestClientFactory.class).getBundleContext();
			bundleContext.ungetService(restClientFactoryServiceReference);
		}
	}

}