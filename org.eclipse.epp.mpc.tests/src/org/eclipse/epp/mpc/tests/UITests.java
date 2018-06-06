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

import org.eclipse.epp.mpc.tests.ui.catalog.CatalogDescriptorTest;
import org.eclipse.epp.mpc.tests.ui.catalog.MarketplaceInfoSerializationTest;
import org.eclipse.epp.mpc.tests.ui.catalog.MarketplaceInfoTest;
import org.eclipse.epp.mpc.tests.ui.wizard.MarketplaceUrlHandlerTest;
import org.eclipse.epp.mpc.tests.ui.wizard.SelectionModelStateSerializerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author David Green
 */
@RunWith(UISuite.class)
@SuiteClasses({ //
	SelectionModelStateSerializerTest.class, //
	MarketplaceUrlHandlerTest.class, //
	MarketplaceInfoTest.class, //
	MarketplaceInfoSerializationTest.class, //
	CatalogDescriptorTest.class //

})
public class UITests {

}
