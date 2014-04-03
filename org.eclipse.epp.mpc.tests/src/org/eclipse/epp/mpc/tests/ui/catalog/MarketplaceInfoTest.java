/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.catalog;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.epp.internal.mpc.core.service.Ius;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceInfo;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.core.model.INode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Test {@link MarketplaceInfo}
 *
 * @author David Green
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class MarketplaceInfoTest {

	private MarketplaceNodeCatalogItem item;

	private MarketplaceInfo catalogRegistry;

	@Before
	public void before() throws Exception {
		catalogRegistry = new MarketplaceInfo();

		item = new MarketplaceNodeCatalogItem();
		item.setMarketplaceUrl(new URL("http://marketplace.eclipse.org"));
		item.setId("123");
		Node node = new Node();
		node.setId(item.getId());
		node.setUrl("http://marketplace.eclipse.org/node/" + node.getId());
		node.setIus(new Ius());
		node.getIus().getIu().add("com.example.test.a1");
		node.getIus().getIu().add("com.example.test.a2");
		item.setData(node);
		item.setInstallableUnits(node.getIus().getIu());
	}

	@Test
	public void addMapCatalogNode() {
		assertEquals(0, catalogRegistry.getNodeKeyToIU().size());
		catalogRegistry.map(item.getMarketplaceUrl(), item.getData());

		assertEquals(1, catalogRegistry.getNodeKeyToIU().size());
		String key = catalogRegistry.getNodeKeyToIU().keySet().iterator().next();

		List<String> list = catalogRegistry.getNodeKeyToIU().get(key);
		assertNotNull(list);
		assertTrue(list.containsAll(item.getInstallableUnits()));
		assertEquals(item.getInstallableUnits().size(), list.size());

		for (String iu : item.getInstallableUnits()) {
			List<String> nodes = catalogRegistry.getIuToNodeKey().get(iu);
			assertNotNull(nodes);
			assertTrue(nodes.contains(key));
		}
	}

	@Test
	public void computeInstalledCatalogNodeIds() {
		assertTrue(item.getInstallableUnits().size() > 1);
		assertEquals(0, catalogRegistry.getNodeKeyToIU().size());
		catalogRegistry.map(item.getMarketplaceUrl(), item.getData());

		assertEquals(1, catalogRegistry.getNodeKeyToIU().size());

		Set<String> installedIus = new HashSet<String>();
		installedIus.add("com.foo.bar");

		Set<? extends INode> installedCatalogNodeIds = catalogRegistry.computeInstalledNodes(item.getMarketplaceUrl(),
				installedIus);
		assertNotNull(installedCatalogNodeIds);
		assertEquals(0, installedCatalogNodeIds.size());

		installedIus.add(item.getInstallableUnits().get(0));

		installedCatalogNodeIds = catalogRegistry.computeInstalledNodes(item.getMarketplaceUrl(), installedIus);
		assertNotNull(installedCatalogNodeIds);
		assertEquals(1, installedCatalogNodeIds.size());

		for (String iu : item.getInstallableUnits()) {
			installedIus.add(iu);
		}

		installedCatalogNodeIds = catalogRegistry.computeInstalledNodes(item.getMarketplaceUrl(), installedIus);
		assertNotNull(installedCatalogNodeIds);
		assertEquals(1, installedCatalogNodeIds.size());

		assertEquals(item.getId(), installedCatalogNodeIds.iterator().next().getId());

	}

	@Test
	public void computeInstalled() {
		assertTrue(item.getInstallableUnits().size() > 1);
		assertEquals(0, catalogRegistry.getNodeKeyToIU().size());
		catalogRegistry.map(item.getMarketplaceUrl(), item.getData());

		assertEquals(1, catalogRegistry.getNodeKeyToIU().size());

		Set<String> installedIus = new HashSet<String>();
		installedIus.add("com.foo.bar");

		boolean isInstalled = catalogRegistry.computeInstalled(installedIus, item.getData());
		assertFalse(isInstalled);

		installedIus.addAll(item.getInstallableUnits());
		isInstalled = catalogRegistry.computeInstalled(installedIus, item.getData());
		assertTrue(isInstalled);

		installedIus.clear();
		installedIus.add(item.getInstallableUnits().get(0));
		isInstalled = catalogRegistry.computeInstalled(installedIus, item.getData());
		assertTrue(isInstalled);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void computeInstalledLegacy() throws Exception {
		Node node = (Node) item.getData();

		assertTrue(item.getInstallableUnits().size() > 1);
		assertEquals(0, catalogRegistry.getNodeKeyToIU().size());
		catalogRegistry.map(item.getMarketplaceUrl(), node);
		assertEquals(1, catalogRegistry.getNodeKeyToIU().size());

		URI updateUri = new URI("http://update.example.org");
		node.setUpdateurl(updateUri.toString());
		Set<String> installedIus = new HashSet<String>();
		installedIus.addAll(item.getInstallableUnits());

		boolean isInstalled = catalogRegistry.computeInstalled(installedIus, Collections.singleton(new URI(
				"http://other.example.org")), node);
		assertFalse(isInstalled);

		isInstalled = catalogRegistry.computeInstalled(installedIus, Collections.singleton(updateUri), node);
		assertTrue(isInstalled);

		node.setUpdateurl(null);
		isInstalled = catalogRegistry.computeInstalled(installedIus, Collections.singleton(updateUri), node);
		assertFalse(isInstalled);
	}
}
