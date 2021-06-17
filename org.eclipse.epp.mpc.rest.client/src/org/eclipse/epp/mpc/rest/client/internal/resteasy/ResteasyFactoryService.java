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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.ext.Providers;

import org.eclipse.epp.mpc.rest.client.internal.support.EmptyConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = ResteasyFactoryService.class, scope = ServiceScope.BUNDLE)
public class ResteasyFactoryService {

	private static final String[] SCAN_BUNDLES = { null, //self-bundle
			"org.jboss.resteasy.client", //$NON-NLS-1$
			"org.jboss.resteasy.core", //$NON-NLS-1$
			"org.jboss.resteasy.jaxb-provider", //$NON-NLS-1$
	"org.jboss.resteasy.jackson2-provider" }; //$NON-NLS-1$

	private Map<String, List<IServiceProvider<?>>> serviceProviders;

	private ResteasyProviderFactory clientInitializedResteasyProviderFactory;

	@Activate
	public void init(ComponentContext context) {
		Map<String, List<IServiceProvider<?>>> serviceProviders = new HashMap<>();
		Stream<Bundle> bundleStream = Arrays.stream(SCAN_BUNDLES).flatMap(this::findBundles);
		Bundle usingBundle = context.getUsingBundle();
		if (usingBundle != null) {
			bundleStream = Stream.concat(bundleStream, Stream.of(usingBundle));
		}
		bundleStream.flatMap(this::findServiceProviders).forEach(sp -> {
			serviceProviders.computeIfAbsent(sp.getServiceClassname(), k -> new ArrayList<>()).add(sp);
		});

		this.serviceProviders = serviceProviders;
		ConfigProviderResolver.setInstance(EmptyConfigProviderResolver.INSTANCE);//TODO Eclipse Config Registry Provider?
		getGlobalResteasyProviderFactory();//Init global singleton
	}

	private Stream<Bundle> findBundles(String bundleName) {
		Bundle contextBundle = FrameworkUtil.getBundle(ResteasyFactoryService.class);
		if (bundleName == null) {
			return Stream.of(contextBundle);
		}
		return Arrays.stream(contextBundle.getBundleContext().getBundles())
				.filter(bundle -> bundleName.equals(bundle.getSymbolicName()));
	}

