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

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

class ExecutorClientWrapper implements HttpClient {
	private final HttpClient client;

	private final HttpContextInterceptor[] contextInterceptors;

	ExecutorClientWrapper(HttpClient client, HttpContextInterceptor... interceptors) {
		this.client = client;
		if (interceptors != null && interceptors.length == 0) {
			interceptors = null;
		}
		this.contextInterceptors = interceptors;
	}

	ExecutorClientWrapper(HttpClient client, HttpContextInterceptor interceptor) {
		this.client = client;
		this.contextInterceptors = interceptor == null ? null : new HttpContextInterceptor[] { interceptor };
	}

	@Deprecated
	public HttpParams getParams() {
		return client.getParams();
	}

	@Deprecated
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
		return client.execute(request);
	}

	public HttpResponse execute(HttpUriRequest request, HttpContext context)
			throws IOException, ClientProtocolException {
		HttpResponse response = null;
		response = client.execute(request, intercept(context));
		return response;
	}

	public HttpResponse execute(HttpHost target, HttpRequest request)
			throws IOException, ClientProtocolException {
		return client.execute(target, request);
	}

	public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context)
			throws IOException, ClientProtocolException {
		HttpResponse response = null;
		response = client.execute(target, request, intercept(context));
		return response;
	}

	public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		return client.execute(request, responseHandler);
	}

	public <T> T execute(HttpUriRequest request, final ResponseHandler<? extends T> responseHandler,
			HttpContext context) throws IOException, ClientProtocolException {
		final HttpResponse[] response = new HttpResponse[1];
		return client.execute(request, new ResponseHandler<T>() {

			public T handleResponse(HttpResponse theResponse) throws ClientProtocolException, IOException {
				response[0] = theResponse;
				return responseHandler.handleResponse(theResponse);
			}
		}, intercept(context));
	}

	public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		return client.execute(target, request, responseHandler);
	}

	public <T> T execute(HttpHost target, HttpRequest request,
			final ResponseHandler<? extends T> responseHandler,
			HttpContext context) throws IOException, ClientProtocolException {
		final HttpResponse[] response = new HttpResponse[1];
		return client.execute(target, request, new ResponseHandler<T>() {

			public T handleResponse(HttpResponse theResponse) throws ClientProtocolException, IOException {
				response[0] = theResponse;
				return responseHandler.handleResponse(theResponse);
			}
		}, intercept(context));
	}

	private HttpContext intercept(HttpContext context) {
		if (contextInterceptors == null) {
			return context;
		}
		for (HttpContextInterceptor interceptor : contextInterceptors) {
			context = interceptor.intercept(this, context);
		}
		return context;
	}

}