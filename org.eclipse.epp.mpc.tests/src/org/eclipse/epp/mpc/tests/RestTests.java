/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
