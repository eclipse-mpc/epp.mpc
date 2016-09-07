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
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
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
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.util.ProxyHelper;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.eclipse.userstorage.internal.StorageProperties;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

@SuppressWarnings({ "deprecation", "restriction" })
public class HttpClientTransport implements ITransport {

	public static final String USER_AGENT;

	public static final String USER_AGENT_PROPERTY = HttpClientTransport.class.getPackage().getName() + ".userAgent"; //$NON-NLS-1$

	private static final HttpClient CLIENT;

	private final CookieStore cookieStore = new org.apache.http.impl.client.BasicCookieStore();

	private final Executor executor = Executor.newInstance(CLIENT).cookieStore(cookieStore);

	static {
		USER_AGENT = initUserAgent();
		CLIENT = initHttpClient();
	}

	private static HttpClient initHttpClient() {
		boolean accessible = false;
		Field clientField = null;
		HttpClient client = null;
		try {
			clientField = Executor.class.getDeclaredField("CLIENT"); //$NON-NLS-1$
			accessible = clientField.isAccessible();
			clientField.setAccessible(true);
			client = (HttpClient) clientField.get(null);
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
		return client;
	}

	private static String initUserAgent() {
		Bundle mpcCoreBundle = FrameworkUtil.getBundle(HttpClientTransport.class);
		BundleContext context = mpcCoreBundle.getBundleContext();

		String version = getAgentVersion(mpcCoreBundle);
		String java = getAgentJava(context);
		String os = getAgentOS(context);
		String language = getProperty(context, "osgi.nl", "unknownLanguage");//$NON-NLS-1$//$NON-NLS-2$
		String agentDetail = getAgentDetail(context);

		String userAgent = MessageFormat.format("mpc/{0} (Java {1}; {2}; {3}) {4}", //$NON-NLS-1$
				/*{0}*/version, /*{1}*/java, /*{2}*/os, /*{3}*/language, /*{4}*/agentDetail);
		return userAgent;
	}

	private static String getAgentVersion(Bundle bundle) {
		String version;
		Version mpcCoreVersion = bundle.getVersion();
		version = MessageFormat.format("{0}.{1}.{2}", mpcCoreVersion.getMajor(), mpcCoreVersion.getMinor(), //$NON-NLS-1$
				mpcCoreVersion.getMicro());
		return version;
	}

	private static String getAgentJava(BundleContext context) {
		String java;
		String javaSpec = getProperty(context, "java.runtime.version", "unknownJava"); //$NON-NLS-1$ //$NON-NLS-2$
		String javaVendor = getProperty(context, "java.vendor", "unknownJavaVendor");//$NON-NLS-1$//$NON-NLS-2$
		java = MessageFormat.format("{0} {1}", javaSpec, javaVendor); //$NON-NLS-1$
		return java;
	}

	private static String getAgentOS(BundleContext context) {
		String os;
		String osName = getProperty(context, "org.osgi.framework.os.name", "unknownOS"); //$NON-NLS-1$ //$NON-NLS-2$
		String osVersion = getProperty(context, "org.osgi.framework.os.version", "unknownOSVersion"); //$NON-NLS-1$ //$NON-NLS-2$
		String osArch = getProperty(context, "org.osgi.framework.processor", "unknownArch");//$NON-NLS-1$//$NON-NLS-2$
		os = MessageFormat.format("{0} {1} {2}", osName, osVersion, osArch); //$NON-NLS-1$
		return os;
	}

	private static String getAgentDetail(BundleContext context) {
		String agentDetail;
		agentDetail = getProperty(context, USER_AGENT_PROPERTY, null);
		if (agentDetail == null) {
			String productId = getProperty(context, "eclipse.product", null); //$NON-NLS-1$
			String productVersion = getProperty(context, "eclipse.buildId", null); //$NON-NLS-1$
			String appId = getProperty(context, "eclipse.application", null); //$NON-NLS-1$
			if (productId == null || productVersion == null) {
				Map<String, String> defaultRequestMetaParameters = ServiceLocator
						.computeDefaultRequestMetaParameters();
				productId = getProperty(defaultRequestMetaParameters, DefaultMarketplaceService.META_PARAM_PRODUCT,
						"unknownProduct"); //$NON-NLS-1$
				productVersion = getProperty(defaultRequestMetaParameters,
						DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION, "unknownBuildId"); //$NON-NLS-1$
			}
			if (appId == null) {
				IProduct product = Platform.getProduct();
				if (product != null) {
					appId = product.getApplication();
				}
				if (appId == null) {
					appId = "unknownApp"; //$NON-NLS-1$
				}
			}
			agentDetail = MessageFormat.format("{0}/{1} ({2})", productId, productVersion, appId); //$NON-NLS-1$
		}
		return agentDetail;
	}

	private static String getProperty(BundleContext context, String key, String defaultValue) {
		String value = context.getProperty(key);
		if (value != null) {
			return value;
		}
		return defaultValue;
	}

	private static String getProperty(Map<?, String> properties, Object key, String defaultValue) {
		String value = properties.get(key);
		if (value != null) {
			return value;
		}
		return defaultValue;
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

	private static HttpClient createClient() {
		return HttpClientBuilder.create().setMaxConnPerRoute(100).setMaxConnTotal(200).build();
	}

	private static HttpClient wrapClient(final HttpClient client) {
		return new HttpClient() {

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
		ProxyHelper.installAuthenticator();
		return HttpClientProxyUtil.proxyAuthentication(executor, uri).execute(request);
	}

	protected Request configureRequest(Request request, URI uri) {
		return request.viaProxy(HttpClientProxyUtil.getProxyHost(uri))
				.staleConnectionCheck(true)
				.connectTimeout(StorageProperties.getProperty(StorageProperties.CONNECT_TIMEOUT, 120000))
				.socketTimeout(StorageProperties.getProperty(StorageProperties.SOCKET_TIMEOUT, 120000));
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
