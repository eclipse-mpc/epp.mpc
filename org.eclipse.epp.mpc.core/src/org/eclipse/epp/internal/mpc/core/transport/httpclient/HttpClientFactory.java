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

import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.userstorage.internal.StorageProperties;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(name = "org.eclipse.epp.mpc.core.http.client.factory", service = { HttpClientFactory.class })
public class HttpClientFactory {

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.STATIC, fieldOption = FieldOption.REPLACE)
	private List<HttpClientCustomizer> customizers;

	public List<HttpClientCustomizer> getCustomizers() {
		return customizers;
	}

	public void setCustomizers(List<HttpClientCustomizer> customizers) {
		this.customizers = customizers;
	}

	public HttpServiceContext build() {
		return build(null);
	}

	public HttpServiceContext build(HttpServiceContext oldContext) {
		HttpClientBuilder clientBuilder = builder();

		CookieStore cookieStore = oldContext == null ? null : oldContext.getCookieStore();
		if (cookieStore == null) {
			cookieStore = createCookieStore();
		}

		CredentialsStore cacheProvider = oldContext == null ? null
				: oldContext.getCredentialsCacheProvider();
		if (cacheProvider == null) {
			cacheProvider = createCredentialsCacheProvider();
		}
		CredentialsStore initialCredentialsProvider = oldContext == null ? null : oldContext.getInitialCredentialsProvider();
		if (initialCredentialsProvider == null) {
			initialCredentialsProvider = createCredentialsProvider();
		}
		CredentialsStore credentialsProvider = initialCredentialsProvider;
		if (credentialsProvider != null) {
			credentialsProvider = customizeCredentialsProvider(clientBuilder, credentialsProvider, cacheProvider);
		}

		clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		clientBuilder.setDefaultCookieStore(cookieStore);

		clientBuilder = customizeBuilder(clientBuilder);

		return new HttpServiceContext(clientBuilder.build(), cookieStore, credentialsProvider, initialCredentialsProvider,
				cacheProvider);
	}

	protected CredentialsStore createCredentialsProvider() {
		return new SystemCredentialsProvider();
	}

	protected CredentialsStore createCredentialsCacheProvider() {
		return new CacheCredentialsProvider();
	}

	protected CookieStore createCookieStore() {
		return new BasicCookieStore();
	}

	private CredentialsStore customizeCredentialsProvider(HttpClientBuilder clientBuilder,
			CredentialsStore credentialsProvider, CredentialsStore cacheProvider) {
		//TODO we should handle configured proxy passwords and dialogs to prompt for unknown credentials on our own...
		credentialsProvider = customizeCredentialsProvider(credentialsProvider);

		if (cacheProvider != null) {
			credentialsProvider = new ChainedCredentialsProvider(cacheProvider, credentialsProvider);

			clientBuilder
			.addRequestInterceptorFirst((HttpRequestInterceptor) (request, entityDetails, context) -> context
					.setAttribute(CacheCredentialsAuthenticationStrategy.CREDENTIALS_CACHE_ATTRIBUTE, cacheProvider));
		}
		credentialsProvider = new SynchronizedCredentialsProvider(credentialsProvider);

		return credentialsProvider;
	}

	protected HttpClientBuilder builder() {
		HttpClientBuilder builder = HttpClientBuilder.create();
		
		PoolingHttpClientConnectionManager connManager = null;
		try {
			SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(SSLContext.getDefault());
			connManager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslFactory).build();

		} catch (Exception defaultProcess) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			connManager = new PoolingHttpClientConnectionManager();
		}

		connManager.setDefaultMaxPerRoute(100);
		connManager.setMaxTotal(200);
		builder.setConnectionManager(connManager);
		setClientDefaultTimeouts(builder);

		builder.addResponseInterceptorLast(new CacheCredentialsAuthenticationStrategy());

		builder.setUserAgent(HttpClientTransport.USER_AGENT);

		return builder;
	}

	private static void setClientDefaultTimeouts(HttpClientBuilder builder) {
		@SuppressWarnings("restriction")
		int connectTimeoutUssDefault = StorageProperties.getProperty(StorageProperties.CONNECT_TIMEOUT,
				HttpClientTransport.DEFAULT_CONNECT_TIMEOUT);
		@SuppressWarnings("restriction")
		int readTimeoutUssDefault = StorageProperties.getProperty(StorageProperties.SOCKET_TIMEOUT,
				HttpClientTransport.DEFAULT_READ_TIMEOUT);

		int connectTimeout = getTimeoutValue(HttpClientTransport.CONNECT_TIMEOUT_PROPERTY, connectTimeoutUssDefault);
		int readTimeout = getTimeoutValue(HttpClientTransport.READ_TIMEOUT_PROPERTY, readTimeoutUssDefault);

		int connectionRequestTimeout = getTimeoutValue(HttpClientTransport.CONNECTION_REQUEST_TIMEOUT_PROPERTY,
				HttpClientTransport.DEFAULT_CONNECTION_REQUEST_TIMEOUT);

		SocketConfig defaultSocketConfig = SocketConfig.copy(SocketConfig.DEFAULT)
				.setSoTimeout(Timeout.ofMilliseconds(readTimeout))
				.setTcpNoDelay(true)//Disable Nagle - see https://en.wikipedia.org/wiki/Nagle%27s_algorithm#Negative_effect_on_larger_writes
				//.setSoLinger(0)
				//TODO is it safe to set this to 0? This will forcefully terminate sockets on close instead of waiting for graceful close
				//See http://docs.oracle.com/javase/6/docs/api/java/net/SocketOptions.html?is-external=true#SO_LINGER
				//and https://issues.apache.org/jira/browse/HTTPCLIENT-1497
				.build();
		RequestConfig defaultRequestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
				.setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
				.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeout))
				.build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setDefaultSocketConfig(defaultSocketConfig);
		builder.setConnectionManager(connManager);
		builder.setDefaultRequestConfig(defaultRequestConfig);
	}

	private static int getTimeoutValue(String property, int defaultValue) {
		String propertyValue = FrameworkUtil.getBundle(HttpClientTransport.class)
				.getBundleContext()
				.getProperty(property);
		if (propertyValue == null || "".equals(propertyValue)) { //$NON-NLS-1$
			return defaultValue;
		}
		try {
			return Integer.parseInt(propertyValue);
		} catch (NumberFormatException ex) {
			//TODO log
			return defaultValue;
		}
	}

	protected HttpClientBuilder customizeBuilder(HttpClientBuilder builder) {
		HttpClientBuilder customBuilder = builder;
		for (HttpClientCustomizer customizer : this.customizers) {
			customBuilder = customizeBuilder(customizer, customBuilder);
		}
		return customBuilder;
	}

	private static HttpClientBuilder customizeBuilder(HttpClientCustomizer customizer, HttpClientBuilder builder) {
		if (customizer == null) {
			return builder;
		}
		HttpClientBuilder customBuilder = customizer.customizeBuilder(builder);
		return customBuilder == null ? builder : customBuilder;
	}

	private CredentialsStore customizeCredentialsProvider(CredentialsStore credentialsProvider) {
		CredentialsStore customizedCredentialsProvider = credentialsProvider;
		for (HttpClientCustomizer customizer : this.customizers) {
			customizedCredentialsProvider = customizeCredentialsProvider(customizer, customizedCredentialsProvider);
		}
		return customizedCredentialsProvider;
	}

	private static CredentialsStore customizeCredentialsProvider(HttpClientCustomizer customizer,
			CredentialsStore credentialsProvider) {
		if (customizer == null) {
			return credentialsProvider;
		}
		CredentialsStore customCredentialsProvider = customizer.customizeCredentialsProvider(credentialsProvider);
		return customCredentialsProvider == null ? credentialsProvider : customCredentialsProvider;
	}
}
