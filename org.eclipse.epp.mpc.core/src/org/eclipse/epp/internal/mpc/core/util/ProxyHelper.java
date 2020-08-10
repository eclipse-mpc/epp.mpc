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
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * @author Carsten Reckord
 */
public class ProxyHelper {

	public static IProxyData getProxyData(String url, IProxyService proxyService) {
		if (proxyService != null) {
			URI uri;
			try {
				uri = new URI(url);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			return doGetProxyData(proxyService, uri);
		}
		return null;
	}

	public static IProxyData getProxyData(URI uri, IProxyService proxyService) {
		if (proxyService != null) {
			return doGetProxyData(proxyService, uri);
		}
		return null;
	}

	private static IProxyData doGetProxyData(final IProxyService proxyService, URI uri) {
		if (uri.getHost() == null || uri.getScheme() == null) {
			return null;
		}
		final IProxyData[] proxyData = proxyService.select(uri);
		if (proxyData == null) {
			return null;
		}
		for (IProxyData pd : proxyData) {
			if (pd != null && pd.getHost() != null) {
				return pd;
			}
		}
		return null;
	}
}
