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
package org.eclipse.epp.internal.mpc.core.transport.httpclient;

import java.io.IOException;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;

class CacheCredentialsAuthenticationStrategy implements HttpResponseInterceptor {

	public static final String CREDENTIALS_CACHE_ATTRIBUTE = CacheCredentialsAuthenticationStrategy.class.getName()
			+ ".credentialsCache"; //$NON-NLS-1$

	public static final String CURRENT_CREDENTIALS = CacheCredentialsAuthenticationStrategy.class.getName()
			+ ".currentCredentials"; //$NON-NLS-1$

	@Override
	public void process(HttpResponse response, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		final HttpClientContext clientContext = HttpClientContext.adapt(context);
		HttpRoute route = (HttpRoute) clientContext.getHttpRoute();
		HttpHost authhost = route.getProxyHost();
		// also for target: HttpHost authhost = AuthSupport.resolveAuthTarget(clientContext.getRequest(), route);
		AuthScheme authScheme = clientContext.getAuthExchange(authhost).getAuthScheme();
		if (response.getCode() == HttpStatus.SC_UNAUTHORIZED
				|| response.getCode() == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
			uncacheCredentials(authhost, authScheme, clientContext);
		} else {
			if (authScheme != null && authScheme.isChallengeComplete()) {
				cacheCredentials(authhost, authScheme, clientContext);
			}
		}
	}

	private void uncacheCredentials(HttpHost authhost, AuthScheme authScheme, HttpClientContext context) {
		CredentialsStore credentialsCache = getCredentialsCache(context);
		if (credentialsCache != null) {
			AuthScope scope = createAuthScope(authhost, authScheme);
			// SUPPORT for PKI enabled
			if (scope != null) {
				credentialsCache.setCredentials(scope, null);
			}
		}
	}

	private void cacheCredentials(HttpHost authhost, AuthScheme authScheme, HttpClientContext context) {
		AuthScope scope = createAuthScope(authhost, authScheme);
		Credentials credentials = context.getAttribute(CURRENT_CREDENTIALS, Credentials.class);
		if (credentials != null) {
			CredentialsStore credentialsCache = getCredentialsCache(context);
			if (credentialsCache != null) {
				credentialsCache.setCredentials(scope, credentials);
			}
		}
	}

	private static CredentialsStore getCredentialsCache(HttpContext context) {
		CredentialsStore credentialsCache = null;
		Object value = context.getAttribute(CREDENTIALS_CACHE_ATTRIBUTE);
		if (value instanceof CredentialsStore) {
			credentialsCache = (CredentialsStore) value;
		}
		return credentialsCache;
	}

	private static AuthScope createAuthScope(HttpHost targetHost, AuthScheme scheme) {
		String schemeName = null;
		String realm = null;
		if (scheme != null) {
			schemeName = scheme.getName();
			realm = scheme.getRealm();
		}
		// SUPPORT for PKI enabled
		try {
			return new AuthScope(targetHost, realm, schemeName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

}
