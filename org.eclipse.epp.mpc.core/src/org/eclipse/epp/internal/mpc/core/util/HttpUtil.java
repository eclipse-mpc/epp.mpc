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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;

/**
 * @author Carsten Reckord
 */
public class HttpUtil {

	public static HttpClient createHttpClient(String baseUri) {
		HttpClientBuilder hcBuilder = HttpClients.custom();
		hcBuilder.setUserAgent(MarketplaceClientCore.BUNDLE_ID);

		if (baseUri != null) {
			configureProxy(hcBuilder, baseUri);
		}

		return hcBuilder.build();
	}

	public static void configureProxy(HttpClientBuilder hcBuilder, String url) {
		final IProxyData proxyData = ProxyHelper.getProxyData(url);
		if (proxyData != null && !IProxyData.SOCKS_PROXY_TYPE.equals(proxyData.getType())) {
			HttpHost proxy = new HttpHost(proxyData.getHost(), proxyData.getPort());
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			hcBuilder.setRoutePlanner(routePlanner);
			if (proxyData.isRequiresAuthentication()) {
				CredentialsProvider provider = new BasicCredentialsProvider();
				provider.setCredentials(new AuthScope(proxyData.getHost(), proxyData.getPort()),
						new UsernamePasswordCredentials(proxyData.getUserId(), proxyData.getPassword()));
				hcBuilder.setDefaultCredentialsProvider(provider);
			}
		}
	}
}
