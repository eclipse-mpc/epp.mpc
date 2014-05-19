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
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
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
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint("org.eclipse.epp.mpc.ui", //$NON-NLS-1$
				"catalog"); //$NON-NLS-1$
		if (extensionPoint == null) {
			throw new IllegalStateException();
		}
		List<CatalogDescriptor> descriptors = new ArrayList<CatalogDescriptor>();
		for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
			if (element.getName().equals("catalog")) { //$NON-NLS-1$
				try {
					String urlText = element.getAttribute("url"); //$NON-NLS-1$
					if (urlText == null || urlText.trim().length() == 0) {
						throw new Exception(Messages.CatalogExtensionPointReader_urlRequired);
					}
					URL url = URLUtil.toURL(urlText);
					String label = element.getAttribute("label"); //$NON-NLS-1$
					if (label == null || label.trim().length() == 0) {
						throw new Exception(Messages.CatalogExtensionPointReader_labelRequired);
					}
					CatalogDescriptor descriptor = new CatalogDescriptor(url, label);
					descriptor.setDescription(element.getAttribute("description")); //$NON-NLS-1$
					final String icon = element.getAttribute("icon"); //$NON-NLS-1$
					if (icon != null) {
						URL iconResource = Platform.getBundle(element.getContributor().getName()).getResource(icon);
						if (iconResource == null) {
							throw new Exception(NLS.bind(Messages.CatalogExtensionPointReader_cannotFindResource, icon));
						}
						descriptor.setIcon(ImageDescriptor.createFromURL(iconResource));
					}
					String selfContained = element.getAttribute("selfContained"); //$NON-NLS-1$
					if (selfContained == null || selfContained.trim().length() == 0) {
						selfContained = "true"; //$NON-NLS-1$
					} else {
						selfContained = selfContained.trim();
					}
					descriptor.setInstallFromAllRepositories(!Boolean.valueOf(selfContained));
					String dependenciesRepository = element.getAttribute("dependenciesRepository"); //$NON-NLS-1$
					if (dependenciesRepository != null && dependenciesRepository.trim().length() > 0) {
						URL repository = new URL(dependenciesRepository);
						repository.toURI(); // we do this later, so let's see the error now.
						descriptor.setDependenciesRepository(repository);
					}
					if (!descriptors.contains(descriptor)) {
						descriptors.add(descriptor);
					}
				} catch (Exception e) {
					MarketplaceClientUi.error(NLS.bind(
							Messages.CatalogExtensionPointReader_cannotRegisterCatalog_bundle_reason,
							element.getContributor().getName(), e.getMessage()), e);
				}
			}
		}
		return descriptors;
	}
}
