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
package org.eclipse.epp.mpc.rest.client.internal.httpclient;

import java.util.Collection;
import java.util.Collections;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope = ServiceScope.PROTOTYPE)
public class HttpClientFactory {

	private CredentialsProvider credentialsProvider;

	private CookieStore cookieStore;

//	private HttpClient client;

	public HttpClient build() {
		HttpClientBuilder clientBuilder = createClientBuilder();

		if (cookieStore == null) {
			cookieStore = new BasicCookieStore();
		}

		if (credentialsProvider == null) {
			credentialsProvider = createCredentialsProvider(clientBuilder);
		}

		clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		clientBuilder.setDefaultCookieStore(cookieStore);
//		client = clientBuilder.build();

		return clientBuilder.build();
	}

	private static CredentialsProvider createCredentialsProvider(HttpClientBuilder clientBuilder) {
		//TODO we should handle configured proxy passwords and dialogs to prompt for unknown credentials on our own...
		CredentialsProvider credentialsProvider = new SystemCredentialsProvider();
		credentialsProvider = customizeCredentialsProvider(credentialsProvider);

		final CacheCredentialsProvider cacheProvider = new CacheCredentialsProvider();
		credentialsProvider = new ChainedCredentialsProvider(cacheProvider, credentialsProvider);
		credentialsProvider = new SynchronizedCredentialsProvider(credentialsProvider);

		clientBuilder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> context
				.setAttribute(CacheCredentialsAuthenticationStrategy.CREDENTIALS_CACHE_ATTRIBUTE, cacheProvider));

		return credentialsProvider;
	}

	private static HttpClientBuilder createClientBuilder() {
		HttpClientBuilder builder = HttpClientBuilder.create();

		builder.setMaxConnPerRoute(100).setMaxConnTotal(200);
		setClientTimeouts(builder);

		builder.setTargetAuthenticationStrategy(
				new CacheCredentialsAuthenticationStrategy.Target(TargetAuthenticationStrategy.INSTANCE));
		builder.setProxyAuthenticationStrategy(
				new CacheCredentialsAuthenticationStrategy.Proxy(ProxyAuthenticationStrategy.INSTANCE));

		builder = customizeBuilder(builder);

		//TODO WIP user agent
		//builder.setUserAgent(HttpClientTransport.USER_AGENT);

		return builder;
	}

	private static void setClientTimeouts(HttpClientBuilder builder) {
		//TODO WIP timeouts
		//		@SuppressWarnings("restriction")
//		int connectTimeoutUssDefault = StorageProperties
//		.getProperty(StorageProperties.CONNECT_TIMEOUT, HttpClientTransport.DEFAULT_CONNECT_TIMEOUT);
//		@SuppressWarnings("restriction")
//		int readTimeoutUssDefault = StorageProperties.getProperty(StorageProperties.SOCKET_TIMEOUT,
//				HttpClientTransport.DEFAULT_READ_TIMEOUT);
//
//		int connectTimeout = getTimeoutValue(HttpClientTransport.CONNECT_TIMEOUT_PROPERTY, connectTimeoutUssDefault);
//		int readTimeout = getTimeoutValue(HttpClientTransport.READ_TIMEOUT_PROPERTY, readTimeoutUssDefault);
//
//		int connectionRequestTimeout = getTimeoutValue(HttpClientTransport.CONNECTION_REQUEST_TIMEOUT_PROPERTY,
//				HttpClientTransport.DEFAULT_CONNECTION_REQUEST_TIMEOUT);
//
//		SocketConfig defaultSocketConfig = SocketConfig.copy(SocketConfig.DEFAULT)
//				.setSoTimeout(readTimeout)
//				.setTcpNoDelay(true)//Disable Nagle - see https://en.wikipedia.org/wiki/Nagle%27s_algorithm#Negative_effect_on_larger_writes
//				//.setSoLinger(0)
//				//TODO is it safe to set this to 0? This will forcefully terminate sockets on close instead of waiting for graceful close
//				//See http://docs.oracle.com/javase/6/docs/api/java/net/SocketOptions.html?is-external=true#SO_LINGER
//				//and https://issues.apache.org/jira/browse/HTTPCLIENT-1497
//				.build();
//		RequestConfig defaultRequestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
//				.setSocketTimeout(readTimeout)
//				.setConnectTimeout(connectTimeout)
//				.setConnectionRequestTimeout(connectionRequestTimeout)
//				.build();
//		builder.setDefaultSocketConfig(defaultSocketConfig);
//		builder.setDefaultRequestConfig(defaultRequestConfig);
	}

	private static int getTimeoutValue(String property, int defaultValue) {
		String propertyValue = FrameworkUtil.getBundle(HttpClientFactory.class)
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

	private static HttpClientBuilder customizeBuilder(HttpClientBuilder builder) {
		BundleContext context = getBundleContext();
		Collection<ServiceReference<HttpClientCustomizer>> serviceReferences = getClientBuilderCustomizers(context);
		HttpClientBuilder customBuilder = builder;
		for (ServiceReference<HttpClientCustomizer> reference : serviceReferences) {
			try {
				customBuilder = customizeBuilder(context.getService(reference), customBuilder);
			} finally {
				context.ungetService(reference);
			}
		}
		return customBuilder;
	}

	private static HttpClientBuilder customizeBuilder(HttpClientCustomizer service, HttpClientBuilder builder) {
		if (service == null) {
			return builder;
		}
		HttpClientBuilder customBuilder = service.customizeBuilder(builder);
		return customBuilder == null ? builder : customBuilder;
	}

	private static CredentialsProvider customizeCredentialsProvider(CredentialsProvider credentialsProvider) {
		BundleContext context = getBundleContext();
		Collection<ServiceReference<HttpClientCustomizer>> serviceReferences = getClientBuilderCustomizers(context);
		CredentialsProvider customizedCredentialsProvider = credentialsProvider;
		for (ServiceReference<HttpClientCustomizer> reference : serviceReferences) {
			try {
				customizedCredentialsProvider = customizeCredentialsProvider(context.getService(reference),
						customizedCredentialsProvider);
			} finally {
				context.ungetService(reference);
			}
		}
		return customizedCredentialsProvider;
	}

	private static CredentialsProvider customizeCredentialsProvider(HttpClientCustomizer service,
			CredentialsProvider credentialsProvider) {
		if (service == null) {
			return credentialsProvider;
		}
		CredentialsProvider customCredentialsProvider = service.customizeCredentialsProvider(credentialsProvider);
		return customCredentialsProvider == null ? credentialsProvider : customCredentialsProvider;
	}

	private static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(HttpClientFactory.class).getBundleContext();
	}

	private static Collection<ServiceReference<HttpClientCustomizer>> getClientBuilderCustomizers(
			BundleContext context) {
		Collection<ServiceReference<HttpClientCustomizer>> serviceReferences;
		try {
			serviceReferences = context.getServiceReferences(HttpClientCustomizer.class,
					/*TransportFactory.computeDisabledTransportsFilter()*/null);
		} catch (InvalidSyntaxException e) {
			//TODO WIP log
			//MarketplaceClientCore.error(e);
			serviceReferences = Collections.emptySet();
		}
		return serviceReferences;
	}

	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public void setCookieStore(CookieStore cookieStore) {
		this.cookieStore = cookieStore;
	}

	public CookieStore getCookieStore() {
		return cookieStore;
	}
//
//	public Executor getExecutor() {
//		return executor;
//	}

//	public HttpClient getClient() {
//		return client;
//	}
}
