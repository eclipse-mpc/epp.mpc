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
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests;

import org.eclipse.epp.mpc.tests.service.CatalogServiceTest;
import org.eclipse.epp.mpc.tests.service.DefaultMarketplaceServiceTest;
import org.eclipse.epp.mpc.tests.service.SolutionCompatibilityFilterTest;
import org.eclipse.epp.mpc.tests.service.xml.UnmarshallerTest;
import org.eclipse.epp.mpc.tests.util.ProxyConfigurationTest;
import org.eclipse.epp.mpc.tests.util.TextUtilTest;
import org.eclipse.epp.mpc.tests.util.TransportFactoryTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Carsten Reckord
 */
@RunWith(LoggingSuite.class)
@SuiteClasses({ //
	ProxyConfigurationTest.class, //
	UnmarshallerTest.class, //
	TextUtilTest.class, //
	TransportFactoryTest.class, //
	CatalogServiceTest.class, //
	DefaultMarketplaceServiceTest.class, //
	SolutionCompatibilityFilterTest.class
})
public class RestTests {
}
