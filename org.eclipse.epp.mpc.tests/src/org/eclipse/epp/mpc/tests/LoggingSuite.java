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
package org.eclipse.epp.mpc.tests;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class LoggingSuite extends Suite {

	private static final RunListener LOG_LISTENER = new RunListener() {

		@Override
		public void testStarted(Description description) throws Exception {
			log("Starting test " + description.getDisplayName());
		}

		@Override
		public void testFinished(Description description) throws Exception {
			log("Finished test " + description.getDisplayName());
		}

		@Override
		public void testFailure(Failure failure) throws Exception {
			log(IStatus.ERROR, "Test " + failure.getDescription().getDisplayName() + "failed: " + failure
					.getMessage(), failure.getException());
		}

		@Override
		public void testAssumptionFailure(Failure failure) {
			log(IStatus.INFO, "Assumption failed for " + failure.getDescription().getDisplayName() + ": " + failure
					.getMessage(), failure.getException());
		}

		@Override
		public void testIgnored(Description description) throws Exception {
			log("Skipped test " + description.getDisplayName());
		}
	};

	private static boolean isLogging = false;

	public LoggingSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
		super(klass, builder);
	}

	public LoggingSuite(RunnerBuilder builder, Class<?>[] classes) throws InitializationError {
		super(builder, classes);
	}

	@Override
	public void run(RunNotifier notifier) {
		boolean wasLogging = isLogging;
		if (!wasLogging) {
			notifier.addListener(LOG_LISTENER);
			isLogging = true;
		}
		try {
			super.run(notifier);
		} finally {
			if (!wasLogging) {
				notifier.removeListener(LOG_LISTENER);
				isLogging = false;
			}
		}
	}

	public static boolean isLogging() {
		return isLogging;
	}

	private static void log(String message) {
		log(IStatus.INFO, message, null);
	}

	private static void log(int severity, String message, Throwable ex) {
		MarketplaceClientCore.getLog().log(new Status(severity, "org.eclipse.epp.mpc.tests", message, ex));
		java.lang.System.out.println(message);
		if (ex != null) {
			ex.printStackTrace();
		}
	}

}
