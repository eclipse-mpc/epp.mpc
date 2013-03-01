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

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;

/**
 * @author Carsten Reckord
 */
public class HttpUtil {

	public static HttpClient createHttpClient(String baseUri) {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT, MarketplaceClientCore.BUNDLE_ID);

		if (baseUri != null) {
			configureProxy(client, baseUri);
		}

		return client;
	}

	public static void configureProxy(HttpClient client, String url) {
		final IProxyData proxyData = ProxyHelper.getProxyData(url);
		if (proxyData != null) {
			HostConfiguration hostConfiguration = client.getHostConfiguration();
			if (hostConfiguration == null) {
				hostConfiguration = new HostConfiguration();
				client.setHostConfiguration(hostConfiguration);
			}
			hostConfiguration.setProxy(proxyData.getHost(), proxyData.getPort());
		}
	}
}
