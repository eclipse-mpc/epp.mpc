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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

public abstract class RequestTemplate<T> {

	public String getUserAgent() {
		return HttpClientTransport.USER_AGENT;
	}

	public T execute(HttpClientService client, URI uri) throws ClientProtocolException, IOException {
		HttpUriRequest request = createRequest(uri);
		request = configureRequest(client, request);
		HttpResponse response = client.execute(request);
		return handleResponse(response);
	}

	protected abstract HttpUriRequest createRequest(URI uri);

	protected HttpUriRequest configureRequest(HttpClientService client, HttpUriRequest request) {
		request.setHeader(HttpHeaders.USER_AGENT, getUserAgent());
		return client.configureRequest(request);
	}

	protected T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
		HttpEntity entity = null;
		try {
			final StatusLine statusLine = response.getStatusLine();
			entity = response.getEntity();
			handleResponseStatus(statusLine.getStatusCode(), statusLine.getReasonPhrase());
			return handleResponseEntity(entity);
		} finally {
			closeResponse(response, entity);
		}
	}

	protected T handleResponseEntity(HttpEntity entity) throws IOException {
		if (entity == null) {
			return handleEmptyResponse();
		}
		Charset charset = null;
		ContentType contentType = ContentType.get(entity);
		if (contentType != null) {
			charset = contentType.getCharset();
		}

		return handleResponseStream(entity.getContent(), charset);
	}

	protected abstract T handleResponseStream(InputStream content, Charset charset) throws IOException;

	protected T handleEmptyResponse() {
		return null;
	}

	protected void handleResponseStatus(int statusCode, String reasonPhrase)
			throws IllegalStateException, IOException {
		if (statusCode >= 300) {
			if (statusCode == 404) {
				throw new FileNotFoundException(reasonPhrase);
			}
			throw new HttpResponseException(statusCode, reasonPhrase);
		}
	}

	private static void closeResponse(HttpResponse response, final HttpEntity entity) throws IOException {
		if (entity != null) {
			EntityUtils.consumeQuietly(entity);
		}
		if (response instanceof CloseableHttpResponse) {
			CloseableHttpResponse closeable = (CloseableHttpResponse) response;
			closeable.close();
		}
	}
}
