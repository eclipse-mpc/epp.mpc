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
package org.eclipse.epp.mpc.ui;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A descriptor for identifying a solutions catalog, ie: a location that implements the Eclipse Marketplace API.
 * 
 * @author David Green
 * @see MarketplaceClientUI#addCatalogDescriptor(CatalogDescriptor)
 */
public final class CatalogDescriptor {
	private URL url;

	private String label;

	private String description;

	private ImageDescriptor icon;

	public CatalogDescriptor() {
	}

	/**
	 * @param url
	 *            The URL of the catalog. See {@link #getIcon()}
	 * @param label
	 *            The label identifying the catalog. See {@link #getLabel()}
	 */
	public CatalogDescriptor(URL url, String label) {
		this.url = url;
		this.label = label;
	}

	public CatalogDescriptor(CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		this.url = catalogDescriptor.url;
		this.label = catalogDescriptor.label;
		this.description = catalogDescriptor.description;
		this.icon = catalogDescriptor.icon;
	}

	/**
	 * The URL of the catalog. The URL identifies the catalog location, which provides an API described by <a
	 * href="http://FIXME">Marketplace REST</a>
	 */
	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	/**
	 * A description of the catalog, presented to the user. Should be brief (ie: one or two sentences).
	 * 
	 * @return the description or null if there is no description
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * An icon to be used in branding the catalog. Must refer to an image of size 32x32
	 */
	public ImageDescriptor getIcon() {
		return icon;
	}

	public void setIcon(ImageDescriptor icon) {
		this.icon = icon;
	}

	/**
	 * The label that identifies the catalog. Presented to the user, should be no more than a few words.
	 */
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CatalogDescriptor other = (CatalogDescriptor) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

}
