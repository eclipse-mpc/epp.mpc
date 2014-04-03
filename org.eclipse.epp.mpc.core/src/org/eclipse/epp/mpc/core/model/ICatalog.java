/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.model;

import org.eclipse.epp.mpc.core.service.ICatalogService;

/**
 * A catalog describes an entry point to a marketplace server. Its {@link #getUrl() url} is the base of the Marketplace
 * REST API. It has a description, image and optional additional provider branding information, which can be used to
 * present the marketplace to users, e.g. in the Marketplace Wizard.
 *
 * @see ICatalogService
 * @see https://wiki.eclipse.org/Marketplace/REST
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICatalog extends IIdentifiable {

	/**
	 * If the catalog is self-contained, then a {@link INode node} installation will only use the node's
	 * {@link INode#getUpdateurl() update url} and the catalog's {@link #getDependencyRepository() dependency
	 * repository}. Otherwise all known repositories are consulted.
	 *
	 * @return true if this catalog is self-contained, false if all known repositories should be used during
	 *         installation
	 */
	boolean isSelfContained();

	/**
	 * @return this catalog's description suitable for presentation to the user.
	 */
	String getDescription();

	/**
	 * @return a URL to an image resource used to present this catalog in a catalog chooser. May be null.
	 */
	String getImageUrl();

	/**
	 * @return additional branding information
	 */
	ICatalogBranding getBranding();

	/**
	 * An optional URI to a repository from which dependencies may be installed, may be null.
	 *
	 * @return the URI to use for dependency resolution, or null.
	 */
	String getDependencyRepository();

	/**
	 * Each catalog can optionally point to a current news entry, e.g. to present the user with a newsletter.
	 *
	 * @return the current news entry for this catalog, or null.
	 */
	INews getNews();

}