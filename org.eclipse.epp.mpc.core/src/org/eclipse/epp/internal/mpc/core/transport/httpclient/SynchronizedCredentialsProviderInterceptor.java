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
package org.eclipse.epp.internal.mpc.core.transport.httpclient;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

public class SynchronizedCredentialsProviderInterceptor implements HttpContextInterceptor {

	public static final SynchronizedCredentialsProviderInterceptor INSTANCE = new SynchronizedCredentialsProviderInterceptor();

	public HttpContext intercept(HttpClient client, HttpContext context) {
		HttpClientContext clientContext = HttpClientContext.adapt(context);
		CredentialsProvider credentialsProvider = clientContext.getCredentialsProvider();
		if (credentialsProvider instanceof SynchronizedCredentialsProvider) {
			return clientContext;
		} else if (credentialsProvider != null) {
			credentialsProvider = new SynchronizedCredentialsProvider(credentialsProvider);
			clientContext.setCredentialsProvider(credentialsProvider);
		}
		return clientContext;
	}
}
