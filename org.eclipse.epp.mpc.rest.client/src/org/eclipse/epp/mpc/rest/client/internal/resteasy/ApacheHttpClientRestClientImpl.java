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

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.ws.rs.client.WebTarget;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.mpc.rest.client.IRestClient;
import org.eclipse.epp.mpc.rest.client.internal.util.RuntimeCoreException;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;

public class ApacheHttpClientRestClientImpl<E> implements IRestClient<E> {

	public static final String CONTEXT_PROVIDER_KEY = ApacheHttpClientRestClientImpl.class + ".httpContextProvider";

	private final HttpContext defaultContext;

	private final WebTarget webTarget;

	private final Function<IProgressMonitor, E> monitoredEndpointSupplier;

	private final E endpoint;

	public ApacheHttpClientRestClientImpl(Class<E> endpointClass, WebTarget target, HttpContext defaultContext) {
		this.defaultContext = defaultContext == null ? new BasicHttpContext() : defaultContext;
		this.endpoint = ProxyBuilder.builder(endpointClass, target).build();
		this.webTarget = target;
		this.monitoredEndpointSupplier = createMonitoredEndpointSupplier(endpointClass, target, this.defaultContext,
				this.endpoint);
	}

	private static <E> Function<IProgressMonitor, E> createMonitoredEndpointSupplier(Class<E> endpointClass,
			WebTarget target, HttpContext defaultContext, E endpoint) {
		HttpContextInjector contextProvider = (HttpContextInjector) target.getConfiguration()
				.getProperty(CONTEXT_PROVIDER_KEY);

		if (contextProvider == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Class<? extends E> proxyClass = (Class<? extends E>) Proxy.getProxyClass(endpointClass.getClassLoader(),
				endpointClass);
		Constructor<? extends E> constructor;
		try {
			constructor = proxyClass.getConstructor(InvocationHandler.class);
		} catch (NoSuchMethodException e) {
			throw new InternalError(e.toString(), e);
		}
		InvocationHandler endpointInvocationHandler = Proxy.getInvocationHandler(endpoint);

		return monitor -> {
			InvocationHandler monitoredInvocationHandler = new MonitoredInvocationHandler(endpointInvocationHandler,
					contextProvider, monitor, defaultContext);
			try {
				return constructor.newInstance(monitoredInvocationHandler);
			} catch (IllegalAccessException | InstantiationException e) {
				throw new InternalError(e.toString(), e);
			} catch (InvocationTargetException e) {
				Throwable t = e.getCause();
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				} else {
					throw new InternalError(t.toString(), t);
				}
			}
		};
	}

	@Override
	public URI getBaseUri() {
		return webTarget.getUri();
	}

	@Override
	public E call() {
		return endpoint;
	}

	@Override
	public E call(IProgressMonitor monitor) throws UnsupportedOperationException, RuntimeCoreException {
		if (monitoredEndpointSupplier == null) {
			throw new UnsupportedOperationException();
		}
		return monitoredEndpointSupplier.apply(monitor);
	}

	private static final class MonitoredInvocationHandler implements InvocationHandler {

		private final InvocationHandler delegate;

		private final AtomicReference<IProgressMonitor> monitorReference;

		private final HttpContextInjector contextProvider;

		private final HttpContext parentContext;

		public MonitoredInvocationHandler(InvocationHandler delegate, HttpContextInjector contextProvider,
				IProgressMonitor monitor, HttpContext parentContext) {
			requireNonNull(delegate, "delegate");
			requireNonNull(delegate, "contextProvider");
			requireNonNull(monitor, "monitor");
			requireNonNull(monitor, "parentContext");
			this.delegate = delegate;
			this.contextProvider = contextProvider;
			this.monitorReference = new AtomicReference<>(monitor);
			this.parentContext = parentContext;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			IProgressMonitor monitor = monitorReference.getAndSet(null);
			if (monitor == null) {
				//TODO exception
			}

			HttpContext monitoredContext = new BasicHttpContext(parentContext);
			monitoredContext.setAttribute(""/*TODO WIP*/, monitor);

			Callable<?> delegateInvoke = () -> {
				try {
					return delegate.invoke(proxy, method, args);
				} catch (Exception ex) {
					throw ex;
				} catch (Throwable t) {
					throw (Error) t;
				}
			};
			return contextProvider.withContext(monitoredContext, delegateInvoke);
		}
	}
}
