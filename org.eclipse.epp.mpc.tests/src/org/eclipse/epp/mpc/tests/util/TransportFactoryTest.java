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
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.ChainedCredentialsProvider;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientCustomizer;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransport;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransportFactory;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.SynchronizedCredentialsProvider;
import org.eclipse.epp.internal.mpc.core.util.FallbackTransportFactory;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.eclipse.epp.mpc.tests.LambdaMatchers;
import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;

public class TransportFactoryTest {

	private static class AbortRequestCustomizer implements HttpClientCustomizer {
		private HttpContext interceptedContext;

		private HttpRequest interceptedRequest;

		@Override
		public CredentialsProvider customizeCredentialsProvider(CredentialsProvider credentialsProvider) {
			return null;
		}

		@Override
		public HttpClientBuilder customizeBuilder(HttpClientBuilder builder) {
			return builder.addInterceptorLast((HttpRequestInterceptor) (request, context) -> {
				interceptedRequest = request;
				interceptedContext = context;
				throw new ConnectionClosedException("Aborting test request before execution");
			}).disableAutomaticRetries();
		}

		public HttpRequest getInterceptedRequest() {
			return interceptedRequest;
		}

		public HttpContext getInterceptedContext() {
			return interceptedContext;
		}
	}

	@Before
	public void clearTransportFilters() {
		System.getProperties().remove("org.eclipse.epp.mpc.core.service.transport.disabled");
		System.getProperties().remove("org.eclipse.ecf.provider.filetransfer.excludeContributors");
	}

	@Test
	public void testRegisteredFactories() throws Exception {
		BundleContext context = MarketplaceClientCorePlugin.getBundle().getBundleContext();
		Collection<ServiceReference<ITransportFactory>> serviceReferences = context.getServiceReferences(
				ITransportFactory.class, null);
		assertFalse(serviceReferences.isEmpty());
		List<Class<?>> registeredFactoryTypes = new ArrayList<>();
		Set<ITransportFactory> registeredFactories = new LinkedHashSet<>();
		for (ServiceReference<ITransportFactory> serviceReference : serviceReferences) {
			try {
				ITransportFactory service = context.getService(serviceReference);
				assertNotNull(service);
				assertThat(registeredFactoryTypes, not(hasItem(service.getClass())));
				assertThat(registeredFactories, not(hasItem(service)));
				registeredFactoryTypes.add(service.getClass());
				registeredFactories.add(service);
			} finally {
				context.ungetService(serviceReference);
			}
		}

		List<ITransportFactory> legacyFactories = TransportFactory.listAvailableFactories();
		for (ITransportFactory factory : legacyFactories) {
			assertThat(registeredFactoryTypes, CoreMatchers.hasItem(factory.getClass()));
		}
	}

