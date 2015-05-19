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
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.io.IOException;
import java.net.URL;

import org.eclipse.epp.internal.mpc.ui.catalog.ResourceProvider.ResourceFuture;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

/**
 * @author Carsten Reckord 
 */
public class ResourceProviderImageDescriptor extends ImageDescriptor {

	private final String resourceName;

	private final ResourceProvider resourceProvider;

	private ImageDescriptor delegate;

	private boolean createDelegatePerformed = false;

	public ResourceProviderImageDescriptor(ResourceProvider resourceProvider, String resourceName) {
		super();
		this.resourceProvider = resourceProvider;
		this.resourceName = resourceName;
	}

	public String getResourceName() {
		return resourceName;
	}

	public ResourceProvider getResourceProvider() {
		return resourceProvider;
	}

	private ImageDescriptor getDelegate() {
		if (delegate == null && !createDelegatePerformed) {
			delegate = createDelegate();
			createDelegatePerformed = true;
		}
		return delegate;
	}

	private ImageDescriptor createDelegate() {
		ResourceFuture resource = resourceProvider.getResource(resourceName);
		if (resource != null) {
			//this blocks until the remote is fully downloaded - that's the best we can do at this point...
			URL url;
			try {
				url = resource.getURL();
			} catch (IOException e) {
				//already logged
				return null;
			}
			return createUrlDescriptor(url);
		}
		return null;
	}

	private ImageDescriptor createUrlDescriptor(URL url) {
		return ImageDescriptor.createFromURL(url);
	}

	@Override
	public Object createResource(Device device) throws DeviceResourceException {
		ImageDescriptor delegate = getDelegate();
		return delegate == null ? super.createResource(device) : delegate.createResource(device);
	}

	@Override
	public void destroyResource(Object previouslyCreatedObject) {
		ImageDescriptor delegate = getDelegate();
		if (delegate != null) {
			delegate.destroyResource(previouslyCreatedObject);
		} else {
			super.destroyResource(previouslyCreatedObject);
		}
	}

	@Override
	public Image createImage() {
		ImageDescriptor delegate = getDelegate();
		return delegate == null ? super.createImage() : delegate.createImage();
	}

	@Override
	public Image createImage(boolean returnMissingImageOnError) {
		ImageDescriptor delegate = getDelegate();
		return delegate == null ? super.createImage(returnMissingImageOnError)
				: delegate.createImage(returnMissingImageOnError);
	}

	@Override
	public Image createImage(Device device) {
		ImageDescriptor delegate = getDelegate();
		return delegate == null ? super.createImage(device) : delegate.createImage(device);
	}

	@Override
	public Image createImage(boolean returnMissingImageOnError, Device device) {
		ImageDescriptor delegate = getDelegate();
		return delegate == null ? super.createImage(returnMissingImageOnError, device) : delegate.createImage(
				returnMissingImageOnError, device);
	}

	@Override
	public ImageData getImageData() {
		ImageDescriptor delegate = getDelegate();
		return delegate == null ? null : delegate.getImageData();
	}

}