	private Stream<IServiceProvider<?>> findServiceProviders(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return Stream.empty();
		}
		Collection<String> services = wiring.listResources("META-INF/services/", "*", 0);
		return services.stream()
				.flatMap(service -> findServiceProviderURLs(bundle, service))
				.flatMap(serviceUrl -> createServiceProviders(bundle, serviceUrl));
	}

	private Stream<IServiceProvider<?>> createServiceProviders(Bundle bundle, URL serviceUrl) {
		String serviceClassname = serviceUrl.getPath().substring(serviceUrl.getPath().lastIndexOf('/') + 1);
		List<IServiceProvider<?>> providers = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(serviceUrl.openStream(), StandardCharsets.UTF_8))) {
			for (String providerClassname = r.readLine(); providerClassname != null; providerClassname = r.readLine()) {
				providerClassname = providerClassname.trim();
				if (providerClassname.isEmpty()) {
					continue;
				}
				IServiceProvider<?> provider = new BundleServiceProvider<>(bundle, serviceClassname, providerClassname);
				providers.add(provider);
			}
		} catch (IOException ex) {
			//TODO
		}
		return providers.stream();
	}

	private Stream<URL> findServiceProviderURLs(Bundle bundle, String servicePath) {
		Enumeration<URL> resources;
		try {
			resources = bundle.getResources(servicePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return Stream.empty();
		}
		if (resources == null || !resources.hasMoreElements()) {
			return Stream.empty();
		}
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<URL>() {
			@Override
			public URL next() {
				return resources.nextElement();
			}

			@Override
			public boolean hasNext() {
				return resources.hasMoreElements();
			}
		}, Spliterator.ORDERED), false);
	}

	List<IServiceProvider<?>> getServiceProviders(String service) {
		return serviceProviders.getOrDefault(service, Collections.emptyList());
	}

	public ResteasyProviderFactory newLocalFactory() {
		LocalResteasyProviderFactory providerFactory = new LocalResteasyProviderFactory(
				getClientInitializedResteasyProviderFactory()) {
			@Override
			protected void registerBuiltin() {
				ResteasyFactoryService.this.registerBuiltin(this);
			}
		};

		if (ResteasyProviderFactory.peekInstance() != null) {
			providerFactory.initializeClientProviders(ResteasyProviderFactory.getInstance());
		}
		// Execution of 'if' above overwrites providerFactory clientRequestFilterRegistry
		// Reregister provider as appropriate.
		if (RegisterBuiltin.isGZipEnabled()) {
			providerFactory.registerProvider(AcceptEncodingGZIPFilter.class, true);
		}
		return providerFactory;
	}

	public ResteasyProviderFactory getGlobalResteasyProviderFactory() {
		ResteasyProviderFactory factory = ResteasyProviderFactory.peekInstance();
		if (factory == null) {
			factory = initSingletonFactory();
			ResteasyProviderFactory.setInstance(factory);
		}
		return factory;
	}

	public ResteasyProviderFactory getClientInitializedResteasyProviderFactory() {
		if (clientInitializedResteasyProviderFactory == null) {

			ResteasyProviderFactory rpf = new ResteasyProviderFactoryImpl(RuntimeType.CLIENT) {
				@Override
				public RuntimeType getRuntimeType() {
					return RuntimeType.CLIENT;
				}
			};
			if (!rpf.isBuiltinsRegistered()) {
				registerBuiltin(rpf);
			}
			clientInitializedResteasyProviderFactory = rpf;
		}
		return clientInitializedResteasyProviderFactory;
	}

	private ResteasyProviderFactory initSingletonFactory() {
		ResteasyProviderFactory resteasyProviderFactory = new ResteasyProviderFactoryImpl();
		registerBuiltin(resteasyProviderFactory);
		return resteasyProviderFactory;
	}

	public ResteasyClientBuilder newClientBuilder() {
		return new ResteasyClientBuilderImpl() {
			@Override
			public ResteasyProviderFactory getProviderFactory() {
				if (providerFactory == null) {
					providerFactory = newLocalFactory();
				}
				return providerFactory;
			}
		};
	}

	public void registerBuiltin(ResteasyProviderFactory factory) {
		final ResteasyProviderFactory monitor = (factory instanceof ThreadLocalResteasyProviderFactory)
				? ((ThreadLocalResteasyProviderFactory) factory).getDelegate()
						: factory;
		synchronized (monitor) {
			if (factory.isBuiltinsRegistered() || !factory.isRegisterBuiltins()) {
				return;
			}
			try {
				registerProviders(factory);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			factory.setBuiltinsRegistered(true);
		}
	}

	public void registerProviders(ResteasyProviderFactory factory) throws Exception {
		List<IServiceProvider<?>> serviceProviders = getServiceProviders(Providers.class.getName());
		serviceProviders.stream()
		.map(this::safeGetServiceProviderClass)
		.filter(clazz -> clazz != null)
		.forEach(clazz -> factory.registerProvider(clazz, true));
		if (RegisterBuiltin.isGZipEnabled()) {
			factory.registerProvider(GZIPDecodingInterceptor.class, true);
			factory.registerProvider(GZIPEncodingInterceptor.class, true);
		}
	}

	private Class<?> safeGetServiceProviderClass(IServiceProvider<?> provider) {
		try {
			return provider.getServiceProviderClass();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	private static interface IServiceProvider<T> {
		String getServiceClassname();

		Class<T> getServiceClass() throws ClassNotFoundException;

		String getServiceProviderClassname();

		Class<? extends T> getServiceProviderClass() throws ClassNotFoundException;

		T getServiceInstance();
	}

	private static class BundleServiceProvider<T> implements IServiceProvider<T> {
		private final Bundle bundle;

		private final String serviceClassname;

		private final String serviceProviderClassname;

		private transient Class<T> serviceClass;

		private transient Class<T> serviceProviderClass;

		BundleServiceProvider(Bundle bundle, String serviceClassname, String serviceProviderClassname) {
			this.bundle = bundle;
			this.serviceClassname = serviceClassname;
			this.serviceProviderClassname = serviceProviderClassname;
		}

		@Override
		public String getServiceClassname() {
			return serviceClassname;
		}

		@Override
		public String getServiceProviderClassname() {
			return serviceClassname;
		}

		@Override
		public Class<T> getServiceClass() throws ClassNotFoundException {
			if (serviceClass == null) {
				@SuppressWarnings("unchecked")
				Class<T> serviceClass = (Class<T>) bundle.loadClass(serviceClassname);
				this.serviceClass = serviceClass;
			}
			return serviceClass;
		}

		@Override
		public Class<T> getServiceProviderClass() throws ClassNotFoundException {
			if (serviceProviderClass == null) {
				@SuppressWarnings("unchecked")
				Class<T> serviceProviderClass = (Class<T>) bundle.loadClass(serviceProviderClassname);
				this.serviceProviderClass = serviceProviderClass;
			}
			return serviceProviderClass;
		}

		@Override
		public T getServiceInstance() {
			try {
				Class<T> serviceProviderClass = getServiceProviderClass();
				return serviceProviderClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new UnsupportedOperationException(e);
			}
		}
	}

}
