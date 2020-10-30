/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.rest.client.internal.resteasy;

import java.util.concurrent.Callable;

import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider;

class HttpContextInjector implements HttpContextProvider {

	private final ThreadLocal<HttpContext> localContext = new ThreadLocal<>();

	public <T> T withContext(HttpContext context, Callable<T> action) throws Exception {
		final HttpContext previousContext = localContext.get();
		try {
			localContext.set(context);
			return action.call();
		} finally {
			setLocalContext(previousContext);
		}
	}

	private void setLocalContext(final HttpContext context) {
		if (context == null) {
			localContext.remove();
		} else {
			localContext.set(context);
		}
	}

	@Override
	public HttpContext getContext() {
		return localContext.get();
	}
}
