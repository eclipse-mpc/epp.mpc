/*******************************************************************************
 * Copyright (c) 2010, 2026 The Eclipse Foundation and others.
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
package org.eclipse.epp.mpc.tests.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.junit.jupiter.api.Test;

public class URLUtilTest {

	@Test
	public void testSimpleUrl() throws MalformedURLException {
		URL url = URLUtil.toURL("https://marketplace.eclipse.org/sites/default/files/logo.png");
		assertEquals("https://marketplace.eclipse.org/sites/default/files/logo.png", url.toString());
	}

	@Test
	public void testUrlWithSpaceInPath() throws MalformedURLException, URISyntaxException {
		URL url = URLUtil.toURL("https://marketplace.eclipse.org/sites/default files/logo 2.png");
		assertEquals("https://marketplace.eclipse.org/sites/default%20files/logo%202.png", url.toString());
	}

	@Test
	public void testUrlWithSpaceInQuery() throws MalformedURLException {
		URL url = URLUtil.toURL("https://marketplace.eclipse.org/sites/default/files/logo.png?foo=bar baz");
		assertEquals("https://marketplace.eclipse.org/sites/default/files/logo.png?foo=bar+baz", url.toString());
	}

	@Test
	public void testEscapedUrl() throws MalformedURLException {
		URL url = URLUtil.toURL("https://marketplace.eclipse.org/sites/default%20files/logo%202.png");
		assertEquals("https://marketplace.eclipse.org/sites/default%20files/logo%202.png", url.toString());
	}

	@Test
	public void testPartiallyEscapedUrl() throws MalformedURLException {
		URL url = URLUtil.toURL("https://marketplace.eclipse.org/sites/default%20files/logo 2.png");
		assertEquals("https://marketplace.eclipse.org/sites/default%20files/logo%202.png", url.toString());
	}

	@Test
	public void testEmptyUrl() {
		assertThrows(MalformedURLException.class, () -> URLUtil.toURL(""));
	}

	@Test
	public void testRelativeUrl() {
		assertThrows(MalformedURLException.class, () -> URLUtil.toURL("sites/default/files/logo.png"));
	}

	@Test
	public void testNullUrl() {
		assertThrows(MalformedURLException.class, () -> URLUtil.toURL(null));
	}

}
