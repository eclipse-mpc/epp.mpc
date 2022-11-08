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
package org.eclipse.epp.mpc.tests.ui.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.model.Iu;
import org.eclipse.epp.internal.mpc.core.model.Ius;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.model.SearchResult;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeInstallableUnitItem;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.IIus;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.junit.Before;
import org.junit.Test;

public class MarketplaceCatalogTest {

	protected Set<INode> installedNodes;

	protected Set<INode> updateAvailable;

	protected Set<INode> checkedForUpdate;
	protected List<Node> discoveryNodes;

	private MarketplaceCatalog catalog;

	@Before
	public void setUp() throws Exception {
		installedNodes = new HashSet<>();
		updateAvailable = new HashSet<>();
		checkedForUpdate = new HashSet<>();
		discoveryNodes = new ArrayList<>();

		setupNodes();
		setupCatalog();
	}

	protected void setupNodes() {
		Node node = new Node();
		node.setName("Not installed");
		node.setId("1001");
		node.setIus(new Ius());
		node.getIus().getIuElements().add(new Iu("org.example.notinstalled.iu"));
		discoveryNodes.add(node);

		node = new Node();
		node.setName("Up to date");
		node.setId("1002");
		node.setIus(new Ius());
		node.getIus().getIuElements().add(new Iu("org.example.installed.iu"));
		discoveryNodes.add(node);
		installedNodes.add(node);

		node = new Node();
		node.setName("Update available");
		node.setId("1003");
		node.setIus(new Ius());
		node.getIus().getIuElements().add(new Iu("org.example.updateable.iu"));
		discoveryNodes.add(node);
		installedNodes.add(node);
		updateAvailable.add(node);
	}

	protected void setupCatalog() throws MalformedURLException {
		final SearchResult discoveryResult = new SearchResult();
		discoveryResult.setNodes(discoveryNodes);

		CatalogDescriptor catalogDescriptor = new CatalogDescriptor();
		catalogDescriptor.setUrl(new URL("https://marketplace.eclipse.org"));

		MarketplaceDiscoveryStrategy discoveryStrategy = new MarketplaceDiscoveryStrategy(catalogDescriptor) {
			final MarketplaceCategory category = new MarketplaceCategory();
			{
				category.setId("<root>");
			}
			@Override
			public void performDiscovery(IProgressMonitor monitor) throws CoreException {
				if (!categories.contains(category)) {
					categories.add(category);
				}
				handleSearchResult(category, discoveryResult, new NullProgressMonitor());
			}

			@Override
			protected synchronized Map<String, IInstallableUnit> computeInstalledIUs(IProgressMonitor monitor) {
				Map<String, IInstallableUnit> installedIus = new HashMap<>();
				for (INode node : installedNodes) {
					IIus ius = node.getIus();
					if (ius != null) {
						for (IIu iu : ius.getIuElements()) {
							String featureId = iu.getId() + ".feature.group";
							InstallableUnit installableUnit = new InstallableUnit();
							installableUnit.setId(featureId);
							installedIus.put(featureId, installableUnit);
						}
					}
				}
				return installedIus;
			}

			@Override
			protected MarketplaceCategory findMarketplaceCategory(IProgressMonitor monitor) throws CoreException {
				return category;
			}
		};

		catalog = new MarketplaceCatalog() {
			@Override
			protected IStatus checkForUpdates(List<MarketplaceNodeCatalogItem> updateCheckNeeded,
					final Map<String, IInstallableUnit> installedIUs, final IProgressMonitor monitor) {
				for (MarketplaceNodeCatalogItem item : updateCheckNeeded) {
					checkedForUpdate.add(item.getData());
					List<MarketplaceNodeInstallableUnitItem> installableUnitItems = item.getInstallableUnitItems();
					boolean hasUpdate = updateAvailable.contains(item.getData());
					for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
						iuItem.setUpdateAvailable(hasUpdate);
					}
				}
				return Status.OK_STATUS;
			}
		};
		catalog.getDiscoveryStrategies().add(discoveryStrategy);
	}

	@Test
	public void testCheckUpdateOnPerformDiscovery() {
		assertTrue(checkedForUpdate.isEmpty());
		catalog.performDiscovery(new NullProgressMonitor());
		List<CatalogItem> items = catalog.getItems();
		assertEquals(discoveryNodes.size(), items.size());
		assertEquals(installedNodes.size(), checkedForUpdate.size());

		int updateable = 0;
		for (CatalogItem item : items) {
			Boolean hasUpdate = ((MarketplaceNodeCatalogItem) item).getUpdateAvailable();
			if (installedNodes.contains(item.getData())) {
				assertNotNull(hasUpdate);
			} else {
				assertNull(hasUpdate);
			}
			assertEquals(updateAvailable.contains(item.getData()), Boolean.TRUE.equals(hasUpdate));
			if (Boolean.TRUE.equals(hasUpdate)) {
				updateable++;
			}
		}
		assertEquals(updateAvailable.size(), updateable);
	}
}
