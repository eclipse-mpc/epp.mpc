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

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.epp.internal.mpc.core.util.ProxyHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(name = "org.eclipse.epp.mpc.core.http.client", service = { HttpClientService.class })
public class HttpClientService {

	private HttpClient client;

	private HttpServiceContext context;

	private volatile IProxyService proxyService;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, updated = "bindClientFactory", unbind = "unbindClientFactory")
	void bindClientFactory(HttpClientFactory factory) {
		context = factory.build(context);
		client = context.getClient();
	}

	void unbindClientFactory(HttpClientFactory factory) {
		//do nothing
	}

	@Reference(field = "proxyService", unbind = "unbindProxyService", policy = ReferencePolicy.DYNAMIC)
	void bindProxyService(IProxyService proxyService) {
		this.proxyService = proxyService;
	}

	void unbindProxyService(IProxyService proxyService) {
		if (this.proxyService == proxyService) {
			this.proxyService = null;
		}
	}

	public HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
		return execute(request, null);
	}

	public HttpResponse execute(HttpUriRequest request, HttpContext context)
			throws ClientProtocolException, IOException {
		HttpClientContext internalContext = context == null ? new HttpClientContext()
				: HttpClientContext.adapt(context);
		HttpUriRequest configuredRequest = configureRequestExecution(request, internalContext);
		return client.execute(configuredRequest, internalContext);
	}

	private HttpUriRequest configureRequestExecution(HttpUriRequest request, HttpClientContext context)
			throws IOException {
		final RequestConfig.Builder builder;
		RequestConfig requestConfig = context.getRequestConfig();

		if (requestConfig != null) {
			builder = RequestConfig.copy(requestConfig);
		} else if (request instanceof Configurable && ((Configurable) request).getConfig() != null) {
			builder = RequestConfig.copy(((Configurable) request).getConfig());
		} else if (client instanceof Configurable && ((Configurable) client).getConfig() != null) {
			builder = RequestConfig.copy(((Configurable) client).getConfig());
		} else {
			builder = RequestConfig.custom();
		}

		configureRequestExecution(request, context, builder);

		RequestConfig config = builder.build();
		if (request instanceof HttpRequestBase) {
			((HttpRequestBase) request).setConfig(config);
			return request;
		} else {
			return RequestBuilder.copy(request).setConfig(config).build();
		}
	}

	protected void configureRequestExecution(HttpUriRequest request, HttpClientContext context,
			RequestConfig.Builder builder) throws IOException {
		configureProxy(request.getURI(), context, builder);
	}

	private static HttpUriRequest setConfig(HttpUriRequest request, RequestConfig config) {
		if (request instanceof HttpRequestBase) {
			((HttpRequestBase) request).setConfig(config);
		} else {
			request = RequestBuilder.copy(request).setConfig(config).build();
		}
		return request;
	}

	public HttpUriRequest configureRequest(HttpUriRequest request) {
		if (client instanceof Configurable && ((Configurable) client).getConfig() != null) {
			return setConfig(request, ((Configurable) client).getConfig());
		}
		return request;
	}

	public HttpResponse configureAndExecute(HttpUriRequest request) throws ClientProtocolException, IOException {
		return configureAndExecute(request, null);
	}

	public HttpResponse configureAndExecute(HttpUriRequest request, HttpContext context)
			throws ClientProtocolException, IOException {
		return execute(configureRequest(request), context);
	}

	public HttpClient getClient() {
		return client;
	}

	public IProxyService getProxyService() {
		return proxyService;
	}

	private IProxyData getProxyData(URI uri) {
		return proxyService == null ? null : ProxyHelper.getProxyData(uri, proxyService);
	}

	private HttpHost getProxyHost(IProxyData proxy) {
		if (IProxyData.HTTPS_PROXY_TYPE.equals(proxy.getType()) || IProxyData.HTTP_PROXY_TYPE.equals(proxy.getType())) {
			return new HttpHost(proxy.getHost(), proxy.getPort());
		}
		//SOCKS proxies are handled by Java on the socket level
		return null;
	}

	private void configureProxy(URI uri, HttpClientContext context, RequestConfig.Builder builder) throws IOException {
		IProxyData proxy = getProxyData(uri);
		if (proxy != null) {
			builder.setProxy(getProxyHost(proxy));
			setProxyAuthentication(context, proxy);
		} else {
			builder.setProxy(null);
		}
	}

	private void setProxyAuthentication(HttpClientContext context, IProxyData proxy) throws IOException {
		String proxyUserID;
		HttpHost proxyHost;
		if ((proxyUserID = proxy.getUserId()) != null && (proxyHost = getProxyHost(proxy)) != null) {
			String domainUserID = NTLMDomainUtil.getNTLMUserName(proxyUserID);
			String password = proxy.getPassword();
			String domain = NTLMDomainUtil.getNTLMUserDomain(proxyUserID);
			if (domain != null || !proxyUserID.equals(domainUserID)) {
				String workstation = NTLMDomainUtil.getNTLMWorkstation();
				setAuth(context, new AuthScope(proxyHost, AuthScope.ANY_REALM, "ntlm"), //$NON-NLS-1$
						new NTCredentials(domainUserID, password, workstation, domain));
			} else {
				setAuth(context, new AuthScope(proxyHost, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME),
						new UsernamePasswordCredentials(proxyUserID, password));
			}
		}
	}

	private void setAuth(HttpClientContext clientContext, AuthScope authScope, Credentials credentials) {
		CredentialsProvider authStore = clientContext.getCredentialsProvider();
		if (authStore == null) {
			authStore = new BasicCredentialsProvider();
			authStore = new ChainedCredentialsProvider(authStore, this.context.getCredentialsProvider());
			clientContext.setCredentialsProvider(authStore);
		}
		authStore.setCredentials(authScope, credentials);
	}
}
