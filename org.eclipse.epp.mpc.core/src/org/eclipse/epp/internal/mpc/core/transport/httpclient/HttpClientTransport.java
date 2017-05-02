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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.util.UserAgentUtil;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;

@SuppressWarnings({ "restriction" })
public class HttpClientTransport implements ITransport {

	public static final String USER_AGENT;

	public static final String USER_AGENT_PROPERTY = HttpClientTransport.class.getPackage().getName() + ".userAgent"; //$NON-NLS-1$

	/**
	 * Maximum time between response packets before the socket closes
	 */
	public static final int DEFAULT_READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

	/**
	 * Maximum time to establish connection with server
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

	/**
	 * Maximum time to wait for an available connection from the connection manager
	 */
	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

	public static final String READ_TIMEOUT_PROPERTY = HttpClientTransport.class.getPackage().getName()
			+ ".readTimeout"; //$NON-NLS-1$

	public static final String CONNECT_TIMEOUT_PROPERTY = HttpClientTransport.class.getPackage().getName()
			+ ".connectTimeout"; //$NON-NLS-1$

	public static final String CONNECTION_REQUEST_TIMEOUT_PROPERTY = HttpClientTransport.class.getPackage().getName()
			+ ".connectionRequestTimeout"; //$NON-NLS-1$

	static {
		USER_AGENT = UserAgentUtil.computeUserAgent();
	}

	private final HttpClient client;

	private final Executor executor;

	public HttpClientTransport() {
		HttpClientFactory httpClientFactory = new HttpClientFactory();
		client = httpClientFactory.build();
		executor = httpClientFactory.getExecutor();
	}

	public HttpClient getClient() {
		return client;
	}

	public Executor getExecutor() {
		return executor;
	}
	protected Response execute(Request request, URI uri) throws ClientProtocolException, IOException {
		return HttpClientProxyUtil.proxyAuthentication(executor, uri).execute(request);
	}

	protected Request configureRequest(Request request, URI uri) {
		return request.viaProxy(HttpClientProxyUtil.getProxyHost(uri));
	}

	public InputStream stream(URI location, IProgressMonitor monitor)
			throws FileNotFoundException, ServiceUnavailableException, CoreException {
		try {
			return createStreamingRequest().execute(location);
		} catch (HttpResponseException e) {
			int statusCode = e.getStatusCode();
			switch (statusCode) {
			case 404:
				FileNotFoundException fnfe = new FileNotFoundException(e.getMessage());
				fnfe.initCause(e);
				throw fnfe;
			case 503:
				throw new ServiceUnavailableException(
						new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, e.getMessage(), e));
			default:
				throw new CoreException(MarketplaceClientCore.computeStatus(e, null));
			}
		} catch (IOException e) {
			throw new CoreException(MarketplaceClientCore.computeStatus(e, null));
		}
	}

	protected RequestTemplate<InputStream> createStreamingRequest() {
		return new RequestTemplate<InputStream>(this) {

			@Override
			protected Request createRequest(URI uri) {
				return Request.Get(uri);
			}

			@Override
			protected InputStream handleResponse(Response response) throws ClientProtocolException, IOException {
				HttpResponse returnResponse = response.returnResponse();
				HttpEntity entity = returnResponse.getEntity();
				StatusLine statusLine = returnResponse.getStatusLine();
				handleResponseStatus(statusLine.getStatusCode(), statusLine.getReasonPhrase(), entity);
				return handleResponseEntity(entity);
			}

			@Override
			protected InputStream handleResponseStream(InputStream content) throws IOException {
				return content;
			}

			@Override
			protected InputStream handleEmptyResponse() {
				return new ByteArrayInputStream(new byte[0]);
			}
		};
	}
}
