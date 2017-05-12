/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.service;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.IStorageService.Dynamic;
import org.eclipse.userstorage.internal.StorageService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class MarketplaceStorageServiceRegistrationTest {

	public static class TemporaryStorageServices extends ExternalResource {
		private final Set<URI> temporaryStorageServiceUris = new LinkedHashSet<URI>();

		public IStorageService create(String name, String uri) {
			URI _uri = URI.create(uri);
			Dynamic service = IStorageService.Registry.INSTANCE.addService(name, _uri);
			cleanup(service.getServiceURI());
			return service;
		}

		public IStorageService provide(String name, String uri) {
			URI _uri = URI.create(uri);
			IStorageService service = IStorageService.Registry.INSTANCE.getService(_uri);
			if (service != null) {
				return service;
			}
			return create(name, uri);
		}

		public void cleanup(URI uri) {
			temporaryStorageServiceUris.add(uri);
		}

		public void cleanup(String uri) {
			cleanup(URI.create(uri));
		}

		@Override
		protected void after() {
			RuntimeException error = null;
			for (URI uri : temporaryStorageServiceUris) {
				try {
					IStorageService service = IStorageService.Registry.INSTANCE.getService(uri);
					if (service != null) {
						((IStorageService.Dynamic) service).remove();
					}
				} catch (RuntimeException ex) {
					if (error != null) {
						error.addSuppressed(ex);
					} else {
						error = ex;
					}
				}
			}
			if (error != null) {
				throw error;
			}
		}
	}

	@Rule
	public final TemporaryStorageServices storageServices = new TemporaryStorageServices();

	private ServiceLocator serviceLocator;

	@Before
	public void setUp() {
		serviceLocator = (ServiceLocator) ServiceHelper.getMarketplaceServiceLocator();
		storageServices.cleanup("https://api-test.example.org/");
		storageServices.cleanup("https://api-test.example.org");
	}

	@Test
	public void testRegisterNewStorageServiceWithSlash() {
		IMarketplaceStorageService registered = serviceLocator.registerStorageService(
				DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, "https://api-test.example.org/", null);
		assertNotNull(registered);
		assertEquals("https://api-test.example.org/", registered.getServiceUri().toString());
	}

	@Test
	public void testRegisterNewStorageServiceWithoutSlash() {
		IMarketplaceStorageService registered = serviceLocator.registerStorageService(
				DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, "https://api-test.example.org", null);
		assertNotNull(registered);
		assertEquals("https://api-test.example.org/", registered.getServiceUri().toString());

		IStorageService withoutSlash = IStorageService.Registry.INSTANCE.getService(URI.create(
				"https://api-test.example.org"));
		assertNull(withoutSlash);
	}

	@Test
	public void testRegisterStorageServiceWithSlashHavingExistingWithoutSlash() {
		storageServices.provide("WithoutSlash", "https://api-test.example.org");
		IMarketplaceStorageService registered = serviceLocator.registerStorageService(
				DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, "https://api-test.example.org/", null);
		assertNotNull(registered);
		assertEquals("https://api-test.example.org", registered.getServiceUri().toString());

		IStorageService withSlash = IStorageService.Registry.INSTANCE.getService(URI.create(
				"https://api-test.example.org/"));
		assertNull(withSlash);
	}

	@Test
	public void testRegisterStorageServiceWithoutSlashHavingExistingWithSlash() {
		storageServices.provide("WithSlash", "https://api-test.example.org/");
		IMarketplaceStorageService registered = serviceLocator.registerStorageService(
				DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, "https://api-test.example.org", null);
		assertNotNull(registered);
		assertEquals("https://api-test.example.org/", registered.getServiceUri().toString());

		IStorageService withoutSlash = IStorageService.Registry.INSTANCE.getService(URI.create(
				"https://api-test.example.org"));
		assertNull(withoutSlash);
	}

	@Test
	public void testCleanupDuplicateStorageService() throws Exception {
		IStorageService withSlash = storageServices.provide("WithSlash", "https://api-test.example.org/");
		IStorageService withoutSlash = storageServices.provide("WithoutSlash", "https://api-test.example.org");
		((StorageService) withSlash).getSecurePreferences().remove("username");
		((StorageService) withoutSlash).getSecurePreferences().put("username", "testuser", false);

		IMarketplaceStorageService registered = serviceLocator.registerStorageService(
				DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, "https://api-test.example.org", null);
		assertNotNull(registered);
		assertEquals("https://api-test.example.org/", registered.getServiceUri().toString());
		assertSame(withSlash, registered.getStorage().getService());

		withoutSlash = IStorageService.Registry.INSTANCE.getService(URI.create(
				"https://api-test.example.org"));
		assertNull(withoutSlash);

		String username = ((StorageService) withSlash).getSecurePreferences().get("username", null);
		assertEquals("testuser", username);
	}

	@Test
	public void testCleanupDuplicateEclipseOrgStorageService() throws Exception {
		IStorageService withSlash = storageServices.provide("WithSlash", "https://api.eclipse.org/");
		assertFalse(withSlash instanceof IStorageService.Dynamic);
		IStorageService withoutSlash = storageServices.provide("WithoutSlash", "https://api.eclipse.org");
		assertTrue(withoutSlash instanceof IStorageService.Dynamic);

		IMarketplaceStorageService registered = serviceLocator.registerStorageService(
				DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, "https://api.eclipse.org/", null);
		assertNotNull(registered);
		assertSame(withSlash, registered.getStorage().getService());

		withoutSlash = IStorageService.Registry.INSTANCE.getService(URI.create("https://api.eclipse.org"));
		assertNull(withoutSlash);
	}
}
