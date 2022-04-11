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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.util.UserAgentUtil;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(name = "org.eclipse.epp.mpc.core.transport.http", service = { HttpClientTransport.class,
		ITransport.class })
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

	private HttpClientService clientService;

	public HttpClient getClient() {
		return clientService.getClient();
	}

	@Reference
	public void bindHttpClientService(HttpClientService service) {
		this.clientService = service;
	}

	protected HttpRequest configureRequest(HttpUriRequest request) {
		return clientService.configureRequest(request);
	}

	@Override
	public InputStream stream(URI location, IProgressMonitor monitor)
			throws FileNotFoundException, ServiceUnavailableException, CoreException {
		try {
			return createStreamingRequest().execute(clientService, location, false);
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
		return new RequestTemplate<>() {

			@Override
			protected HttpUriRequest createRequest(URI uri) {
				return new HttpGet(uri);
			}

			@Override
			protected InputStream handleResponse(ClassicHttpResponse response)
					throws ClientProtocolException, IOException {
				HttpEntity entity = response.getEntity();
				handleResponseStatus(response.getCode(), response.getReasonPhrase());
				return handleResponseEntity(entity);
			}

			@Override
			protected InputStream handleResponseStream(InputStream content, Charset charset) throws IOException {
				return content;
			}

			@Override
			protected InputStream handleEmptyResponse() {
				return new ByteArrayInputStream(new byte[0]);
			}
		};
	}
}
