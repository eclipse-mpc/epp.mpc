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
package org.eclipse.epp.mpc.tests.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.rules.ExternalResource;

public class TemporaryEnvironment extends ExternalResource {

	private static final Object MISSING_VALUE = new Object();

	private final Map<String, Object> originalProperties = new HashMap<>();

	public TemporaryEnvironment set(String property, String value) {
		Properties systemProperties = System.getProperties();
		boolean containsKey = systemProperties.containsKey(property);
		Object originalValue;
		if (value == null) {
			originalValue = systemProperties.remove(property);
			if (originalValue == null && !containsKey) {
				return this;
			}
		} else {
			originalValue = systemProperties.setProperty(property, value);
			if (value.equals(originalValue)) {
				return this;
			}
		}
		if (originalValue == null && !containsKey) {
			originalValue = MISSING_VALUE;
		}
		if (!originalProperties.containsKey(property)) {
			originalProperties.put(property, originalValue);
		}
		return this;
	}

	public TemporaryEnvironment reset(String property) {
		Object originalValue = originalProperties.get(property);
		if (originalValue == null && !originalProperties.containsKey(property)) {
			return this;
		}
		Properties systemProperties = System.getProperties();
		if (MISSING_VALUE == originalValue) {
			systemProperties.remove(property);
		} else {
			systemProperties.put(property, originalValue);
		}
		originalProperties.remove(property);
		return this;
	}

	public TemporaryEnvironment resetAll() {
		Properties systemProperties = System.getProperties();
		for (Entry<String, Object> entry : originalProperties.entrySet()) {
			Object originalValue = entry.getValue();
			if (MISSING_VALUE == originalValue) {
				systemProperties.remove(entry.getKey());
			} else {
				systemProperties.put(entry.getKey(), originalValue);
			}
		}
		originalProperties.clear();
		return this;
	}

	@Override
	protected void after() {
		resetAll();
	}
}
