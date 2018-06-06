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
package org.eclipse.epp.internal.mpc.core.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;

public class URLUtil {

	public static URI toURI(String s) throws URISyntaxException {
		try {
			return new URI(s);
		} catch (URISyntaxException e) {
			URL url;
			try {
				url = new URL(s);
			} catch (MalformedURLException e1) {
				//throw original error
				throw e;
			}
			if (!s.equals(url.toString())) {
				try {
					return url.toURI();
				} catch (URISyntaxException e1) {
					//keep going
				}
			}
			try {
				return new URI(url.getProtocol(), url.getAuthority(), urlDecode(url.getPath()),
						encodeQuery(url.getQuery()),
						url.getRef());
			} catch (URISyntaxException e1) {
				//throw original error
				throw e;
			}
		}
	}

	public static URL toURL(String s) throws MalformedURLException {
		try {
			//try going through URI for proper encoding
			URI uri = toURI(s);
			return uri.toURL();
		} catch (URISyntaxException e) {
			throw new MalformedURLException(e.getMessage());
		} catch (RuntimeException e) {
			//fall back to direct URL construction
			return new URL(s);
		}
	}

	public static String encode(String value) {
		if (value == null) {
			return null;
		}
		try {
			return URLEncoder.encode(value, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			// should not happen anyways
			throw new RuntimeException(e);
		}
	}

	private static String encodeQuery(String query) {
		return query == null ? null : query.replace(" ", "+"); //$NON-NLS-1$//$NON-NLS-2$
	}

	public static String toggleHttps(String url) {
		if (url.startsWith("http:")) { //$NON-NLS-1$
			url = "https:" + url.substring("http:".length()); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (url.startsWith("https:")) { //$NON-NLS-1$
			url = "http:" + url.substring("https:".length()); //$NON-NLS-1$//$NON-NLS-2$
		}
		return url;
	}

	public static String appendPath(String... urlParts) {
		if (urlParts == null || urlParts.length == 0) {
			return null;
		} else if (urlParts.length == 1) {
			return urlParts[0];
		}
		StringBuilder url = new StringBuilder();
		for (String part : urlParts) {
			if (((url.length() > 0 && url.charAt(url.length() - 1) != '/') && (part.length() == 0 || part.charAt(0) != '/'))
					|| (url.length() == 0 && part.length() == 0)) {
				url.append('/');
			}
			url.append(part);
		}
		return url.toString();
	}

	public static String urlEncode(String s) {
		try {
			return s == null ? null : URLEncoder.encode(s, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			//this should be impossible
			MarketplaceClientCore.error(e);
			return s;
		}
	}

	public static String urlDecode(String path) {
		try {
			return path == null ? null : URLDecoder.decode(path, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			//should not be possible
			return path;
		}
	}

	public static String setScheme(String url, String scheme) {
		int schemeSeparator = url.indexOf(":"); //$NON-NLS-1$
		if (schemeSeparator == -1) {
			throw new IllegalArgumentException();
		}
		return scheme + url.substring(schemeSeparator);
	}
}
