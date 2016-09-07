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

import static org.eclipse.epp.internal.mpc.core.transport.httpclient.CacheCredentialsAuthenticationStrategy.CREDENTIALS_CACHE_ATTRIBUTE;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

public class CacheCredentialsInterceptor extends AbstractAddCredentialsProviderInterceptor {

	private final CredentialsProvider credentialsCache;

	public CacheCredentialsInterceptor(CacheCredentialsProvider credentialsCache) {
		this.credentialsCache = credentialsCache;
	}

	public CacheCredentialsInterceptor() {
		this.credentialsCache = new CacheCredentialsProvider();
	}

	public HttpContext intercept(HttpClient client, HttpContext context) {
		addCredentialsProvider(CREDENTIALS_CACHE_ATTRIBUTE, context);
		return context;
	}

	@Override
	protected boolean isMatchingProvider(CredentialsProvider credentialsProvider) {
		return credentialsProvider == credentialsCache;
	}

	@Override
	protected CredentialsProvider getCredentialsProviderToAdd() {
		return credentialsCache;
	}

	@Override
	protected ChainedCredentialsProvider chainCredentialsProviders(CredentialsProvider existingProvider,
			CredentialsProvider addedProvider) {
		return super.chainCredentialsProviders(addedProvider, existingProvider);
	}
}
