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
import java.lang.reflect.Field;
import java.net.URI;
import java.text.MessageFormat;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.eclipse.userstorage.internal.StorageProperties;
import org.eclipse.userstorage.internal.util.ProxyUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

@SuppressWarnings({ "deprecation", "restriction" })
public class HttpClientTransport implements ITransport {

	public static final String USER_AGENT_ID;

	public static final String USER_AGENT_PROPERTY = HttpClientTransport.class.getPackage().getName() + ".userAgent"; //$NON-NLS-1$

	private static final org.apache.http.client.HttpClient CLIENT;

	private final Executor executor = Executor.newInstance(CLIENT);//WIP .cookieStore(cookieStore);

	static {
		Bundle mpcCoreBundle = FrameworkUtil.getBundle(HttpClientTransport.class);
		String version;
		if (mpcCoreBundle == null) {
			version = ""; //$NON-NLS-1$
		} else {
			Version mpcCoreVersion = mpcCoreBundle.getVersion();
			version = MessageFormat.format("/{0}.{1}.{2}", mpcCoreVersion.getMajor(), mpcCoreVersion.getMinor(), //$NON-NLS-1$
					mpcCoreVersion.getMicro());
		}
		USER_AGENT_ID = "mpc" + version; //$NON-NLS-1$

		boolean accessible = false;
		Field clientField = null;
		org.apache.http.client.HttpClient client = null;
		try {
			clientField = Executor.class.getDeclaredField("CLIENT"); //$NON-NLS-1$
			accessible = clientField.isAccessible();
			clientField.setAccessible(true);
			client = (org.apache.http.client.HttpClient) clientField.get(null);
		} catch (Throwable t) {
		} finally {
			if (clientField != null && !accessible) {
				try {
					clientField.setAccessible(false);
				} catch (SecurityException e) {
				}
			}
		}
		if (client == null) {
			client = createClient();
		}
		client = wrapClient(client);
		CLIENT = client;
	}

	private static final class ChainedSystemDefaultCredentialsProvider extends SystemDefaultCredentialsProvider {
		private final CredentialsProvider chainedCredentialsProvider;

		private ChainedSystemDefaultCredentialsProvider(CredentialsProvider configuredCredentialsProvider) {
			this.chainedCredentialsProvider = configuredCredentialsProvider;
		}

		@Override
		public void setCredentials(AuthScope authscope, Credentials credentials) {
			chainedCredentialsProvider.setCredentials(authscope, credentials);
		}

		@Override
		public Credentials getCredentials(AuthScope authscope) {
			Credentials credentials = chainedCredentialsProvider.getCredentials(authscope);
			if (credentials == null) {
				credentials = super.getCredentials(authscope);
			}
			return credentials;
		}

		@Override
		public void clear() {
			chainedCredentialsProvider.clear();
			super.clear();
		}
	}

	private static org.apache.http.client.HttpClient createClient() {
		return HttpClientBuilder.create().setMaxConnPerRoute(100).setMaxConnTotal(200).build();
	}

	private static org.apache.http.client.HttpClient wrapClient(final org.apache.http.client.HttpClient client) {
		return new org.apache.http.client.HttpClient() {

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
				return client.execute(request, setCredentialsProvider(context));
			}

			public HttpResponse execute(HttpHost target, HttpRequest request)
					throws IOException, ClientProtocolException {
				return client.execute(target, request);
			}

			public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context)
					throws IOException, ClientProtocolException {
				return client.execute(target, request, setCredentialsProvider(context));
			}

			public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler)
					throws IOException, ClientProtocolException {
				return client.execute(request, responseHandler);
			}

			public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler,
					HttpContext context) throws IOException, ClientProtocolException {
				return client.execute(request, responseHandler, setCredentialsProvider(context));
			}

			public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler)
					throws IOException, ClientProtocolException {
				return client.execute(target, request, responseHandler);
			}

			public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler,
					HttpContext context) throws IOException, ClientProtocolException {
				return client.execute(target, request, responseHandler, setCredentialsProvider(context));
			}

		};
	}

	private static HttpContext setCredentialsProvider(HttpContext context) {
		final CredentialsProvider configuredCredentialsProvider = (CredentialsProvider) context
				.getAttribute(HttpClientContext.CREDS_PROVIDER);
		if (configuredCredentialsProvider instanceof SystemDefaultCredentialsProvider) {
			return context;
		}
		CredentialsProvider credentialsProvider = createCredentialsProvider(configuredCredentialsProvider);
		context.setAttribute(HttpClientContext.CREDS_PROVIDER, credentialsProvider);
		return context;
	}

	private static final CredentialsProvider createCredentialsProvider(final CredentialsProvider delegate) {

		if (delegate == null) {
			return new SystemDefaultCredentialsProvider();
		}
		return new ChainedSystemDefaultCredentialsProvider(delegate);
	}

	protected Response execute(Request request, URI uri) throws ClientProtocolException, IOException {
		return ProxyUtil.proxyAuthentication(executor, uri).execute(request);
	}

	protected Request configureRequest(Request request, URI uri) {
		String userAgent = System.getProperty(USER_AGENT_PROPERTY, USER_AGENT_ID);

		return request.viaProxy(ProxyUtil.getProxyHost(uri))
				.staleConnectionCheck(true)
				.connectTimeout(StorageProperties.getProperty(StorageProperties.CONNECT_TIMEOUT, 120000))
				.socketTimeout(StorageProperties.getProperty(StorageProperties.SOCKET_TIMEOUT, 120000))
				.setHeader(HttpHeaders.USER_AGENT, userAgent);
	}

	public InputStream stream(URI location, IProgressMonitor monitor)
			throws FileNotFoundException, ServiceUnavailableException, CoreException {
		try {
			return new RequestTemplate<InputStream>(this) {

				@Override
				protected Request createRequest(URI uri) {
					return Request.Get(uri);
				}

				@Override
				protected InputStream handleResponse(Response response) throws ClientProtocolException, IOException {
					return handleResponseEntity(response.returnResponse().getEntity());
				}

				@Override
				protected InputStream handleResponseStream(InputStream content) throws IOException {
					return content;
				}

				@Override
				protected InputStream handleEmptyResponse() {
					return new ByteArrayInputStream(new byte[0]);
				}
			}.execute(location);
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
}
