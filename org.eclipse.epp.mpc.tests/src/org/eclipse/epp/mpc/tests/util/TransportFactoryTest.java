/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransport;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransportFactory;
import org.eclipse.epp.internal.mpc.core.util.FallbackTransportFactory;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

public class TransportFactoryTest {

	@Test
	public void testRegisteredFactories() throws Exception {
		BundleContext context = MarketplaceClientCorePlugin.getBundle().getBundleContext();
		Collection<ServiceReference<ITransportFactory>> serviceReferences = context.getServiceReferences(
				ITransportFactory.class, null);
		assertFalse(serviceReferences.isEmpty());
		List<Class<?>> registeredFactoryTypes = new ArrayList<Class<?>>();
		Set<ITransportFactory> registeredFactories = new LinkedHashSet<ITransportFactory>();
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
			List<Class<?>> registeredFactoryTypes = new ArrayList<Class<?>>();
			Set<Object> registeredFactories = new LinkedHashSet<Object>();
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
		Mockito.when(secondaryTransport.stream(Matchers.<URI> any(), Matchers.<IProgressMonitor> any())).thenReturn(
				expectedResultStream);

		FallbackTransportFactory fallbackTransportFactory = new FallbackTransportFactory();
		fallbackTransportFactory.setPrimaryFactory(httpClientFactory);
		fallbackTransportFactory.setSecondaryFactory(secondaryFactory);

		InputStream stream = fallbackTransportFactory.getTransport().stream(URI.create("http://127.0.0.1:54321"), null);
		assertSame(expectedResultStream, stream);
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
		ServiceReference<ITransportFactory> transportServiceReference = bundleContext.getServiceReference(
				ITransportFactory.class);
		if (transportServiceReference != null) {
			String transportServiceName = (String) transportServiceReference.getProperty(
					ComponentConstants.COMPONENT_NAME);
			bundleContext.ungetService(transportServiceReference);
			return transportServiceName;
		}
		return null;
	}
}
