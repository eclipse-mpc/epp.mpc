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
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
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
		return transport.configureRequest(request, uri);
	}

	protected T handleResponse(Response response) throws ClientProtocolException, IOException {
		return response.handleResponse(new ResponseHandler<T>() {

			public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				final StatusLine statusLine = response.getStatusLine();
				final HttpEntity entity = response.getEntity();
				handleResponseStatus(statusLine.getStatusCode(), statusLine.getReasonPhrase());
				return handleResponseEntity(entity);
			}
		});
	}

	protected T handleResponseEntity(HttpEntity entity) throws IOException {
		if (entity == null) {
			return handleEmptyResponse();
		}
		return handleResponseStream(entity.getContent());
	}

	protected abstract T handleResponseStream(InputStream content) throws IOException;

	protected T handleEmptyResponse() {
		return null;
	}

	protected void handleResponseStatus(int statusCode, String reasonPhrase) throws HttpResponseException {
		if (statusCode >= 300) {
			throw new HttpResponseException(statusCode, reasonPhrase);
		}
	}
}
