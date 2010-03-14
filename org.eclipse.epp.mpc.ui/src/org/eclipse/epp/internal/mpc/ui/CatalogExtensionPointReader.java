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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;

/**
 * Read catalog descriptors from the <tt>org.eclipse.epp.mpc.ui.catalog</tt> extension point.
 * 
 * @author David Green
 */
class CatalogExtensionPointReader {

	public List<CatalogDescriptor> getCatalogDescriptors() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint("org.eclipse.epp.mpc.ui",
				"catalog");
		if (extensionPoint == null) {
			throw new IllegalStateException();
		}
		List<CatalogDescriptor> descriptors = new ArrayList<CatalogDescriptor>();
		for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
			if (element.getName().equals("catalog")) {
				try {
					String urlText = element.getAttribute("url");
					if (urlText == null || urlText.trim().length() == 0) {
						throw new Exception("Must specify url");
					}
					URL url = new URL(urlText);
					String label = element.getAttribute("label");
					if (label == null || label.trim().length() == 0) {
						throw new Exception("Must specify label");
					}
					CatalogDescriptor descriptor = new CatalogDescriptor(url, label);
					descriptor.setDescription(element.getAttribute("description"));
					final String icon = element.getAttribute("icon");
					if (icon != null) {
						URL iconResource = Platform.getBundle(element.getContributor().getName()).getResource(icon);
						if (iconResource == null) {
							throw new Exception(NLS.bind("Cannot find resourcce {0}", icon));
						}
						descriptor.setIcon(ImageDescriptor.createFromURL(iconResource));
					}
					if (!descriptors.contains(descriptor)) {
						descriptors.add(descriptor);
					}
				} catch (Exception e) {
					MarketplaceClientUi.error(NLS.bind("Cannot register catalog for bundle {0}: {1}",
							element.getContributor().getName(), e.getMessage()), e);
				}
			}
		}
		return descriptors;
	}
}
