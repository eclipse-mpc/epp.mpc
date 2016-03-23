/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public abstract class TestProperties {
	private TestProperties() {
		throw new UnsupportedOperationException();
	}

	private static Properties testProperties;

	public static synchronized Properties getTestProperties() throws IOException {
		return new Properties(internalGetTestProperties());
	}

	private static synchronized Properties internalGetTestProperties() throws IOException {
		if (testProperties == null) {
			testProperties = loadTestProperties();
		}
		return testProperties;
	}

	private static Properties loadTestProperties() throws IOException {
		Properties properties = null;
		URL testBundleResource = TestProperties.class.getResource("/org.eclipse.epp.mpc.tests.properties");
		if (testBundleResource != null) {
			try (InputStream in = testBundleResource.openStream()) {
				properties = new Properties(properties);
				properties.load(in);
			}
		}
		File testUserFile = new File(System.getProperty("user.home"), "org.eclipse.epp.mpc.tests.properties");
		if (testUserFile.isFile()) {
			try (InputStream in = new FileInputStream(testUserFile)) {
				properties = new Properties(properties);
				properties.load(in);
			}
		}
		File testExecutionFile = new File("./org.eclipse.epp.mpc.tests.properties");
		if (testExecutionFile.isFile()) {
			try (InputStream in = new FileInputStream(testExecutionFile)) {
				properties = new Properties(properties);
				properties.load(in);
			}
		}
		if (properties == null) {
			return System.getProperties();
		}
		properties = new Properties(properties);
		properties.putAll(System.getProperties());
		return properties;
	}

	public static String getTestProperty(String key) throws IOException {
		return getTestProperty(key, null);
	}

	public static String getTestProperty(String key, String dflt) throws IOException {
		return getTestProperties().getProperty(key, dflt);
	}
}
