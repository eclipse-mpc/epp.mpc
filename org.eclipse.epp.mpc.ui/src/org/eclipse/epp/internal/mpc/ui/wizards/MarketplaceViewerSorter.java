/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.util.CatalogCategoryComparator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

final class MarketplaceViewerSorter extends ViewerSorter {
	CatalogCategoryComparator categoryComparator = new CatalogCategoryComparator();

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		if (o1 == o2) {
			return 0;
		}
		CatalogCategory cat1 = getCategory(o1);
		CatalogCategory cat2 = getCategory(o2);

		// FIXME filter uncategorized items?
		if (cat1 == null) {
			return (cat2 != null) ? 1 : 0;
		} else if (cat2 == null) {
			return 1;
		}

		int i = categoryComparator.compare(cat1, cat2);
		if (i == 0) {
			if (o1 instanceof CatalogCategory) {
				return -1;
			}
			if (o2 instanceof CatalogCategory) {
				return 1;
			}

			CatalogItem i1 = (CatalogItem) o1;
			CatalogItem i2 = (CatalogItem) o2;

			// catalog descriptor comes last
			if (i1.getData() instanceof CatalogDescriptor) {
				i = 1;
			} else if (i2.getData() instanceof CatalogDescriptor) {
				i = -1;
			} else {
				// otherwise we sort by name
				i = i1.getName().compareToIgnoreCase(i2.getName());
				if (i == 0) {
					i = i1.getName().compareTo(i2.getName());
					if (i == 0) {
						// same name, so we sort by id.
						i = i1.getId().compareTo(i2.getId());
					}
				}
			}
		}
		return i;
	}

	private CatalogCategory getCategory(Object o) {
		if (o instanceof CatalogCategory) {
			return (CatalogCategory) o;
		}
		if (o instanceof CatalogItem) {
			return ((CatalogItem) o).getCategory();
		}
		return null;
	}
}