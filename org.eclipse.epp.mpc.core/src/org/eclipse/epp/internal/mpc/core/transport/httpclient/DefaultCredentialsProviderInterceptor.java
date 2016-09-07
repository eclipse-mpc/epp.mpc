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
import org.apache.http.protocol.HttpContext;

public class DefaultCredentialsProviderInterceptor extends AbstractAddCredentialsProviderInterceptor implements HttpContextInterceptor {

	private static final String CREDENTIALS_PROVIDER_ATTRIBUTE = DefaultCredentialsProviderInterceptor.class
			.getName() + ".credentialsProvider"; //$NON-NLS-1$

	private final CredentialsProvider defaultCredentialsProvider;

	public DefaultCredentialsProviderInterceptor(CredentialsProvider defaultCredentialsProvider) {
		super();
		this.defaultCredentialsProvider = defaultCredentialsProvider;
	}

	public HttpContext intercept(HttpClient client, HttpContext context) {
		String providerAttribute = CREDENTIALS_PROVIDER_ATTRIBUTE;
		addCredentialsProvider(providerAttribute, context);
		return context;
	}

	@Override
	protected CredentialsProvider getCredentialsProviderToAdd() {
		return defaultCredentialsProvider;
	}

	@Override
	protected boolean isMatchingProvider(CredentialsProvider credentialsProvider) {
		return credentialsProvider == defaultCredentialsProvider;
	}
}
