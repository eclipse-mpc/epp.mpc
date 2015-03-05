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

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.mpc.tests.service.SolutionCompatibilityFilterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Carsten Reckord
 */
@RunWith(Suite.class)
@SuiteClasses({ //
//	UnmarshallerTest.class, //
	//	TextUtilTest.class, //
	//	TransportFactoryTest.class, //
	//	CatalogServiceTest.class, //
	//	DefaultMarketplaceServiceTest.class, //
	SolutionCompatibilityFilterTest.class
})
public class RestTests {

	private static Timer timer;

	@BeforeClass
	public static void start() {
		MarketplaceClientCore.error("RestTests started", null);
		final Thread testThread = Thread.currentThread();
		timer = new Timer("stacktrace-logger", true);
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
				StackTraceElement[] testTrace = traces.get(testThread);
				if (testTrace != null) {
					Throwable dump = new Throwable();
					dump.setStackTrace(testTrace);
					MarketplaceClientCore.error("RestTests thread dump", dump);
				}
			}

		}, 1000, 30000);
	}

	@AfterClass
	public static void stop() {
		Timer cancel = timer;
		timer = null;
		cancel.cancel();
	}
}
