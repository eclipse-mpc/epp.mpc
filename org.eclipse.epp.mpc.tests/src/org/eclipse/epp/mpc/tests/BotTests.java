/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests;

import org.eclipse.epp.mpc.tests.ui.wizard.MarketplaceClientServiceTest;
import org.eclipse.epp.mpc.tests.ui.wizard.MarketplaceWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author David Green
 */
@RunWith(Suite.class)
@SuiteClasses({ //
MarketplaceClientServiceTest.class, MarketplaceWizardTest.class
})
public class BotTests {

}
