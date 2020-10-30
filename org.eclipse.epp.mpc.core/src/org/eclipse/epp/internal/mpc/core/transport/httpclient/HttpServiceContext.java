/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.transport.httpclient;

import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpServiceContext {

	private final CloseableHttpClient client;

	private final CookieStore cookieStore;

	private final CredentialsProvider credentialsProvider;

	private final CredentialsProvider credentialsCacheProvider;

	private final CredentialsProvider initialCredentialsProvider;

	HttpServiceContext(CloseableHttpClient client, CookieStore cookieStore, CredentialsProvider credentialsProvider,
			CredentialsProvider initialCredentialsProvider, CredentialsProvider credentialsCacheProvider) {
		this.client = client;
		this.cookieStore = cookieStore;
		this.credentialsProvider = credentialsProvider;
		this.initialCredentialsProvider = initialCredentialsProvider;
		this.credentialsCacheProvider = credentialsCacheProvider;
	}

	public CloseableHttpClient getClient() {
		return client;
	}

	public CookieStore getCookieStore() {
		return cookieStore;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	CredentialsProvider getInitialCredentialsProvider() {
		return initialCredentialsProvider;
	}

	CredentialsProvider getCredentialsCacheProvider() {
		return credentialsCacheProvider;
	}
}
