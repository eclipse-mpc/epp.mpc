/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.rest.client.internal.resteasy;

import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.HostnameVerificationPolicy;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public interface IResteasyClientBuilder {

	ResteasyClient build();

	IResteasyClientBuilder providerFactory(ResteasyProviderFactory providerFactory);

	IResteasyClientBuilder connectionTTL(long ttl, TimeUnit unit);

	IResteasyClientBuilder readTimeout(long timeout, TimeUnit unit);

	IResteasyClientBuilder connectTimeout(long timeout, TimeUnit unit);

	IResteasyClientBuilder maxPooledPerRoute(int maxPooledPerRoute);

	IResteasyClientBuilder connectionCheckoutTimeout(long timeout, TimeUnit unit);

	IResteasyClientBuilder connectionPoolSize(int connectionPoolSize);

	IResteasyClientBuilder responseBufferSize(int size);

	IResteasyClientBuilder disableTrustManager();

	IResteasyClientBuilder hostnameVerification(HostnameVerificationPolicy policy);

	IResteasyClientBuilder httpEngine(ClientHttpEngine httpEngine);

	IResteasyClientBuilder useAsyncHttpEngine();

	IResteasyClientBuilder sslContext(SSLContext sslContext);

	IResteasyClientBuilder trustStore(KeyStore truststore);

	IResteasyClientBuilder keyStore(KeyStore keyStore, String password);

	IResteasyClientBuilder keyStore(KeyStore keyStore, char[] password);

	IResteasyClientBuilder property(String name, Object value);

	IResteasyClientBuilder sniHostNames(String... sniHostNames);

	IResteasyClientBuilder defaultProxy(String hostname);

	IResteasyClientBuilder defaultProxy(String hostname, int port);

	IResteasyClientBuilder defaultProxy(String hostname, int port, final String scheme);

	IResteasyClientBuilder hostnameVerifier(HostnameVerifier verifier);

	IResteasyClientBuilder register(Class<?> componentClass);

	IResteasyClientBuilder register(Class<?> componentClass, int priority);

	IResteasyClientBuilder register(Class<?> componentClass, Class<?>... contracts);

	IResteasyClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts);

	IResteasyClientBuilder register(Object component);

	IResteasyClientBuilder register(Object component, int priority);

	IResteasyClientBuilder register(Object component, Class<?>... contracts);

	IResteasyClientBuilder register(Object component, Map<Class<?>, Integer> contracts);

	IResteasyClientBuilder withConfig(Configuration config);

	IResteasyClientBuilder executorService(ExecutorService executorService);

	IResteasyClientBuilder executorService(ExecutorService executorService, boolean cleanupExecutor);

	IResteasyClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);

	ResteasyProviderFactory getProviderFactory();

	Configuration getConfiguration();

	long getConnectionTTL(TimeUnit unit);

	int getMaxPooledPerRoute();

	long getConnectionCheckoutTimeout(TimeUnit unit);

	int getConnectionPoolSize();

	int getResponseBufferSize();

	boolean isTrustManagerDisabled();

	boolean isTrustSelfSignedCertificates();

	void setIsTrustSelfSignedCertificates(boolean b);

	HostnameVerificationPolicy getHostnameVerification();

	ClientHttpEngine getHttpEngine();

	boolean isUseAsyncHttpEngine();

	List<String> getSniHostNames();

	String getDefaultProxyHostname();

	int getDefaultProxyPort();

	String getDefaultProxyScheme();

	long getReadTimeout(TimeUnit unit);

	long getConnectionTimeout(TimeUnit unit);

	SSLContext getSSLContext();

	KeyStore getKeyStore();

	String getKeyStorePassword();

	KeyStore getTrustStore();

	HostnameVerifier getHostnameVerifier();

	ResteasyClientBuilder enableCookieManagement();

	boolean isCookieManagementEnabled();

	ResteasyClientBuilder disableAutomaticRetries();

	boolean isDisableAutomaticRetries();

}