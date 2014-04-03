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

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.core.service.SearchResult;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class MarketplaceDiscoveryStrategyTest {

	private MarketplaceDiscoveryStrategy discoveryStrategy;

	private MarketplaceCatalog catalog;

	private URL catalogUrl;

	private CatalogDescriptor catalogDescriptor;

	@Before
	public void setUp() throws Exception {
		catalogUrl = new URL("http://marketplace.eclipse.org");
		catalogDescriptor = new CatalogDescriptor(catalogUrl, "Eclipse.org Marketplace");
		CatalogRegistry.getInstance().register(catalogDescriptor);
		catalog = new MarketplaceCatalog();
	}

	private void setupCatalog(final IMarketplaceService marketplaceService) {
		discoveryStrategy = new MarketplaceDiscoveryStrategy(catalogDescriptor) {
			@Override
			public IMarketplaceService createMarketplaceService() {
				return marketplaceService;
			}
		};
		catalog.getDiscoveryStrategies().clear();
		catalog.getDiscoveryStrategies().add(discoveryStrategy);
	}

	@After
	public void tearDown() throws Exception {
		catalog.dispose();
		CatalogRegistry.getInstance().unregister(catalogDescriptor);
	}

	@Test
	public void testSearchByNodeUrl() throws Exception {
		final INode[] testNode = new INode[1];
		final IMarketplaceService marketplaceService = new DefaultMarketplaceService(catalogUrl) {
			@Override
			public Node getNode(INode node, IProgressMonitor monitor) throws CoreException {
				testNode[0] = node;
				return (Node) node;
			}

			@Override
			public SearchResult search(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
					throws CoreException {
				Assert.fail("Unexpected invocation");
				return null;//dead code
			}
		};
		setupCatalog(marketplaceService);

		testNode[0] = null;
		catalog.performQuery(null, null, new URL(catalogUrl, "content/test").toExternalForm(),
				new NullProgressMonitor());
		assertNotNull(testNode[0]);
		assertNotNull(testNode[0].getUrl());

		testNode[0] = null;
		catalog.performQuery(null, null, new URL(catalogUrl, "node/12345").toExternalForm(), new NullProgressMonitor());
		assertNotNull(testNode[0]);
		assertNotNull(testNode[0].getId());
	}
}
