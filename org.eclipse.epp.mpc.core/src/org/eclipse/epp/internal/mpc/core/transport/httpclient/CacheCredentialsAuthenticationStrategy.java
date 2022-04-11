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

import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.AuthenticationStrategy;
import org.apache.hc.client5.http.auth.AuthChallenge;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.ChallengeType;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;

abstract class CacheCredentialsAuthenticationStrategy implements AuthenticationStrategy {

	public static final String CREDENTIALS_CACHE_ATTRIBUTE = CacheCredentialsAuthenticationStrategy.class.getName()
			+ ".credentialsCache"; //$NON-NLS-1$

	static class Target extends CacheCredentialsAuthenticationStrategy { // TODO httpclient5: not needed like that

		public Target(AuthenticationStrategy delegate) {
			super(delegate);
		}

		@Override
		protected CredentialsProvider getAuthState(HttpClientContext clientContext) {
			return clientContext.getCredentialsProvider();
		}

	}

	static class Proxy extends CacheCredentialsAuthenticationStrategy { // TODO httpclient5: not needed like that

		public Proxy(AuthenticationStrategy delegate) {
			super(delegate);
		}

		@Override
		protected CredentialsProvider getAuthState(HttpClientContext clientContext) {
			return clientContext.getCredentialsProvider();
		}
	}

	private final AuthenticationStrategy delegate;

	public CacheCredentialsAuthenticationStrategy(AuthenticationStrategy delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<AuthScheme> select(ChallengeType challengeType, Map<String, AuthChallenge> challenges,
			HttpContext context) {
		return delegate.select(challengeType, challenges, context);
	}

	// TODO httpclient5: @Override
	public void authSucceeded(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		// TODO httpclient5: delegate.authSucceeded(authhost, authScheme, context);
		if (authScheme != null && authScheme.isChallengeComplete()) {
			cacheCredentials(authhost, authScheme, context);
		}
	}

	// TODO httpclient5: @Override
	public void authFailed(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		// TODO httpclient5: delegate.authFailed(authhost, authScheme, context);
		uncacheCredentials(authhost, authScheme, context);
	}

	private void uncacheCredentials(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		CredentialsStore credentialsCache = getCredentialsCache(context);
		if (credentialsCache != null) {
			AuthScope scope = createAuthScope(authhost, authScheme);
			credentialsCache.setCredentials(scope, null);
		}
	}

	private void cacheCredentials(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		Credentials credentials = getCredentials(context);
		if (credentials != null) {
			CredentialsStore credentialsCache = getCredentialsCache(context);
			if (credentialsCache != null) {
				AuthScope scope = createAuthScope(authhost, authScheme);
				credentialsCache.setCredentials(scope, credentials);
			}
		}
	}

	protected Credentials getCredentials(HttpContext context) {
		HttpClientContext clientContext = HttpClientContext.adapt(context);
		CredentialsProvider authState = getAuthState(clientContext);
		if (authState != null) {
			return authState.getCredentials(null, context);
		}
		return null;
	}

	protected abstract CredentialsProvider getAuthState(HttpClientContext clientContext);

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
		return new AuthScope(targetHost, realm, schemeName);
	}

}
