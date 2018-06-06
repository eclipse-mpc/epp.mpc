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
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.equinox.internal.p2.discovery.model.AbstractCatalogItem;

public class MarketplaceNodeInstallableUnitItem extends AbstractCatalogItem {

	private static final String DOT_FEATURE_DOT_GROUP = ".feature.group"; //$NON-NLS-1$

	private MarketplaceNodeCatalogItem catalogItem;

	private String id;

	private Boolean available;

	private Boolean installed;

	private Boolean updateAvailable;

	private Boolean optional;

	private Boolean defaultSelected;

	public void init(IIu iu) {
		id = iu.getId();
		if (!id.endsWith(DOT_FEATURE_DOT_GROUP)) {
			id = id + DOT_FEATURE_DOT_GROUP;
		}
		optional = iu.isOptional();
		defaultSelected = !optional || iu.isSelected();
	}

	public MarketplaceNodeCatalogItem getCatalogItem() {
		return catalogItem;
	}

	public void setCatalogItem(MarketplaceNodeCatalogItem catalogItem) {
		this.catalogItem = catalogItem;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Boolean getAvailable() {
		return available;
	}

	public void setAvailable(Boolean available) {
		this.available = available;
	}

	public Boolean getInstalled() {
		return installed;
	}

	public void setInstalled(Boolean installed) {
		this.installed = installed;
	}

	public Boolean getUpdateAvailable() {
		return updateAvailable;
	}

	public void setUpdateAvailable(Boolean updateAvailable) {
		this.updateAvailable = updateAvailable;
	}

	public Boolean getOptional() {
		return optional;
	}

	public void setOptional(Boolean optional) {
		this.optional = optional;
	}

	public Boolean getDefaultSelected() {
		return defaultSelected;
	}

	public void setDefaultSelected(Boolean defaultSelected) {
		this.defaultSelected = defaultSelected;
	}

	public boolean isOptional() {
		return optional == null || Boolean.TRUE.equals(optional);//consider features optional by default
	}

	public boolean isDefaultSelected() {
		return (defaultSelected == null && (optional == null || Boolean.FALSE.equals(optional)))
				|| (defaultSelected != null && Boolean.TRUE.equals(defaultSelected));
	}
}
