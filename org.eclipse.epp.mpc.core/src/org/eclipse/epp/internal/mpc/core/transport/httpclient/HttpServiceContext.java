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

import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

public class HttpServiceContext {

	private final CloseableHttpClient client;

	private final CookieStore cookieStore;

	private final CredentialsStore credentialsProvider;

	private final CredentialsStore credentialsCacheProvider;

	private final CredentialsStore initialCredentialsProvider;

	HttpServiceContext(CloseableHttpClient client, CookieStore cookieStore, CredentialsStore credentialsProvider,
			CredentialsStore initialCredentialsProvider, CredentialsStore credentialsCacheProvider) {
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

	public CredentialsStore getCredentialsProvider() {
		return credentialsProvider;
	}

	CredentialsStore getInitialCredentialsProvider() {
		return initialCredentialsProvider;
	}

	CredentialsStore getCredentialsCacheProvider() {
		return credentialsCacheProvider;
	}
}
