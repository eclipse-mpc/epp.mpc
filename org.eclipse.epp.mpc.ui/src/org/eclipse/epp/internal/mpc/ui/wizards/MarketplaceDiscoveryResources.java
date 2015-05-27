/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.Field;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalogSource;
import org.eclipse.epp.internal.mpc.ui.catalog.ResourceProvider.ResourceFuture;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * @author Carsten Reckord
 */
public class MarketplaceDiscoveryResources extends DiscoveryResources {

	public static interface ImageReceiver {
		void setImage(Image image);
	}

	private final Display display;

	private final ResourceManager resourceManager;

	private final boolean disposeResourceManager;

	public MarketplaceDiscoveryResources(Display display) {
		super(display);
		this.display = display;
		ResourceManager existingResourceManager = retrieveResourceManager(display);
		this.disposeResourceManager = existingResourceManager == null;
		this.resourceManager = existingResourceManager == null ? new LocalResourceManager(
				JFaceResources.getResources(display)) : existingResourceManager;
	}

	private ResourceManager retrieveResourceManager(Display display) {
		try {
			Field resourceManager = DiscoveryResources.class.getDeclaredField("resourceManager"); //$NON-NLS-1$
			try {
				resourceManager.setAccessible(true);
				return (ResourceManager) resourceManager.get(this);
			} finally {
				resourceManager.setAccessible(false);
			}
		} catch (Exception e) {
			MarketplaceClientUi.error(e);
			return null;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (disposeResourceManager) {
			resourceManager.dispose();
		}
	}

	public String getIconPath(Icon icon, int dimension, boolean fallback) {
		String imagePath;
		switch (dimension) {
		case 64:
			imagePath = icon.getImage64();
			if (imagePath != null || !fallback) {
				break;
			}
		case 48:
			imagePath = icon.getImage48();
			if (imagePath != null || !fallback) {
				break;
			}
		case 32:
			imagePath = icon.getImage32();
			break;
		default:
			throw new IllegalArgumentException();
		}
		return imagePath != null && imagePath.length() > 0 ? imagePath : null;
	}

	public Image getImage(AbstractCatalogSource discoverySource, String imagePath) {
		if (imagePath != null && imagePath.length() > 0) {
			URL resource = discoverySource.getResource(imagePath);
			if (resource != null) {
				ImageDescriptor descriptor = ImageDescriptor.createFromURL(resource);
				return resourceManager.createImage(descriptor);
			}
		}
		return null;
	}

	public void setImage(final ImageReceiver receiver, final AbstractCatalogSource discoverySource,
			final String imagePath, Image fallbackImage) {
		if (imagePath != null && imagePath.length() > 0) {
			if (discoverySource instanceof MarketplaceCatalogSource) {
				MarketplaceCatalogSource marketplaceSource = (MarketplaceCatalogSource) discoverySource;
				ResourceFuture resource = marketplaceSource.getResourceProvider().getResource(imagePath);
				if (resource != null) {
					URL localURL = resource.getLocalURL();
					if (localURL != null) {
						ImageDescriptor descriptor = ImageDescriptor.createFromURL(localURL);
						Image image = resourceManager.createImage(descriptor);
						receiver.setImage(image);
						return;
					}
				}
			}
			if (fallbackImage != null) {
				receiver.setImage(fallbackImage);
			}
			new Job(Messages.MarketplaceDiscoveryResources_retrievingImage) {

				{
					setPriority(INTERACTIVE);
					setUser(false);
					setSystem(true);
				}

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (!display.isDisposed()) {
						final Image image = getImage(discoverySource, imagePath);
						if (image != null) {
							display.asyncExec(new Runnable() {
								public void run() {
									receiver.setImage(image);
								}
							});
						}
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		} else if (fallbackImage != null) {
			receiver.setImage(fallbackImage);
		}
	}

}