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
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/

package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogFilter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * A filter that can cause items to be hidden from the catalog view. Each filter may have an optional UI. Multiple
 * filters may be added to the {@link CatalogConfiguration#getFilters() configuration filters}.
 *
 * @author David Green
 * @see CatalogConfiguration
 */
public abstract class MarketplaceFilter extends CatalogFilter {
	private Catalog catalog;

	private final List<IPropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

	public Catalog getCatalog() {
		return catalog;
	}

	public void setCatalog(Catalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Add a change listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a change listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Fire a property change event.
	 */
	protected void firePropertyChange(String property, Object oldValue, Object newValue) {
		if (!listeners.isEmpty()) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
			for (IPropertyChangeListener listener : listeners) {
				listener.propertyChange(event);
			}
		}
	}

	/**
	 * Create the filter control, if any. The {@link #getCatalog() catalog} must be set prior to calling this method.
	 */
	public abstract void createControl(Composite parent);

	/**
	 * Returns whether the given element makes it through this filter.
	 *
	 * @return <code>true</code> if element is included in the filtered set, and <code>false</code> if excluded
	 */
	@Override
	public abstract boolean select(CatalogItem item);

	/**
	 * Called when the category was modified.
	 *
	 * @param wasCancelled
	 *            true if and only if the update was canceled before the update was completed
	 */
	public abstract void catalogUpdated(boolean wasCancelled);

}
