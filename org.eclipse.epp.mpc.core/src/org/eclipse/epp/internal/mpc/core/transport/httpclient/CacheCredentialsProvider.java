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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;

class CacheCredentialsProvider implements CredentialsProvider {

	private final HashMap<AuthScope, Credentials> credentials;

	/**
	 * Default constructor.
	 */
	public CacheCredentialsProvider() {
		super();
		this.credentials = new HashMap<>();
	}

	public synchronized void removeCredentials(final AuthScope authscope) {
		if (authscope == null) {
			throw new IllegalArgumentException("Authentication scope may not be null");
		}
		Credentials match = getCredentials(authscope);
		if (match == null) {
			return;
		}
		Iterator<Entry<AuthScope, Credentials>> entries = credentials.entrySet().iterator();
		Entry<AuthScope, Credentials> nextMatch;
		while ((nextMatch = findNextCredentials(entries, authscope, -1)) != null) {
			Credentials nextCredentials = nextMatch.getValue();
			if (match.equals(nextCredentials)) {
				entries.remove();
			}
		}
	}

	@Override
	public synchronized void setCredentials(final AuthScope authscope, final Credentials credentials) {
		if (authscope == null) {
			throw new IllegalArgumentException("Authentication scope may not be null");
		}
		if (credentials == null) {
			removeCredentials(authscope);
		} else {
			this.credentials.put(authscope, credentials);
		}
	}

	private Credentials findBestCredentials(final AuthScope authscope) {
		Credentials bestMatch = credentials.get(authscope);
		if (bestMatch == null) {
			// Nope.
			// Do a full scan
			int bestMatchFactor = -1;
			Iterator<Entry<AuthScope, Credentials>> entries = credentials.entrySet().iterator();
			Entry<AuthScope, Credentials> nextMatch;
			while ((nextMatch = findNextCredentials(entries, authscope, bestMatchFactor)) != null) {
				bestMatch = nextMatch.getValue();
				bestMatchFactor = authscope.match(nextMatch.getKey());
			}
		}
		return bestMatch;
	}

	private static Map.Entry<AuthScope, Credentials> findNextCredentials(
			Iterator<Map.Entry<AuthScope, Credentials>> iterator,
			final AuthScope authscope, int minFactor) {
		while (iterator.hasNext()) {
			Entry<AuthScope, Credentials> current = iterator.next();
			final int factor = authscope.match(current.getKey());
			if (factor > minFactor) {
				return current;
			}
		}
		return null;
	}

	@Override
	public synchronized Credentials getCredentials(final AuthScope authscope) {
		if (authscope == null) {
			throw new IllegalArgumentException("Authentication scope may not be null");
		}
		return findBestCredentials(authscope);
	}

	@Override
	public synchronized void clear() {
		this.credentials.clear();
	}

	@Override
	public String toString() {
		return credentials.toString();
	}
}
