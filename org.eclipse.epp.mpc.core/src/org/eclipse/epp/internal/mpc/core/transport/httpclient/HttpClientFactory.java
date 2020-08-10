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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.userstorage.internal.StorageProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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

		CredentialsProvider cacheProvider = oldContext == null ? null
				: oldContext.getCredentialsCacheProvider();
		if (cacheProvider == null) {
			cacheProvider = createCredentialsCacheProvider();
		}
		CredentialsProvider initialCredentialsProvider = oldContext == null ? null
				: oldContext.getInitialCredentialsProvider();
		if (initialCredentialsProvider == null) {
			initialCredentialsProvider = createCredentialsProvider();
		}
		CredentialsProvider credentialsProvider = initialCredentialsProvider;
		if (credentialsProvider != null) {
			credentialsProvider = customizeCredentialsProvider(clientBuilder, credentialsProvider, cacheProvider);
		}

		clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		clientBuilder.setDefaultCookieStore(cookieStore);

		clientBuilder = customizeBuilder(clientBuilder);

		return new HttpServiceContext(clientBuilder.build(), cookieStore, credentialsProvider,
				initialCredentialsProvider, cacheProvider);
	}

	protected CredentialsProvider createCredentialsProvider() {
		return new SystemCredentialsProvider();
	}

	protected CredentialsProvider createCredentialsCacheProvider() {
		return new CacheCredentialsProvider();
	}

	protected CookieStore createCookieStore() {
		return new BasicCookieStore();
	}

	private CredentialsProvider customizeCredentialsProvider(HttpClientBuilder clientBuilder,
			CredentialsProvider credentialsProvider, CredentialsProvider cacheProvider) {
		//TODO we should handle configured proxy passwords and dialogs to prompt for unknown credentials on our own...
		credentialsProvider = customizeCredentialsProvider(credentialsProvider);

		if (cacheProvider != null) {
			credentialsProvider = new ChainedCredentialsProvider(cacheProvider, credentialsProvider);

			clientBuilder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> context
					.setAttribute(CacheCredentialsAuthenticationStrategy.CREDENTIALS_CACHE_ATTRIBUTE, cacheProvider));
		}
		credentialsProvider = new SynchronizedCredentialsProvider(credentialsProvider);

		return credentialsProvider;
	}

	protected HttpClientBuilder builder() {
		HttpClientBuilder builder = HttpClientBuilder.create();

		builder.setMaxConnPerRoute(100).setMaxConnTotal(200);
		setClientDefaultTimeouts(builder);

		builder.setTargetAuthenticationStrategy(
				new CacheCredentialsAuthenticationStrategy.Target(TargetAuthenticationStrategy.INSTANCE));
		builder.setProxyAuthenticationStrategy(
				new CacheCredentialsAuthenticationStrategy.Proxy(ProxyAuthenticationStrategy.INSTANCE));

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
				.setSoTimeout(readTimeout)
				.setTcpNoDelay(true)//Disable Nagle - see https://en.wikipedia.org/wiki/Nagle%27s_algorithm#Negative_effect_on_larger_writes
				//.setSoLinger(0)
				//TODO is it safe to set this to 0? This will forcefully terminate sockets on close instead of waiting for graceful close
				//See http://docs.oracle.com/javase/6/docs/api/java/net/SocketOptions.html?is-external=true#SO_LINGER
				//and https://issues.apache.org/jira/browse/HTTPCLIENT-1497
				.build();
		RequestConfig defaultRequestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
				.setSocketTimeout(readTimeout)
				.setConnectTimeout(connectTimeout)
				.setConnectionRequestTimeout(connectionRequestTimeout)
				.build();
		builder.setDefaultSocketConfig(defaultSocketConfig);
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

	private CredentialsProvider customizeCredentialsProvider(CredentialsProvider credentialsProvider) {
		CredentialsProvider customizedCredentialsProvider = credentialsProvider;
		for (HttpClientCustomizer customizer : this.customizers) {
			customizedCredentialsProvider = customizeCredentialsProvider(customizer, customizedCredentialsProvider);
		}
		return customizedCredentialsProvider;
	}

	private static CredentialsProvider customizeCredentialsProvider(HttpClientCustomizer customizer,
			CredentialsProvider credentialsProvider) {
		if (customizer == null) {
			return credentialsProvider;
		}
		CredentialsProvider customCredentialsProvider = customizer.customizeCredentialsProvider(credentialsProvider);
		return customCredentialsProvider == null ? credentialsProvider : customCredentialsProvider;
	}

	private static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(HttpClientTransport.class).getBundleContext();
	}

	private static Collection<ServiceReference<HttpClientCustomizer>> getClientBuilderCustomizers(
			BundleContext context) {
		Collection<ServiceReference<HttpClientCustomizer>> serviceReferences;
		try {
			serviceReferences = context.getServiceReferences(HttpClientCustomizer.class,
					/*TransportFactory.computeDisabledTransportsFilter()*/null);
		} catch (InvalidSyntaxException e) {
			MarketplaceClientCore.error(e);
			serviceReferences = Collections.emptySet();
		}
		return serviceReferences;
	}
}
