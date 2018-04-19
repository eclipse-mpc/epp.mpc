/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public abstract class RequestTemplate<T> {
	private HttpClientTransport transport;

	private ServiceReference<HttpClientTransport> transportReference;

	public RequestTemplate() {
	}

	protected RequestTemplate(HttpClientTransport transport) {
		this.transport = transport;
	}

	public String getUserAgent() {
		return HttpClientTransport.USER_AGENT;
	}

	public T execute(URI uri) throws ClientProtocolException, IOException {
		if (transport != null) {
			return executeImpl(uri);
		}
		try {
			acquireTransport();
			return executeImpl(uri);
		} finally {
			releaseTransport();
		}
	}

	private void releaseTransport() {
		ServiceReference<?> reference = transportReference;
		if (reference != null) {
			transport = null;
			transportReference = null;
			Bundle bundle = reference.getBundle();
			if (bundle != null) {
				bundle.getBundleContext().ungetService(reference);
			}
		}
	}

	private void acquireTransport() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		BundleContext bundleContext = bundle.getBundleContext();
		ServiceReference<HttpClientTransport> serviceReference = bundleContext
				.getServiceReference(HttpClientTransport.class);
		if (serviceReference == null) {
			throw new IllegalStateException();
		}
		HttpClientTransport transport = bundleContext.getService(serviceReference);
		if (transport == null) {
			throw new IllegalStateException();
		}
		this.transportReference = serviceReference;
		this.transport = transport;
	}

	protected T executeImpl(URI uri) throws ClientProtocolException, IOException {
		Request request = createRequest(uri);
		request = configureRequest(request, uri);
		Response response = transport.execute(request, uri);
		return handleResponse(response);
	}

	protected abstract Request createRequest(URI uri);

	protected Request configureRequest(Request request, URI uri) {
		request.setHeader(HttpHeaders.USER_AGENT, getUserAgent());
		return transport.configureRequest(request, uri);
	}

	protected T handleResponse(Response response) throws ClientProtocolException, IOException {
		return response.handleResponse(response1 -> {
			HttpEntity entity = null;
			try {
				final StatusLine statusLine = response1.getStatusLine();
				entity = response1.getEntity();
				handleResponseStatus(statusLine.getStatusCode(), statusLine.getReasonPhrase());
				return handleResponseEntity(entity);
			} finally {
				closeResponse(response1, entity);
			}
		});
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
