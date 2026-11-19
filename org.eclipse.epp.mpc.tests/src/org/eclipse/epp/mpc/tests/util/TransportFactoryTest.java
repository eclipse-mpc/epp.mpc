/*******************************************************************************
 * Copyright (c) 2010, 2026 The Eclipse Foundation and others.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.ServiceHelperImpl;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientCustomizer;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientService;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransport;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransportFactory;
import org.eclipse.epp.internal.mpc.core.util.FallbackTransportFactory;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.eclipse.epp.mpc.tests.LambdaMatchers;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
		public CredentialsStore customizeCredentialsProvider(CredentialsStore credentialsProvider) {
			return credentialsProvider;
		}

		@Override
		public HttpClientBuilder customizeBuilder(HttpClientBuilder builder) {
			HttpClientConnectionManager manager = Mockito.spy(new BasicHttpClientConnectionManager());
			try {
				Mockito.doNothing().when(manager).connect(ArgumentMatchers.any(), ArgumentMatchers.any(),
						ArgumentMatchers.any());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			builder.setConnectionManager(manager);
			return builder.addRequestInterceptorLast((HttpRequestInterceptor) (request, entityDetails, context) -> {
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

	@BeforeEach
	public void clearTransportFilters() {
		System.getProperties().remove("org.eclipse.epp.mpc.core.service.transport.disabled");
		System.getProperties().remove("org.eclipse.ecf.provider.filetransfer.excludeContributors");
	}

	@BeforeEach
	public void initServiceHelper() {
		ServiceHelperImpl.getImplInstance();
	}

	@Test
	public void testRegisteredFactories() throws Exception {
		BundleContext context = FrameworkUtil.getBundle(TransportFactoryTest.class).getBundleContext();
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

	@Test
	public void testHttpClientTransportErrorHandling() throws Exception {
		HttpClientTransport transport = createFailingHttpClientTransport();
		assertThrows(ServiceUnavailableException.class,
				() -> transport.stream(URI.create("http://127.0.0.1:54321"), null));
	}

	private static HttpClientTransport createFailingHttpClientTransport() throws Exception {
		HttpClientTransport transport = new HttpClientTransport();
		HttpClientService mockClient = Mockito.mock(HttpClientService.class);
		Mockito.when(mockClient.configureRequest(ArgumentMatchers.any())).thenAnswer(args -> args.getArgument(0));

		Mockito.doReturn(mockResponse(503, "Expected test error")).when(mockClient).execute(
				ArgumentMatchers.any());

		transport.bindHttpClientService(mockClient);
		return transport;
	}

	@Test
	public void testHttpClientTransportFallback() throws Exception {
		HttpClientTransport httpClientTransport = createFailingHttpClientTransport();

		HttpClientTransportFactory httpClientFactory = new HttpClientTransportFactory();
		httpClientFactory.setTransport(httpClientTransport);

		String expectedMessage = "Secondary transport";
		ITransportFactory secondaryFactory = Mockito.mock(ITransportFactory.class);
		ITransport secondaryTransport = Mockito.mock(ITransport.class);
		InputStream expectedResultStream = new ByteArrayInputStream(expectedMessage.getBytes(
				StandardCharsets.UTF_8));
		Mockito.when(secondaryFactory.getTransport()).thenReturn(secondaryTransport);
		Mockito.when(secondaryTransport.stream(ArgumentMatchers.<URI> any(), ArgumentMatchers.<IProgressMonitor> any())).thenReturn(
				expectedResultStream);

		FallbackTransportFactory fallbackTransportFactory = new FallbackTransportFactory();
		fallbackTransportFactory.setPrimaryFactory(httpClientFactory);
		fallbackTransportFactory.setSecondaryFactory(secondaryFactory);

		InputStream stream = fallbackTransportFactory.getTransport().stream(URI.create("http://127.0.0.1:54321"), null);
		byte[] resultBytes = new byte[24];
		int read = stream.read(resultBytes);
		String actualMessage = new String(resultBytes, 0, read, StandardCharsets.UTF_8);
		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	public void testHttpClientCustomizer() throws Exception {
		final HttpClientCustomizer customizer = Mockito.mock(HttpClientCustomizer.class);
		Mockito.when(customizer.customizeBuilder(ArgumentMatchers.any())).thenAnswer(invocation -> {
			HttpClientBuilder builder = (HttpClientBuilder) invocation.getArguments()[0];
			return builder == null ? null
					: builder.addRequestInterceptorFirst((HttpRequestInterceptor) (request, entityDetails, context) -> request.addHeader(
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

		BundleContext clientServiceContext = FrameworkUtil.getBundle(HttpClientService.class).getBundleContext();
		List<ServiceRegistration<?>> registrations = new ArrayList<>();
		for (int i = 0; i < customizers.length; i++) {
			for (HttpClientCustomizer customizer : customizers) {
				if (Mockito.mockingDetails(customizer).isMock()) {
					Mockito.clearInvocations(customizer);
				}
			}
			HttpClientCustomizer customizer = customizers[i];
			Dictionary<String, Object> serviceProperties = ServiceUtil.serviceName(
					"org.eclipse.epp.mpc.core.transport.http.test.customizer." + i, ServiceUtil.serviceRanking(1000 + i,
							null));
			ServiceRegistration<?> registration = clientServiceContext
					.registerService(HttpClientCustomizer.class, customizer, serviceProperties);
			registrations.add(registration);
		}

		ServiceReference<HttpClientService> clientServiceRef = clientServiceContext.getServiceReference(
				HttpClientService.class);
		HttpClientService clientService = clientServiceContext.getService(clientServiceRef);

		HttpClientTransport httpClientTransport;
		try
		{
			httpClientTransport = new HttpClientTransport();
			httpClientTransport.bindHttpClientService(clientService);
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
	@EnabledOnOs(OS.WINDOWS)
	public void testHttpClientTransportWin32Support() throws Exception {
		HttpContext context = interceptRequest().getInterceptedContext();

		Lookup<AuthSchemeFactory> authRegistry = HttpClientContext.castOrCreate(context).getAuthSchemeRegistry();
		CredentialsProvider credentialsProvider = HttpClientContext.castOrCreate(context).getCredentialsProvider();

		assertNotNull(authRegistry);
		Object ntlmFactory = authRegistry.lookup("NTLM");
		assertNotNull(ntlmFactory);
		assertEquals("org.apache.hc.client5.http.impl.win.WindowsNTLMSchemeFactory", ntlmFactory.getClass().getName());

		assertNotNull(credentialsProvider);
		//		List<CredentialsProvider> nestedProviders = listCredentialsProviders(credentialsProvider);
		//		assertThat(nestedProviders, hasItem(LambdaMatchers.map(x -> x.getClass().getName()).matches(
		//				"org.apache.http.impl.auth.win.WindowsCredentialsProvider")));
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
		HttpContext context = HttpClientContext.create();
		assertThrows(ConnectionClosedException.class, () -> client.execute(new HttpGet("http://localhost/test"),
				context, response -> null));
		return abortRequestCustomizer;
	}

	private static ClassicHttpResponse mockResponse(int statusCode, String reasonPhrase) {

		ClassicHttpResponse mockResponse = Mockito.mock(ClassicHttpResponse.class);
		Mockito.when(mockResponse.getCode()).thenReturn(statusCode);
		Mockito.when(mockResponse.getReasonPhrase()).thenReturn(reasonPhrase);
		return mockResponse;
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

}
