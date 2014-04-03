/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.net.URL;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * @author David Green
 */
public class MarketplaceNodeCatalogItem extends CatalogItem {

	private URL marketplaceUrl;

	private Boolean updateAvailable;

	@Override
	public INode getData() {
		return (INode) super.getData();
	}

	public URL getMarketplaceUrl() {
		return marketplaceUrl;
	}

	public void setMarketplaceUrl(URL marketplaceUrl) {
		this.marketplaceUrl = marketplaceUrl;
	}

	public Boolean getUpdateAvailable() {
		return updateAvailable;
	}

	public void setUpdateAvailable(Boolean updateAvailable) {
		this.updateAvailable = updateAvailable;
	}

	public Set<Operation> getAvailableOperations() {
		Set<Operation> available = EnumSet.noneOf(Operation.class);
		MarketplaceNodeCatalogItem catalogItem = (MarketplaceNodeCatalogItem) getData();
		if (!catalogItem.getInstallableUnits().isEmpty()) {
			if (isInstalled()) {
				available.add(Operation.UNINSTALL);
				if (maybeUpdateAvailable()) {
					available.add(Operation.UPDATE);
				}
			} else if (maybeAvailable()) {
				available.add(Operation.INSTALL);
			}
		}
		return available;
	}

	private boolean maybeAvailable() {
		Boolean available = getAvailable();
		return available == null || Boolean.TRUE.equals(available);
	}

	private boolean maybeUpdateAvailable() {
		Boolean updateAvailable = getUpdateAvailable();
		return updateAvailable == null || Boolean.TRUE.equals(updateAvailable);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

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
		MarketplaceNodeCatalogItem other = (MarketplaceNodeCatalogItem) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}
