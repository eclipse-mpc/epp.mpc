/*******************************************************************************
 * Copyright (c) 2010, 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.model.ICatalogBranding;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A descriptor for identifying a solutions catalog, ie: a location that implements the Eclipse Marketplace API.
 *
 * @author David Green
 * @see org.eclipse.epp.mpc.core.model.ICatalog
 * @see MarketplaceClient#addCatalogDescriptor(CatalogDescriptor)
 */
public final class CatalogDescriptor {
	private URL url;

	private String label;

	private String description;

	private ImageDescriptor icon;

	private ICatalogBranding catalogBranding;

	private boolean installFromAllRepositories;

	private URL dependenciesRepository;

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
		this.catalogBranding = catalogDescriptor.catalogBranding;//FIXME we should create a defensive copy of this, too
		this.dependenciesRepository = catalogDescriptor.dependenciesRepository;
		this.installFromAllRepositories = catalogDescriptor.installFromAllRepositories;
	}

	public CatalogDescriptor(ICatalog catalog) throws MalformedURLException {
		setLabel(catalog.getName());
		setUrl(URLUtil.toURL(catalog.getUrl()));
		setIcon(ImageDescriptor.createFromURL(URLUtil.toURL(catalog.getImageUrl())));
		setDescription(catalog.getDescription());
		setInstallFromAllRepositories(!catalog.isSelfContained());
		if (catalog.getDependencyRepository() != null) {
			setDependenciesRepository(URLUtil.toURL(catalog.getDependencyRepository()));
		}
		setCatalogBranding(catalog.getBranding());
		if (catalog.getNews() != null) {
			CatalogRegistry.getInstance().addCatalogNews(this, catalog.getNews());
		}
	}

	/**
	 * The URL of the catalog. The URL identifies the catalog location, which provides an API described by <a
	 * href="http://wiki.eclipse.org/Marketplace/REST">Marketplace REST</a>
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

	/**
	 * Indicate if install operations are resolved against all repositories registered in the current workspace
	 * configuration. When false installation resolves only against repositories of the selected catalog items including
	 * repositories considered as default for the catalog. Currently there is no way to define catalog default
	 * repositories, however it is expected that this may change in the future. The default value is false.
	 *
	 * @return true if installation occurs from all repositories, otherwise false.
	 */
	public boolean isInstallFromAllRepositories() {
		return installFromAllRepositories;
	}

	/**
	 * @see #isInstallFromAllRepositories()
	 */
	public void setInstallFromAllRepositories(boolean installFromAllRepositories) {
		this.installFromAllRepositories = installFromAllRepositories;
	}

	/**
	 * An URL that points to a a software repository that can be used to resolve dependencies for solutions installed
	 * from this catalog. If multiple repositories are needed this URL can point to a composite repository.
	 */
	public URL getDependenciesRepository() {
		return dependenciesRepository;
	}

	/**
	 * @see #getDependenciesRepository()
	 */
	public void setDependenciesRepository(URL dependenciesRepository) {
		this.dependenciesRepository = dependenciesRepository;
	}

	/**
	 * Branding information controlling wizard title and icon and available tabs.
	 */
	public ICatalogBranding getCatalogBranding() {
		return catalogBranding;
	}

	/**
	 * @see #getCatalogBranding()
	 */
	public void setCatalogBranding(ICatalogBranding catalogBranding) {
		this.catalogBranding = catalogBranding;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.toString().hashCode());
		return result;
	}

	/**
	 * identity is determined by the {@link #getUrl()}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CatalogDescriptor other = (CatalogDescriptor) obj;
		if (!urlEquals(url, other.url)) {
			return false;
		}
		return true;
	}

	private static boolean urlEquals(URL url1, URL url2) {
		// bug 338399: test URL equality without doing DNS lookups
		if (url1 == url2) {
			return true;
		} else if (url1 == null) {
			return false;
		} else if (url2 == null) {
			return false;
		}
		try {
			return url1.toURI().equals(url2.toURI());
		} catch (URISyntaxException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return "CatalogDescriptor [url=" + url + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
