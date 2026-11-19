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
package org.eclipse.epp.mpc.tests.ui.wizard;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.model.SearchResult;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MarketplaceDiscoveryStrategyTest {

	private MarketplaceDiscoveryStrategy discoveryStrategy;

	private MarketplaceCatalog catalog;

	private URL catalogUrl;

	private CatalogDescriptor catalogDescriptor;

	@BeforeEach
	public void setUp() throws Exception {
		catalogUrl = new URI("https://marketplace.eclipse.org").toURL();
		catalogDescriptor = new CatalogDescriptor(catalogUrl, "Eclipse.org Marketplace");
		CatalogRegistry.getInstance().register(catalogDescriptor);
		catalog = new MarketplaceCatalog();
	}

	private void setupCatalog(final IMarketplaceService testService) {
		discoveryStrategy = new MarketplaceDiscoveryStrategy(catalogDescriptor) {
			@Override
			public IMarketplaceService acquireMarketplaceService() {
				return testService;
			}
		};
		catalog.getDiscoveryStrategies().clear();
		catalog.getDiscoveryStrategies().add(discoveryStrategy);
	}

	@AfterEach
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
				Assertions.fail("Unexpected invocation");
				return null;//dead code
			}
		};
		setupCatalog(marketplaceService);

		testNode[0] = null;
		catalog.performQuery(null, null, catalogUrl.toURI().resolve("content/test").toString(),
				new NullProgressMonitor());
		assertNotNull(testNode[0]);
		assertNotNull(testNode[0].getUrl());

		testNode[0] = null;
		catalog.performQuery(null, null, catalogUrl.toURI().resolve("node/12345").toString(),
				new NullProgressMonitor());
		assertNotNull(testNode[0]);
		assertNotNull(testNode[0].getId());
	}
}
