/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import org.eclipse.epp.mpc.core.model.ICatalog;


/**
 * @author Benjamin Muskalla
 */
public class Catalog extends Identifiable implements ICatalog {

	private boolean selfContained;

	private String description;

	private String imageUrl;

	private CatalogBranding branding;

	private String dependencyRepository;

	private News news;

	public Catalog() {
	}

	public boolean isSelfContained() {
		return selfContained;
	}

	public void setSelfContained(boolean selfContained) {
		this.selfContained = selfContained;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public CatalogBranding getBranding() {
		return branding;
	}

	public void setBranding(CatalogBranding branding) {
		this.branding = branding;
	}

	public String getDependencyRepository() {
		return dependencyRepository;
	}

	public void setDependencyRepository(String dependencyRepository) {
		this.dependencyRepository = dependencyRepository;
	}

	public News getNews() {
		return news;
	}

	public void setNews(News news) {
		this.news = news;
	}

	@Override
	protected boolean equalsType(Object obj) {
		return obj instanceof ICatalog;
	}
}
