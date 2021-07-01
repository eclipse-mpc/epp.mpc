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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

@SuppressWarnings("restriction")
public class ResteasyClientBuilderImplExt extends ResteasyClientBuilderImpl implements IResteasyClientBuilder {

	@Override
	public ResteasyClientBuilderImplExt providerFactory(ResteasyProviderFactory providerFactory) {
		super.providerFactory(providerFactory);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt connectionTTL(long ttl, TimeUnit unit) {
		super.connectionTTL(ttl, unit);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt readTimeout(long timeout, TimeUnit unit) {
		super.readTimeout(timeout, unit);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt connectTimeout(long timeout, TimeUnit unit) {
		super.connectTimeout(timeout, unit);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt maxPooledPerRoute(int maxPooledPerRoute) {
		super.maxPooledPerRoute(maxPooledPerRoute);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt connectionCheckoutTimeout(long timeout, TimeUnit unit) {
		super.connectionCheckoutTimeout(timeout, unit);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt connectionPoolSize(int connectionPoolSize) {
		super.connectionPoolSize(connectionPoolSize);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt responseBufferSize(int size) {
		super.responseBufferSize(size);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt disableTrustManager() {
		super.disableTrustManager();
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt hostnameVerification(HostnameVerificationPolicy policy) {
		super.hostnameVerification(policy);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt httpEngine(ClientHttpEngine httpEngine) {
		super.httpEngine(httpEngine);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt useAsyncHttpEngine() {
		super.useAsyncHttpEngine();
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt sslContext(SSLContext sslContext) {
		super.sslContext(sslContext);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt trustStore(KeyStore truststore) {
		super.trustStore(truststore);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt keyStore(KeyStore keyStore, String password) {
		super.keyStore(keyStore, password);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt keyStore(KeyStore keyStore, char[] password) {
		super.keyStore(keyStore, password);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt sniHostNames(String... sniHostNames) {
		super.sniHostNames(sniHostNames);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt defaultProxy(String hostname) {
		super.defaultProxy(hostname);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt defaultProxy(String hostname, int port) {
		super.defaultProxy(hostname, port);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt defaultProxy(String hostname, int port, String scheme) {
		super.defaultProxy(hostname, port, scheme);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt hostnameVerifier(HostnameVerifier verifier) {
		super.hostnameVerifier(verifier);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Class<?> componentClass) {
		super.register(componentClass);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Class<?> componentClass, int priority) {
		super.register(componentClass, priority);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Class<?> componentClass, Class<?>... contracts) {
		super.register(componentClass, contracts);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
		super.register(componentClass, contracts);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Object component) {
		super.register(component);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Object component, int priority) {
		super.register(component, priority);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Object component, Class<?>... contracts) {
		super.register(component, contracts);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt register(Object component, Map<Class<?>, Integer> contracts) {
		super.register(component, contracts);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt withConfig(Configuration config) {
		super.withConfig(config);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt executorService(ExecutorService executorService) {
		super.executorService(executorService);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt executorService(ExecutorService executorService, boolean cleanupExecutor) {
		super.executorService(executorService, cleanupExecutor);
		return this;
	}

	@Override
	public ResteasyClientBuilderImplExt scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
		super.scheduledExecutorService(scheduledExecutorService);
		return this;
	}
}
