/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.epp.internal.mpc.core.service.CatalogBranding;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;

/**
 * @author David Green
 */
public class CatalogRegistry {

	private static CatalogRegistry instance;

	public synchronized static CatalogRegistry getInstance() {
		if (instance == null) {
			instance = new CatalogRegistry();
		}
		return instance;
	}

	private final List<CatalogDescriptor> catalogDescriptors = new CopyOnWriteArrayList<CatalogDescriptor>();

	private final Map<CatalogDescriptor, CatalogBranding> catalogBrandings = new HashMap<CatalogDescriptor, CatalogBranding>();

	public CatalogRegistry() {
		catalogDescriptors.addAll(new CatalogExtensionPointReader().getCatalogDescriptors());
	}

	public void register(CatalogDescriptor catalogDescriptor) {
		catalogDescriptors.add(new CatalogDescriptor(catalogDescriptor));
	}

	public void unregister(CatalogDescriptor catalogDescriptor) {
		catalogDescriptors.remove(catalogDescriptor);
	}

	public List<CatalogDescriptor> getCatalogDescriptors() {
		return Collections.unmodifiableList(catalogDescriptors);
	}

	// TODO: remove and integrate into CatalogDescriptor once we are not in API freeze
	public void addCatalogBranding(CatalogDescriptor descriptor, CatalogBranding branding) {
		catalogBrandings.put(descriptor, branding);
	}

	public CatalogBranding getCatalogBranding(CatalogDescriptor descriptor) {
		return catalogBrandings.get(descriptor);
	}

	public CatalogDescriptor findCatalogDescriptor(String url) {
		if (url == null || url.length() == 0) {
			return null;
		}
		for (CatalogDescriptor catalogDescriptor : catalogDescriptors) {
			if (url.startsWith(catalogDescriptor.getUrl().toExternalForm())) {
				return catalogDescriptor;
			}
		}
		return null;
	}
}
