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
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests;

import org.eclipse.epp.mpc.tests.ui.wizard.MarketplaceClientServiceTest;
import org.eclipse.epp.mpc.tests.ui.wizard.MarketplaceWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author David Green
 */
@RunWith(LoggingSuite.class)
@SuiteClasses({ //
	MarketplaceClientServiceTest.class, MarketplaceWizardTest.class
})
public class BotTests {

}
