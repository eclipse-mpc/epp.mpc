/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.service;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.Catalog;
import org.eclipse.epp.internal.mpc.core.service.CatalogService;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Benjamin Muskalla
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class CatalogServiceTest {

	private final class MockCatalogService implements CatalogService {
		public List<Catalog> listCatalogs(IProgressMonitor monitor) throws CoreException {
			Catalog catalog = new Catalog();
			catalog.setId("mock");
			return Collections.singletonList(catalog);
		}
	}

	private ICatalogService catalogService;

	private IMarketplaceServiceLocator serviceLocator;

	private ServiceReference<IMarketplaceServiceLocator> serviceLocatorReference;

	private BundleContext bundleContext;

	@Before
	public void setUp() throws Exception {
		bundleContext = MarketplaceClientCorePlugin.getBundle().getBundleContext();
		serviceLocatorReference = bundleContext.getServiceReference(IMarketplaceServiceLocator.class);
		serviceLocator = bundleContext.getService(serviceLocatorReference);
		catalogService = serviceLocator.getCatalogService();
	}

	@Test
	public void listCatalogs() throws CoreException {
		List<? extends ICatalog> catalogs = catalogService.listCatalogs(new NullProgressMonitor());
		assertNotNull(catalogs);
		assertFalse(catalogs.isEmpty());

		for (ICatalog catalog : catalogs) {
			assertNotNull(catalog.getId());
			assertNotNull(catalog.getUrl());
			assertNotNull(catalog.getName());
		}
	}

	@Test
	public void testSampleCatalog() throws Exception {
		//		ServiceLocator oldInstance = ServiceLocator.getInstance();
		//		ServiceLocator.setInstance(new ServiceLocator() {
		//			@Override
		//			public ICatalogService getCatalogService() {
		//				return new MockCatalogService();
		//			}
		//		});
		ServiceRegistration<ICatalogService> registration = bundleContext.registerService(ICatalogService.class,
				new MockCatalogService(), ServiceUtil.serviceRanking(Integer.MAX_VALUE, null));
		try {
			catalogService = ServiceLocator.getInstance().getCatalogService();
			List<? extends ICatalog> catalogs = catalogService.listCatalogs(null);
			assertEquals(1, catalogs.size());
			assertEquals("mock", catalogs.get(0).getId());
		} finally {
			//			ServiceLocator.setInstance(oldInstance);
			registration.unregister();
		}
	}

	@After
	public void tearDown() {
		serviceLocator = null;
		bundleContext.ungetService(serviceLocatorReference);
		serviceLocatorReference = null;
		bundleContext = null;
	}
}
