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

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.core5.http.protocol.HttpContext;

public class ChainedCredentialsProvider implements CredentialsStore {

	private final CredentialsStore first;

	private final CredentialsStore second;

	public ChainedCredentialsProvider(CredentialsStore first, CredentialsStore second) {
		super();
		this.first = first;
		this.second = second;
	}

	@Override
	public void setCredentials(AuthScope authscope, Credentials credentials) {
		first.setCredentials(authscope, credentials);
	}

	@Override
	public Credentials getCredentials(AuthScope authscope, HttpContext context) {
		Credentials credentials = first.getCredentials(authscope, context);
		if (credentials != null) {
			return credentials;
		}
		credentials = second.getCredentials(authscope, context);
		context.setAttribute(CacheCredentialsAuthenticationStrategy.CURRENT_CREDENTIALS, credentials);
		return credentials;
	}

	@Override
	public void clear() {
		first.clear();
		second.clear();
	}

	/**
	 * @noreference For test purposes only. This method is not intended to be referenced by clients.
	 */
	public CredentialsStore getFirst() {
		return first;
	}

	/**
	 * @noreference For test purposes only. This method is not intended to be referenced by clients.
	 */
	public CredentialsStore getSecond() {
		return second;
	}
}
