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
package org.eclipse.epp.mpc.tests.util;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.junit.Test;

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

	@Test(expected = MalformedURLException.class)
	public void testEmptyUrl() throws MalformedURLException {
		URLUtil.toURL("");
	}

	@Test(expected = MalformedURLException.class)
	public void testRelativeUrl() throws MalformedURLException {
		URLUtil.toURL("sites/default/files/logo.png");
	}

	@Test(expected = MalformedURLException.class)
	public void testNullUrl() throws MalformedURLException {
		URLUtil.toURL(null);
	}

}
