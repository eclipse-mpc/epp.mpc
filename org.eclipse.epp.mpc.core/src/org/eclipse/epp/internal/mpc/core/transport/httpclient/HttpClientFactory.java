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

import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;

//FIXME remove...
@Deprecated
class HttpClientFactory extends org.eclipse.epp.mpc.rest.client.internal.httpclient.HttpClientFactory {

	private HttpClient client;

	private Executor executor;

	@Override
	public HttpClient build() {
		client = super.build();
		executor = Executor.newInstance(client).use(getCookieStore()).use(getCredentialsProvider());

		return client;
	}

	public Executor getExecutor() {
		return executor;
	}

	public HttpClient getClient() {
		return client;
	}
}
