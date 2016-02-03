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

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

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
	public void testTansportFactoryInstance() {
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
}