	@Test
	public void testTransportFactoryInstance() {
		ITransport transport = TransportFactory.createTransport();
		assertNotNull(transport);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testLegacyTansportFactoryInstance() {
		TransportFactory instance = TransportFactory.instance();
		org.eclipse.epp.internal.mpc.core.util.ITransport transport = instance.getTransport();
		assertNotNull(transport);
	}

	@Test
	public void testLegacyServices() {
		BundleContext context = FrameworkUtil.getBundle(ITransportFactory.class).getBundleContext();
		assertNotNull(context);
		TransportFactory.LegacyTransportFactoryTracker legacyTransportFactoryTracker = new TransportFactory.LegacyTransportFactoryTracker(
				context);
		legacyTransportFactoryTracker.open();
		try {
			Object[] services = legacyTransportFactoryTracker.getServices();
			List<Class<?>> registeredFactoryTypes = new ArrayList<>();
			Set<Object> registeredFactories = new LinkedHashSet<>();
			for (Object service : services) {
				assertNotNull(service);
				assertThat(service, instanceOf(TransportFactory.class));
				assertThat(registeredFactoryTypes, not(hasItem(service.getClass())));
				assertThat(registeredFactories, not(hasItem(service)));
				registeredFactoryTypes.add(service.getClass());
				registeredFactories.add(service);
			}

			List<ITransportFactory> legacyFactories = TransportFactory.listAvailableFactories();
			for (ITransportFactory factory : legacyFactories) {
				assertThat(registeredFactoryTypes, CoreMatchers.hasItem(factory.getClass()));
			}
		} finally {
			legacyTransportFactoryTracker.close();
		}
	}

	@Test
	public void testStream() throws Exception {
		ITransport transport = ServiceHelper.getTransportFactory().getTransport();
		URI uri = new URI("http://www.eclipse.org/index.php");
		InputStream stream = transport.stream(uri, new NullProgressMonitor());
		assertNotNull(stream);
		stream.close();
	}

	@Test
	public void testHttpWrapperDefaultService() {
		String transportServiceName = getTransportServiceName();
		assertEquals("org.eclipse.epp.mpc.core.transport.http.wrapper", transportServiceName);
	}

	@Test
	public void testDisableTransports() {
		System.setProperty("org.eclipse.epp.mpc.core.service.transport.disabled",
				"org.eclipse.epp.mpc.core.transport.http.wrapper");
		String transportServiceName = getTransportServiceName();
		assertEquals("legacy:org.eclipse.epp.internal.mpc.core.util.P2TransportFactory", transportServiceName);

		System.setProperty("org.eclipse.epp.mpc.core.service.transport.disabled",
				"org.eclipse.epp.mpc.core.transport.http.wrapper,"
						+ "legacy:org.eclipse.epp.internal.mpc.core.util.P2TransportFactory,"
						+ "legacy:org.eclipse.epp.internal.mpc.core.util.Eclipse36TransportFactory");
		transportServiceName = getTransportServiceName();
		assertEquals("legacy:org.eclipse.epp.internal.mpc.core.util.JavaPlatformTransportFactory",
				transportServiceName);
	}

	@Test
	public void testDisableHttpClientTransportsThroughECF() {
		System.setProperty("org.eclipse.ecf.provider.filetransfer.excludeContributors",
				"org.eclipse.ecf.provider.filetransfer.httpclient4");
		String transportServiceName = getTransportServiceName();
		assertEquals("legacy:org.eclipse.epp.internal.mpc.core.util.P2TransportFactory", transportServiceName);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testHttpClientTransportErrorHandling() throws Exception {
		createFailingHttpClientTransport().stream(URI.create("http://127.0.0.1:54321"), null);
	}

	private static HttpClientTransport createFailingHttpClientTransport() {
		return new HttpClientTransport() {
			@Override
			protected Response execute(Request request, URI uri) throws ClientProtocolException, IOException {
				HttpResponse mockResponse = mockResponse(mockStatusLine(503, "Expected test error"), null);
				try {
					Constructor<Response> ctor = Response.class.getDeclaredConstructor(HttpResponse.class);
					ctor.setAccessible(true);
					return ctor.newInstance(mockResponse);
				} catch (Exception e) {
					try {
						fail("Failed to create response");
					} catch (AssertionError ae) {
						ae.initCause(e);
						throw ae;
					}
					return null;
				}
			}
		};
	}

	@Test
	public void testHttpClientTransportFallback() throws Exception {
		HttpClientTransport httpClientTransport = createFailingHttpClientTransport();

		HttpClientTransportFactory httpClientFactory = new HttpClientTransportFactory();
		httpClientFactory.setTransport(httpClientTransport);
		ITransportFactory secondaryFactory = Mockito.mock(ITransportFactory.class);
		ITransport secondaryTransport = Mockito.mock(ITransport.class);
		InputStream expectedResultStream = new ByteArrayInputStream("Secondary transport".getBytes("UTF-8"));
		Mockito.when(secondaryFactory.getTransport()).thenReturn(secondaryTransport);
		Mockito.when(secondaryTransport.stream(ArgumentMatchers.<URI> any(), ArgumentMatchers.<IProgressMonitor> any())).thenReturn(
				expectedResultStream);

		FallbackTransportFactory fallbackTransportFactory = new FallbackTransportFactory();
		fallbackTransportFactory.setPrimaryFactory(httpClientFactory);
		fallbackTransportFactory.setSecondaryFactory(secondaryFactory);

		InputStream stream = fallbackTransportFactory.getTransport().stream(URI.create("http://127.0.0.1:54321"), null);
		assertSame(expectedResultStream, stream);
	}

	@Test
	public void testHttpClientCustomizer() throws Exception {
		final HttpClientCustomizer customizer = Mockito.mock(HttpClientCustomizer.class);
		Mockito.when(customizer.customizeBuilder(ArgumentMatchers.any())).thenAnswer(invocation -> {
			HttpClientBuilder builder = (HttpClientBuilder) invocation.getArguments()[0];
			return builder == null ? null
					: builder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> request.addHeader(
							"X-Customizer-Test", "true"));
		});
		Mockito.when(customizer.customizeCredentialsProvider(ArgumentMatchers.any())).thenReturn(null);

		HttpRequest request = interceptRequest(customizer).getInterceptedRequest();

		Mockito.verify(customizer).customizeBuilder(ArgumentMatchers.any());
		Mockito.verify(customizer).customizeCredentialsProvider(ArgumentMatchers.any());

		assertThat(request.getFirstHeader("X-Customizer-Test"), LambdaMatchers.<Header, String> map(x -> x == null
				? null : x.getValue())
				.matches("true"));
	}

	private static HttpClientTransport createClient(HttpClientCustomizer... customizers) {
		if (customizers == null || customizers.length == 0) {
			return new HttpClientTransport();
		}

		List<ServiceRegistration<?>> registrations = new ArrayList<>();
		for (int i = 0; i < customizers.length; i++) {
			HttpClientCustomizer customizer = customizers[i];
			Dictionary<String, Object> serviceProperties = ServiceUtil.serviceName(
					"org.eclipse.epp.mpc.core.transport.http.test.customizer." + i, ServiceUtil.serviceRanking(1000 + i,
							null));
			ServiceRegistration<?> registration = FrameworkUtil.getBundle(HttpClientCustomizer.class).getBundleContext()
					.registerService(HttpClientCustomizer.class, customizer, serviceProperties);
			registrations.add(registration);
		}
		HttpClientTransport httpClientTransport;
		try
		{
			httpClientTransport = new HttpClientTransport();
		}
		finally
		{
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
		return httpClientTransport;
	}


	@Test
	public void testHttpClientTransportWin32Support() throws Exception {
		BundleContext bundleContext = FrameworkUtil.getBundle(TransportFactory.class).getBundleContext();
		Assume.assumeThat(bundleContext.getProperty("osgi.os"), is("win32"));
		HttpContext context = interceptRequest().getInterceptedContext();

		Lookup<?> authRegistry = (Lookup<?>) context.getAttribute(HttpClientContext.AUTHSCHEME_REGISTRY);
		CredentialsProvider credentialsProvider = (CredentialsProvider) context.getAttribute(
				HttpClientContext.CREDS_PROVIDER);

		assertNotNull(authRegistry);
		Object ntlmFactory = authRegistry.lookup(AuthSchemes.NTLM);
		assertNotNull(ntlmFactory);
		assertEquals("org.apache.http.impl.auth.win.WindowsNTLMSchemeFactory", ntlmFactory.getClass().getName());

		assertNotNull(credentialsProvider);
		List<CredentialsProvider> nestedProviders = listCredentialsProviders(credentialsProvider);
		assertThat(nestedProviders, hasItem(LambdaMatchers.map(x -> x.getClass().getName()).matches(
				"org.apache.http.impl.auth.win.WindowsCredentialsProvider")));
	}

	private static AbortRequestCustomizer interceptRequest(HttpClientCustomizer... customizers) throws Exception {
		AbortRequestCustomizer abortRequestCustomizer = new AbortRequestCustomizer();
		HttpClientCustomizer[] mergedCustomizers;
		if (customizers == null || customizers.length == 0) {
			mergedCustomizers = new HttpClientCustomizer[] { abortRequestCustomizer };
		} else {
			mergedCustomizers = new HttpClientCustomizer[customizers.length + 1];
			System.arraycopy(customizers, 0, mergedCustomizers, 0, customizers.length);
			mergedCustomizers[customizers.length] = abortRequestCustomizer;
		}

		HttpClientTransport httpClientTransport = createClient(mergedCustomizers);
		HttpClient client = httpClientTransport.getClient();
		HttpContext context = new BasicHttpContext();
		try {
			client.execute(new HttpGet("http://localhost/test"), context);
			fail("Expected request execution to fail");
		} catch (ConnectionClosedException ex) {
			//ignore expected exception
		}
		return abortRequestCustomizer;
	}

	@Test
	public void testECFContributorPropertiesUnchanged() {
		assertEquals("org.eclipse.ecf.provider.filetransfer.excludeContributors",
				org.eclipse.ecf.internal.provider.filetransfer.Activator.PLUGIN_EXCLUDED_SYS_PROP_NAME);
		assertEquals("org.eclipse.ecf.provider.filetransfer.httpclient4",
				org.eclipse.ecf.internal.provider.filetransfer.httpclient4.Activator.PLUGIN_ID);
	}

	private static HttpResponse mockResponse(StatusLine statusLine, HttpEntity entity) {
		HttpResponse mockResponse = Mockito.mock(HttpResponse.class);
		Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);
		Mockito.when(mockResponse.getEntity()).thenReturn(entity);
		return mockResponse;
	}

	private static StatusLine mockStatusLine(int statusCode, String reasonPhrase) {
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(statusLine.getReasonPhrase()).thenReturn(reasonPhrase);
		Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
		return statusLine;
	}

	private static String getTransportServiceName() {
		BundleContext bundleContext = FrameworkUtil.getBundle(TransportFactory.class).getBundleContext();
		Collection<ServiceReference<ITransportFactory>> transportServiceReferences = TransportFactory
				.getTransportServiceReferences(
				bundleContext);
		if (!transportServiceReferences.isEmpty()) {
			ServiceReference<ITransportFactory> transportServiceReference = transportServiceReferences.iterator()
					.next();
			String transportServiceName = (String) transportServiceReference.getProperty(
					ComponentConstants.COMPONENT_NAME);
			bundleContext.ungetService(transportServiceReference);
			return transportServiceName;
		}
		return null;
	}

	private static List<CredentialsProvider> listCredentialsProviders(CredentialsProvider provider) {
		ArrayList<CredentialsProvider> providers = new ArrayList<>();
		doListCredentialsProviders(provider, providers);
		return providers;
	}

	private static void doListCredentialsProviders(CredentialsProvider provider, List<CredentialsProvider> providers) {
		providers.add(provider);
		if (provider instanceof SynchronizedCredentialsProvider) {
			SynchronizedCredentialsProvider synced = (SynchronizedCredentialsProvider) provider;
			doListCredentialsProviders(synced.getDelegate(), providers);
		} else if (provider instanceof ChainedCredentialsProvider) {
			ChainedCredentialsProvider chain = (ChainedCredentialsProvider) provider;
			doListCredentialsProviders(chain.getFirst(), providers);
			doListCredentialsProviders(chain.getSecond(), providers);
		}
	}
}
