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

public class SynchronizedCredentialsProvider implements CredentialsStore {

	private final CredentialsStore delegate;

	private final Object lock;

	public SynchronizedCredentialsProvider(CredentialsStore delegate) {
		super();
		this.delegate = delegate;
		Object lock = findLock(delegate);
		this.lock = lock == null ? this : lock;
	}

	private static Object findLock(CredentialsStore credentialsProvider) {
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
	public Credentials getCredentials(AuthScope authscope, HttpContext context) {
		synchronized (lock) {
			return delegate.getCredentials(authscope, context);
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
	public CredentialsStore getDelegate() {
		return delegate;
	}

}
