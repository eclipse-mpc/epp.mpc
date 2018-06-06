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
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWTException;
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

	public void setImage(final ImageReceiver receiver, final AbstractCatalogSource discoverySource,
			final String imagePath, Image fallbackImage) {
		if (imagePath != null && imagePath.length() > 0) {
			Image image = null;
			if (discoverySource instanceof MarketplaceCatalogSource) {
				MarketplaceCatalogSource marketplaceSource = (MarketplaceCatalogSource) discoverySource;
				ResourceFuture resource = marketplaceSource.getResourceProvider().getResource(imagePath);
				if (resource != null) {
					URL localURL = resource.getLocalURL();
					if (localURL != null) {
						try {
							File imageFile = new File(
									new URI(localURL.getProtocol(), null, localURL.getPath(), null, null));
							if (imageFile.exists()) {
								image = safeCreateImage(imagePath, localURL);
							}
						} catch (URISyntaxException e) {
							logFailedLoadingImage(imagePath, localURL, e);
						}
					}
				}
			}
			if (image != null) {
				receiver.setImage(image);
			} else if (fallbackImage != null) {
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
						try {
							if (imagePath != null && imagePath.length() > 0) {
								final URL resource = discoverySource.getResource(imagePath);
								if (resource != null) {
									display.asyncExec(() -> {
										final Image image1 = safeCreateImage(imagePath, resource);
										if (image1 != null) {
											receiver.setImage(image1);
										}
									});
								}
							}
						} catch (Exception e) {
							MarketplaceClientUi.log(IStatus.WARNING,
									Messages.MarketplaceDiscoveryResources_FailedCreatingImage, imagePath,
									discoverySource.getId(), e);
							return Status.CANCEL_STATUS;//we don't want any additional logging or error popups...
						}
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		} else if (fallbackImage != null) {
			receiver.setImage(fallbackImage);
		}
	}

	private Image safeCreateImage(String imagePath, URL url) {
		try {
			ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
			Image image = resourceManager.createImage(descriptor);
			return image;
		} catch (DeviceResourceException ex) {
			logFailedLoadingImage(imagePath, url, ex);
		} catch (SWTException ex) {
			logFailedLoadingImage(imagePath, url, ex);
		}
		return null;
	}

	private void logFailedLoadingImage(String imagePath, URL url, Exception ex) {
		MarketplaceClientUi.log(IStatus.WARNING, Messages.MarketplaceDiscoveryResources_LoadImageError, imagePath, ex);
	}
}
