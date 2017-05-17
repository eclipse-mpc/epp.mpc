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

import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * @author David Green
 */
public class MarketplaceNodeCatalogItem extends CatalogItem {

	private static final Field changeSupportField;

	static {
		Boolean accessible = null;
		Field field = null;
		try {
			field = CatalogItem.class.getDeclaredField("changeSupport"); //$NON-NLS-1$
			accessible = field.isAccessible();
			field.setAccessible(true);
		} catch (Exception e) {
			field = null;
			MarketplaceClientCore.error(Messages.MarketplaceNodeCatalogItem_changeSupportError, e);
		} finally {
			if (field != null && accessible != null && !accessible.equals(field.isAccessible())) {
				field.setAccessible(accessible);
			}
		}
		changeSupportField = field;
	}

	public MarketplaceNodeCatalogItem() {
		super();
		// ignore
	}

	private URL marketplaceUrl;

	private Boolean userFavorite;

	private List<MarketplaceNodeInstallableUnitItem> installableUnitItems = new ArrayList<MarketplaceNodeInstallableUnitItem>();

	private transient PropertyChangeSupport propertyChangeSupport;

	private PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = accessPropertyChangeSupport();
		}
		return propertyChangeSupport;
	}

	private synchronized PropertyChangeSupport accessPropertyChangeSupport() {
		Boolean accessible = null;
		try {
			accessible = changeSupportField.isAccessible();
			changeSupportField.setAccessible(true);
			PropertyChangeSupport changeSupport = (PropertyChangeSupport) changeSupportField.get(this);
			return changeSupport;
		} catch (Exception e) {
			MarketplaceClientCore.error(Messages.MarketplaceNodeCatalogItem_changeSupportAccessError, e);
		} finally {
			if (changeSupportField != null && accessible != null
					&& !accessible.equals(changeSupportField.isAccessible())) {
				changeSupportField.setAccessible(accessible);
			}
		}
		return null;
	}

	protected void firePropertyChange(String property, Object oldValue, Object newValue) {
		PropertyChangeSupport propertyChangeSupport = getPropertyChangeSupport();
		if (propertyChangeSupport != null) {
			propertyChangeSupport.firePropertyChange(property, oldValue, newValue);
		}
	}

	@Override
	public void setInstallableUnits(List<String> installableUnits) {
		super.setInstallableUnits(installableUnits);
		updateInstallableUnitItems();
	}

	private void updateInstallableUnitItems() {
		List<IIu> iuElements = getData().getIus().getIuElements();
		List<MarketplaceNodeInstallableUnitItem> installableUnitItems = new ArrayList<MarketplaceNodeInstallableUnitItem>();
		for (String iuId : installableUnits) {
			MarketplaceNodeInstallableUnitItem iuItem = getInstallableUnitItem(iuId);
			if (iuItem == null) {
				iuItem = new MarketplaceNodeInstallableUnitItem();
				iuItem.setId(iuId);
			}
			for (IIu iu : iuElements) {
				if (iu.getId().equals(iuId)) {
					iuItem.init(iu);
					break;
				}
			}
			installableUnitItems.add(iuItem);
		}
		doSetInstallableUnitItems(installableUnitItems);
	}

	public MarketplaceNodeInstallableUnitItem getInstallableUnitItem(String iuId) {
		if (installableUnitItems == null) {
			return null;
		}
		for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
			if (iuId.equals(iuItem.getId())) {
				return iuItem;
			}
		}
		return null;
	}

	public void setInstallableUnitItems(List<MarketplaceNodeInstallableUnitItem> installableUnitItems) {
		doSetInstallableUnitItems(new ArrayList<MarketplaceNodeInstallableUnitItem>(
				installableUnitItems));
		updateInstallableUnits();
	}

	private void doSetInstallableUnitItems(List<MarketplaceNodeInstallableUnitItem> items) {
		this.installableUnitItems = Collections.unmodifiableList(items);
	}

	private void updateInstallableUnits() {
		installableUnits.clear();
		for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
			installableUnits.add(iuItem.getId());
		}
		super.setInstallableUnits(installableUnits);
	}

	public List<MarketplaceNodeInstallableUnitItem> getInstallableUnitItems() {
		return installableUnitItems;
	}

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
		Boolean updateAvailable = false;
		List<MarketplaceNodeInstallableUnitItem> installableUnitItems = getInstallableUnitItems();
		for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
			Boolean iuUpdateAvailable = iuItem.getUpdateAvailable();
			if (iuUpdateAvailable == null) {
				updateAvailable = null;
			} else if (Boolean.TRUE.equals(iuUpdateAvailable)) {
				return true;
			}
		}
		return updateAvailable;
	}

	public Boolean getHasOptionalFeatures() {
		Boolean hasOptional = false;
		List<MarketplaceNodeInstallableUnitItem> installableUnitItems = getInstallableUnitItems();
		for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
			Boolean iuOptional = iuItem.getOptional();
			if (iuOptional == null) {
				hasOptional = null;
			} else if (Boolean.TRUE.equals(iuOptional)) {
				return true;
			}
		}
		return hasOptional;
	}

	@Override
	public Boolean getAvailable() {
		Boolean available = super.getAvailable();
		if (available == null) {
			List<MarketplaceNodeInstallableUnitItem> installableUnitItems = getInstallableUnitItems();
			if (installableUnitItems == null || installableUnitItems.isEmpty()) {
				return false;
			}
			available = true;
			boolean hasRequired = false;
			for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
				if (!iuItem.isOptional()) {
					hasRequired = true;
					Boolean iuAvailable = iuItem.getAvailable();
					if (Boolean.FALSE.equals(iuAvailable)) {
						return false;
					} else if (iuAvailable == null) {
						available = null;
					}
				}
			}
			if (!hasRequired) {
				for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
					Boolean iuAvailable = iuItem.getAvailable();
					if (Boolean.FALSE.equals(iuAvailable)) {
						return false;
					} else if (iuAvailable == null) {
						available = null;
					}
				}
			}
		}
		return available;
	}

	public List<Operation> getAvailableOperations() {
		List<Operation> available = new ArrayList<Operation>();
		if (!getInstallableUnits().isEmpty()) {
			if (isInstalled()) {
				if (maybeUpdateAvailable()) {
					available.add(Operation.UPDATE);
				}
				if (maybeHasOptionalFeatures()) {
					available.add(Operation.CHANGE);
				}
				available.add(Operation.UNINSTALL);
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

	private boolean maybeHasOptionalFeatures() {
		Boolean hasOptional = getHasOptionalFeatures();
		return !Boolean.FALSE.equals(hasOptional) && getInstallableUnitItems().size() > 1;
	}

	public void setUserFavorite(Boolean favorited) {
		if ((favorited == null && this.userFavorite != null)
				|| (favorited != null && !favorited.equals(this.userFavorite))) {
			Boolean oldValue = this.userFavorite;
			this.userFavorite = favorited;
			firePropertyChange("userFavorite", oldValue, favorited); //$NON-NLS-1$
		}
	}

	public Boolean getUserFavorite() {
		return userFavorite;
	}

	@Override
	public void setSelected(boolean selected) {
		boolean wasSelected = isSelected();
		if (wasSelected != selected) {
			super.setSelected(selected);
			firePropertyChange("selected", wasSelected, selected); //$NON-NLS-1$
		}
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
