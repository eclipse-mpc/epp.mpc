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
package org.eclipse.epp.mpc.tests.ui.catalog;

import static org.junit.Assert.*;

import java.net.URL;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.junit.Test;

public class CatalogDescriptorTest {

	@Test
	public void testCatalogDescriptor() throws Exception {
		URL dependenciesRepository = new URL("http://marketplace.eclipse.org/repo/");
		String description = "some description";
		ImageDescriptor icon = ImageDescriptor.getMissingImageDescriptor();
		boolean installFromAllRepositories = true;
		String label = "some label";
		URL url = new URL("http://marketplace.eclipse.org");

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
		URL url = new URL("http://marketplace.eclipse.org");
		CatalogDescriptor descriptor = new CatalogDescriptor(url, label);

		assertEquals(label, descriptor.getLabel());
		assertEquals(url.toString(), descriptor.getUrl().toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCopyCtorNull() {
		new CatalogDescriptor((CatalogDescriptor) null);
	}
}
