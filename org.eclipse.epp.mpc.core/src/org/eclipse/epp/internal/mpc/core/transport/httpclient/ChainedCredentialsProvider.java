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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;

public class ChainedCredentialsProvider implements CredentialsProvider {

	private final CredentialsProvider first;

	private final CredentialsProvider second;

	public ChainedCredentialsProvider(CredentialsProvider first, CredentialsProvider second) {
		super();
		this.first = first;
		this.second = second;
	}

	public void setCredentials(AuthScope authscope, Credentials credentials) {
		first.setCredentials(authscope, credentials);
	}

	public Credentials getCredentials(AuthScope authscope) {
		Credentials credentials = first.getCredentials(authscope);
		if (credentials != null) {
			return credentials;
		}
		return second.getCredentials(authscope);
	}

	public void clear() {
		first.clear();
		second.clear();
	}

	/**
	 * @noreference For test purposes only. This method is not intended to be referenced by clients.
	 */
	public CredentialsProvider getFirst() {
		return first;
	}

	/**
	 * @noreference For test purposes only. This method is not intended to be referenced by clients.
	 */
	public CredentialsProvider getSecond() {
		return second;
	}
}
