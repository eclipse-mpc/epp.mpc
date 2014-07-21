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
package org.eclipse.epp.internal.mpc.core.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

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
				return new URI(url.getProtocol(), url.getAuthority(), decode(url.getPath()),
						encodeQuery(url.getQuery()),
						url.getRef());
			} catch (URISyntaxException e1) {
				//throw original error
				throw e;
			}
		}
	}

	private static String decode(String path) {
		try {
			return path == null ? null : URLDecoder.decode(path, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			//should not be possible
			return path;
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

	public static String encode(String url) throws URISyntaxException {
		return toURI(url).toString();
	}

	private static String encodeQuery(String query) {
		return query == null ? null : query.replace(" ", "+"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
