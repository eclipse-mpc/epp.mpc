/*******************************************************************************
 * Copyright (c) 2010, 2020 The Eclipse Foundation and others.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatalogDescriptorTest {

	@Before
	@After
	public void cleanRegistry() {
		CatalogRegistry registry = CatalogRegistry.getInstance();
		List<CatalogDescriptor> catalogs = new ArrayList<>(registry.getCatalogDescriptors());
		for (CatalogDescriptor descriptor : catalogs) {
			registry.unregister(descriptor);
		}
	}

	@Test
	public void testCatalogDescriptor() throws Exception {
		URL dependenciesRepository = new URL("https://marketplace.eclipse.org/repo/");
		String description = "some description";
		ImageDescriptor icon = ImageDescriptor.getMissingImageDescriptor();
		boolean installFromAllRepositories = true;
		String label = "some label";
		URL url = new URL("https://marketplace.eclipse.org");

		CatalogDescriptor descriptor = new CatalogDescriptor();
		descriptor.setDependenciesRepository(dependenciesRepository);
		descriptor.setDescription(description);
		descriptor.setIcon(icon);
		descriptor.setInstallFromAllRepositories(installFromAllRepositories);
		descriptor.setLabel(label);
		descriptor.setUrl(url);

		assertEquals(dependenciesRepository.toString(), descriptor.getDependenciesRepository().toString());
		assertEquals(description, descriptor.getDescription());
		assertEquals(icon, descriptor.getIcon());
		assertEquals(installFromAllRepositories, descriptor.isInstallFromAllRepositories());
		assertEquals(label, descriptor.getLabel());
		assertEquals(url.toString(), descriptor.getUrl().toString());

		CatalogDescriptor copyDescriptor = new CatalogDescriptor(descriptor);
		assertEquals(dependenciesRepository.toString(), copyDescriptor.getDependenciesRepository().toString());
		assertEquals(description, copyDescriptor.getDescription());
		assertEquals(icon, copyDescriptor.getIcon());
		assertEquals(installFromAllRepositories, copyDescriptor.isInstallFromAllRepositories());
		assertEquals(label, copyDescriptor.getLabel());
		assertEquals(url.toString(), copyDescriptor.getUrl().toString());

		assertTrue(descriptor.equals(copyDescriptor));
		assertEquals(descriptor.hashCode(), copyDescriptor.hashCode());

	}

	@Test
	public void testCatalogDescriptorURLString() throws Exception {
		String label = "some label";
		URL url = new URL("https://marketplace.eclipse.org");
		CatalogDescriptor descriptor = new CatalogDescriptor(url, label);

		assertEquals(label, descriptor.getLabel());
		assertEquals(url.toString(), descriptor.getUrl().toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCopyCtorNull() {
		new CatalogDescriptor((CatalogDescriptor) null);
	}

	/**
	 * @throws Exception
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=488436">bug 488436</a>
	 */
	@Test
	public void testFindCatalogDescriptorWithCommonPrefix() throws Exception {
		CatalogRegistry registry = CatalogRegistry.getInstance();
		String marketplace = "https://marketplace.eclipse.org";
		String hostedMarketplace = "https://marketplace.eclipse.org/hosted_catalog/test";
		CatalogDescriptor marketplaceDescriptor = new CatalogDescriptor(new URL(marketplace), null);
		CatalogDescriptor hostedMarketplaceDescriptor = new CatalogDescriptor(new URL(hostedMarketplace), null);
		registry.register(marketplaceDescriptor);
		registry.register(hostedMarketplaceDescriptor);

		CatalogDescriptor found = registry.findCatalogDescriptor(marketplace);
		assertThat(found, is(marketplaceDescriptor));

		found = registry.findCatalogDescriptor(hostedMarketplace);
		assertThat(found, is(hostedMarketplaceDescriptor));
	}

	/**
	 * @throws Exception
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=488436">bug 488436</a>
	 */
	@Test
	public void testFindCatalogDescriptorWithCommonPrefixDifferentProtocol1() throws Exception {
		CatalogRegistry registry = CatalogRegistry.getInstance();
		String marketplace = "https://marketplace.eclipse.org";
		String marketplaceHttps = URLUtil.toggleHttps(marketplace);
		String hostedMarketplace = "https://marketplace.eclipse.org/hosted_catalog/test";
		String hostedMarketplaceHttp = URLUtil.toggleHttps(hostedMarketplace);

		CatalogDescriptor marketplaceDescriptor = new CatalogDescriptor(new URL(marketplace), null);
		CatalogDescriptor hostedMarketplaceDescriptor = new CatalogDescriptor(new URL(hostedMarketplace), null);
		registry.register(marketplaceDescriptor);
		registry.register(hostedMarketplaceDescriptor);

		CatalogDescriptor found = registry.findCatalogDescriptor(marketplaceHttps);
		assertThat(found, is(marketplaceDescriptor));

		found = registry.findCatalogDescriptor(hostedMarketplaceHttp);
		assertThat(found, is(hostedMarketplaceDescriptor));
	}

	/**
	 * @throws Exception
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=488436">bug 488436</a>
	 */
	@Test
	public void testFindCatalogDescriptorWithCommonPrefixDifferentProtocol2() throws Exception {
		CatalogRegistry registry = CatalogRegistry.getInstance();
		String marketplace = "https://marketplace.eclipse.org";
		String marketplaceHttp = URLUtil.toggleHttps(marketplace);
		String hostedMarketplace = "https://marketplace.eclipse.org/hosted_catalog/test";
		String hostedMarketplaceHttps = URLUtil.toggleHttps(hostedMarketplace);

		CatalogDescriptor marketplaceDescriptor = new CatalogDescriptor(new URL(marketplace), null);
		CatalogDescriptor hostedMarketplaceDescriptor = new CatalogDescriptor(new URL(hostedMarketplace), null);
		registry.register(marketplaceDescriptor);
		registry.register(hostedMarketplaceDescriptor);

		CatalogDescriptor found = registry.findCatalogDescriptor(marketplaceHttp);
		assertThat(found, is(marketplaceDescriptor));

		found = registry.findCatalogDescriptor(hostedMarketplaceHttps);
		assertThat(found, is(hostedMarketplaceDescriptor));
	}

	@Test
	public void testFindCatalogDescriptorMatchingProtocol() throws Exception {
		CatalogRegistry registry = CatalogRegistry.getInstance();
		String marketplace = "https://marketplace.eclipse.org";
		String marketplaceHttps = URLUtil.toggleHttps(marketplace);
		CatalogDescriptor marketplaceDescriptor = new CatalogDescriptor(new URL(marketplace), null);
		CatalogDescriptor marketplaceHttpsDescriptor = new CatalogDescriptor(new URL(marketplaceHttps), null);
		registry.register(marketplaceHttpsDescriptor);
		registry.register(marketplaceDescriptor);

		CatalogDescriptor found = registry.findCatalogDescriptor(marketplace);
		assertThat(found, is(marketplaceDescriptor));

		found = registry.findCatalogDescriptor(marketplaceHttps);
		assertThat(found, is(marketplaceHttpsDescriptor));
	}

	@Test
	public void testIgnoreCatalogDescriptorWithDedicatedHostingSuffix() throws Exception {
		CatalogRegistry registry = CatalogRegistry.getInstance();
		String marketplace = "https://marketplace.eclipse.org";
		String dedicatedHostingMarketplace = marketplace + "/" + ICatalogService.DEDICATED_CATALOG_HOSTING_SEGMENT
				+ "test";
		CatalogDescriptor dedicatedHostingDescriptor = new CatalogDescriptor(new URL(dedicatedHostingMarketplace),
				null);
		registry.register(dedicatedHostingDescriptor);

		CatalogDescriptor found = registry.findCatalogDescriptor(marketplace);
		assertNull(found);

		found = registry.findCatalogDescriptor(dedicatedHostingMarketplace);
		assertThat(found, is(dedicatedHostingDescriptor));
	}
}
