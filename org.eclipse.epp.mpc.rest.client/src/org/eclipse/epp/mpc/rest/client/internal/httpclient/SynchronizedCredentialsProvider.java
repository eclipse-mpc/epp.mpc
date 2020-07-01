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
package org.eclipse.epp.mpc.rest.client.internal.httpclient;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;

public class SynchronizedCredentialsProvider implements CredentialsProvider {

	private final CredentialsProvider delegate;

	private final Object lock;

	public SynchronizedCredentialsProvider(CredentialsProvider delegate) {
		super();
		this.delegate = delegate;
		Object lock = findLock(delegate);
		this.lock = lock == null ? this : lock;
	}

	private static Object findLock(CredentialsProvider credentialsProvider) {
		if (credentialsProvider instanceof SynchronizedCredentialsProvider) {
			return ((SynchronizedCredentialsProvider) credentialsProvider).lock;
		}
		if (credentialsProvider instanceof ChainedCredentialsProvider) {
			ChainedCredentialsProvider chain = (ChainedCredentialsProvider) credentialsProvider;
			Object lock = findLock(chain.getSecond());
			if (lock != null) {
				return lock;
			}
			return findLock(chain.getFirst());
		}
		return null;
	}

	@Override
	public void setCredentials(AuthScope authscope, Credentials credentials) {
		synchronized (lock) {
			delegate.setCredentials(authscope, credentials);
		}
	}

	@Override
	public Credentials getCredentials(AuthScope authscope) {
		synchronized (lock) {
			return delegate.getCredentials(authscope);
		}
	}

	@Override
	public void clear() {
		synchronized (lock) {
			delegate.clear();
		}
	}

	/**
	 * @noreference For test purposes only. This method is not intended to be referenced by clients.
	 */
	public CredentialsProvider getDelegate() {
		return delegate;
	}

}
