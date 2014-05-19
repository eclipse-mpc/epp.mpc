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

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.junit.Test;

public class URLUtilTest {

	@Test
	public void testSimpleUrl() throws MalformedURLException, URISyntaxException {
		URL url = URLUtil.toURL("http://marketplace.eclipse.org/sites/default/files/logo.png");
		assertEquals("http://marketplace.eclipse.org/sites/default/files/logo.png", url.toString());
	}

	@Test
	public void testUrlWithSpaceInPath() throws MalformedURLException, URISyntaxException {
		URL url = URLUtil.toURL("http://marketplace.eclipse.org/sites/default files/logo 2.png");
		assertEquals("http://marketplace.eclipse.org/sites/default%20files/logo%202.png", url.toString());
	}

	@Test
	public void testUrlWithSpaceInQuery() throws MalformedURLException, URISyntaxException {
		URL url = URLUtil.toURL("http://marketplace.eclipse.org/sites/default/files/logo.png?foo=bar baz");
		assertEquals("http://marketplace.eclipse.org/sites/default/files/logo.png?foo=bar+baz", url.toString());
	}

	@Test
	public void testEscapedUrl() throws MalformedURLException, URISyntaxException {
		URL url = URLUtil.toURL("http://marketplace.eclipse.org/sites/default%20files/logo%202.png");
		assertEquals("http://marketplace.eclipse.org/sites/default%20files/logo%202.png", url.toString());
	}

	@Test
	public void testPartiallyEscapedUrl() throws MalformedURLException, URISyntaxException {
		URL url = URLUtil.toURL("http://marketplace.eclipse.org/sites/default%20files/logo 2.png");
		assertEquals("http://marketplace.eclipse.org/sites/default%20files/logo%202.png", url.toString());
	}

}
