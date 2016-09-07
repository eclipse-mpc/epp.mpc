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

import java.util.Map;
import java.util.Queue;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

abstract class CacheCredentialsAuthenticationStrategy implements AuthenticationStrategy {

	public static final String CREDENTIALS_CACHE_ATTRIBUTE = CacheCredentialsInterceptor.class.getName()
			+ ".credentialsCache";

	static class Target extends CacheCredentialsAuthenticationStrategy {

		public Target(AuthenticationStrategy delegate) {
			super(delegate);
		}

		@Override
		protected AuthState getAuthState(HttpClientContext clientContext) {
			return clientContext.getTargetAuthState();
		}
	}

	static class Proxy extends CacheCredentialsAuthenticationStrategy {

		public Proxy(AuthenticationStrategy delegate) {
			super(delegate);
		}

		@Override
		protected AuthState getAuthState(HttpClientContext clientContext) {
			return clientContext.getProxyAuthState();
		}
	}

	private final AuthenticationStrategy delegate;

	public CacheCredentialsAuthenticationStrategy(AuthenticationStrategy delegate) {
		this.delegate = delegate;
	}

	public boolean isAuthenticationRequested(HttpHost authhost, HttpResponse response, HttpContext context) {
		return delegate.isAuthenticationRequested(authhost, response, context);
	}

	public Map<String, Header> getChallenges(HttpHost authhost, HttpResponse response, HttpContext context)
			throws MalformedChallengeException {
		return delegate.getChallenges(authhost, response, context);
	}

	public Queue<AuthOption> select(Map<String, Header> challenges, HttpHost authhost, HttpResponse response,
			HttpContext context) throws MalformedChallengeException {
		return delegate.select(challenges, authhost, response, context);
	}

	public void authSucceeded(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		delegate.authSucceeded(authhost, authScheme, context);
		if (authScheme != null && authScheme.isComplete()) {
			cacheCredentials(authhost, authScheme, context);
		}
	}

	public void authFailed(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		delegate.authFailed(authhost, authScheme, context);
		uncacheCredentials(authhost, authScheme, context);
	}

	private void uncacheCredentials(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		CredentialsProvider credentialsCache = getCredentialsCache(context);
		if (credentialsCache != null) {
			AuthScope scope = createAuthScope(authhost, authScheme);
			credentialsCache.setCredentials(scope, null);
		}
	}

	private void cacheCredentials(HttpHost authhost, AuthScheme authScheme, HttpContext context) {
		Credentials credentials = getCredentials(context);
		if (credentials != null) {
			CredentialsProvider credentialsCache = getCredentialsCache(context);
			if (credentialsCache != null) {
				AuthScope scope = createAuthScope(authhost, authScheme);
				credentialsCache.setCredentials(scope, credentials);
			}
		}
	}

	protected Credentials getCredentials(HttpContext context) {
		HttpClientContext clientContext = HttpClientContext.adapt(context);
		AuthState authState = getAuthState(clientContext);
		if (authState != null) {
			return authState.getCredentials();
		}
		return null;
	}

	protected abstract AuthState getAuthState(HttpClientContext clientContext);

	private static CredentialsProvider getCredentialsCache(HttpContext context) {
		CredentialsProvider credentialsCache = null;
		Object value = context.getAttribute(CREDENTIALS_CACHE_ATTRIBUTE);
		if (value instanceof CredentialsProvider) {
			credentialsCache = (CredentialsProvider) value;
		}
		return credentialsCache;
	}

	private static AuthScope createAuthScope(HttpHost targetHost, AuthScheme scheme) {
		String schemeName = null;
		String realm = null;
		if (scheme != null) {
			schemeName = scheme.getSchemeName();
			realm = scheme.getRealm();
		}
		return new AuthScope(targetHost, realm, schemeName);
	}

}
