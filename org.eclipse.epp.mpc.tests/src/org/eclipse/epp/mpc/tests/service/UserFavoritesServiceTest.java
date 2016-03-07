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
import java.util.List;

import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.internal.StorageServiceRegistry;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserFavoritesServiceTest {

	private static final URI USERSTORAGE_SERVICE_URI = URI.create("https://api-staging.eclipse.org");

	protected IMarketplaceStorageService marketplaceStorageService;

	protected IUserFavoritesService favoritesService;

	@Before
	public void setUp() throws Exception {
		assureService();
		marketplaceStorageService = ServiceHelper.getMarketplaceServiceLocator().getDefaultStorageService();
		assertNotNull(marketplaceStorageService);
		assertEquals(USERSTORAGE_SERVICE_URI, marketplaceStorageService.getServiceUri());
		//marketplaceStorageService.login("eclipse_test_123456789", "plaintext123456789");
		marketplaceStorageService.getStorage().setCredentialsProvider(new ICredentialsProvider() {

			@Override
			public Credentials provideCredentials(IStorageService service, boolean reauthentication) {
				//return new Credentials("eclipse_test_123456789", "plaintext123456789");//WIP
				return new Credentials("reckord@yatta.de", "FAC26w05,1E6X92>44~_11Ox");
			}

		});
		favoritesService = ServiceHelper.getMarketplaceServiceLocator().getDefaultFavoritesService();
		assertNotNull(favoritesService);
		assertSame(marketplaceStorageService, favoritesService.getStorageService());
	}

	private void assureService() {
		StorageService service = StorageServiceRegistry.INSTANCE.getService(USERSTORAGE_SERVICE_URI);
		if (service == null) {
			StorageServiceRegistry.INSTANCE.addService("Staging", USERSTORAGE_SERVICE_URI);
			service = StorageServiceRegistry.INSTANCE.getService(USERSTORAGE_SERVICE_URI);
		}
		assertNotNull(service);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetFavorites() throws Exception {
		//marketplaceStorageService.get
		List<INode> favorites = favoritesService.getFavorites();
		for (INode node : favorites) {
			System.out.println("Favorite: " + node.getId());
		}
	}

	@Test
	public void testToggleFavorite() throws Exception {
		INode favNode = QueryHelper.nodeById("12345");
		favoritesService.setFavorite(favNode, true);
		List<INode> favorites = favoritesService.getFavorites();
		assertNotNull(QueryHelper.findById(favorites, favNode));

		favoritesService.setFavorite(favNode, false);
		favorites = favoritesService.getFavorites();
		assertNull(QueryHelper.findById(favorites, favNode));
	}

}
