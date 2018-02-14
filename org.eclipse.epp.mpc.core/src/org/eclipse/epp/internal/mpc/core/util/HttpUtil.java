/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;

/**
 * @author Carsten Reckord
 */
public class HttpUtil {

	public static HttpClient createHttpClient(String baseUri) {
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, MarketplaceClientCore.BUNDLE_ID);

		if (baseUri != null) {
			configureProxy(client, baseUri);
		}

		return client;
	}

	public static void configureProxy(HttpClient client, String url) {
		final IProxyData proxyData = ProxyHelper.getProxyData(url);
		if (proxyData != null && !IProxyData.SOCKS_PROXY_TYPE.equals(proxyData.getType())) {
			HttpHost proxy = new HttpHost(proxyData.getHost(), proxyData.getPort());
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

			if (proxyData.isRequiresAuthentication()) {
				((AbstractHttpClient) client).getCredentialsProvider().setCredentials(
						new AuthScope(proxyData.getHost(), proxyData.getPort()),
						new UsernamePasswordCredentials(proxyData.getUserId(), proxyData.getPassword()));
			}
		}
	}
}
