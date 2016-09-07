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
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

abstract class AbstractAddCredentialsProviderInterceptor implements HttpContextInterceptor {

	protected abstract boolean isMatchingProvider(CredentialsProvider credentialsProvider);

	protected CredentialsProvider findExistingProvider(CredentialsProvider credentialsProvider) {
		if (isMatchingProvider(credentialsProvider)) {
			return credentialsProvider;
		}
		if (credentialsProvider instanceof ChainedCredentialsProvider) {
			ChainedCredentialsProvider chain = (ChainedCredentialsProvider) credentialsProvider;
			CredentialsProvider existingProvider = findExistingProvider(chain.getSecond());
			if (existingProvider != null) {
				return existingProvider;
			}
			return findExistingProvider(chain.getFirst());
		}
		if (credentialsProvider instanceof SynchronizedCredentialsProvider) {
			SynchronizedCredentialsProvider syncProvider = (SynchronizedCredentialsProvider) credentialsProvider;
			return findExistingProvider(syncProvider.getDelegate());
		}
		return null;
	}

	protected void addCredentialsProvider(String providerAttribute, HttpContext context) {
		Object value = context.getAttribute(providerAttribute);
		if (value != null) {
			return;
		}
		HttpClientContext clientContext = HttpClientContext.adapt(context);
		CredentialsProvider credentialsProvider = clientContext.getCredentialsProvider();
		CredentialsProvider resultProvider = findExistingProvider(credentialsProvider);
		if (resultProvider == null) {
			resultProvider = getCredentialsProviderToAdd();
			if (credentialsProvider == null) {
				credentialsProvider = resultProvider;
			} else {
				credentialsProvider = chainCredentialsProviders(credentialsProvider, resultProvider);
			}
			clientContext.setCredentialsProvider(credentialsProvider);
		}
		clientContext.setAttribute(providerAttribute, resultProvider);
	}

	protected abstract CredentialsProvider getCredentialsProviderToAdd();

	protected ChainedCredentialsProvider chainCredentialsProviders(CredentialsProvider existingProvider,
			CredentialsProvider addedProvider) {
		return new ChainedCredentialsProvider(existingProvider, addedProvider);
	}

}