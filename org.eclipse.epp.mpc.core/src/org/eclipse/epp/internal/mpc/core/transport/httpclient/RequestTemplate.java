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

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;


public abstract class RequestTemplate<T> {

	public String getUserAgent() {
		return HttpClientTransport.USER_AGENT;
	}

	public T execute(HttpClientService client, URI uri)
			throws ClientProtocolException, IOException {
		ClassicHttpRequest request = createRequest(uri);
		request = configureRequest(client, request);
		ClassicHttpResponse response = client.execute(request);
		try {
			return handleResponse(response);
		} finally {
			response.close();
		}
	}

	protected abstract ClassicHttpRequest createRequest(URI uri);

	protected ClassicHttpRequest configureRequest(HttpClientService client, ClassicHttpRequest request) {
		request.setHeader(HttpHeaders.USER_AGENT, getUserAgent());
		return client.configureRequest(request);
	}

	protected T handleResponse(ClassicHttpResponse response) throws ClientProtocolException, IOException {
		HttpEntity entity = null;
		try {
			entity = response.getEntity();
			handleResponseStatus(response.getCode(), response.getReasonPhrase());
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
		ContentType contentType = ContentType.parse(entity.getContentType());

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

	private static void closeResponse(ClassicHttpResponse response, final HttpEntity entity) throws IOException {
		if (entity != null) {
			EntityUtils.consumeQuietly(entity);
		}
		response.close();
	}
}
