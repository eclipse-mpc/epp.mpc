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
package org.eclipse.epp.mpc.tests.ui.wizard;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.wizards.InstallProfile;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModelStateSerializer;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class SelectionModelStateSerializerTest {

	private MarketplaceDiscoveryStrategy discoveryStrategy;

	private MarketplaceCatalog catalog;

	private SelectionModel selectionModel;

	@Before
	public void setUp() throws Exception {
		CatalogDescriptor catalogDescriptor = new CatalogDescriptor(new URL("http://marketplace.eclipse.org"),
				"Eclipse.org Marketplace");
		discoveryStrategy = new MarketplaceDiscoveryStrategy(catalogDescriptor) {
			@Override
			public IMarketplaceService createMarketplaceService() {
				DefaultMarketplaceService marketplaceService = new DefaultMarketplaceService(catalogDescriptor.getUrl());
				Map<String, String> requestMetaParameters = new HashMap<String, String>();
				requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_CLIENT, MarketplaceClientCore.BUNDLE_ID);
				marketplaceService.setRequestMetaParameters(requestMetaParameters);
				return marketplaceService;
			}
		};
		catalog = new MarketplaceCatalog();
		catalog.getDiscoveryStrategies().add(discoveryStrategy);

		selectionModel = new SelectionModel(new InstallProfile() {
			public Set<String> getInstalledFeatures() {
				return Collections.emptySet();
			}
		});
	}

	@After
	public void tearDown() throws Exception {
		catalog.dispose();
	}

	@Test
	public void testSerialize() {
		catalog.performQuery(null, null, "Mylyn", new NullProgressMonitor());
		assertFalse(catalog.getItems().isEmpty());
		assertTrue(catalog.getItems().size() > 3);

		CatalogItem firstItem = catalog.getItems().get(0);
		CatalogItem secondItem = catalog.getItems().get(1);
		selectionModel.select(firstItem, Operation.INSTALL);
		selectionModel.select(secondItem, Operation.INSTALL);

		SelectionModelStateSerializer serializer = new SelectionModelStateSerializer(catalog, selectionModel);
		String state = serializer.serialize();
		assertNotNull(state);
		assertFalse(state.trim().length() == 0);

		assertTrue(selectionModel.computeProvisioningOperationViable());

		selectionModel.clear();

		assertTrue(selectionModel.getItemToSelectedOperation().isEmpty());
		assertFalse(selectionModel.computeProvisioningOperationViable());

		serializer.deserialize(state, new NullProgressMonitor());

		assertEquals(2, selectionModel.getItemToSelectedOperation().size());
		assertTrue(selectionModel.computeProvisioningOperationViable());

		Map<CatalogItem, Operation> itemToOperation = selectionModel.getItemToSelectedOperation();
		assertEquals(Operation.INSTALL, itemToOperation.get(firstItem));
		assertEquals(Operation.INSTALL, itemToOperation.get(secondItem));
	}
}
