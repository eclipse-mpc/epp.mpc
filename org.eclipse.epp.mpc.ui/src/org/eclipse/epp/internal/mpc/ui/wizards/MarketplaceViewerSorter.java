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
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.util.CatalogCategoryComparator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

final class MarketplaceViewerSorter extends ViewerComparator {
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
				String n1 = i1.getName();
				String n2 = i2.getName();
				if (n1 == null) {
					n1 = ""; //$NON-NLS-1$
				}
				if (n2 == null) {
					n2 = ""; //$NON-NLS-1$
				}
				i = n1.compareToIgnoreCase(n2);
				if (i == 0) {
					i = n1.compareTo(n2);
					if (i == 0) {
						// same name, so we sort by id.
						String id1 = i1.getId();
						String id2 = i2.getId();
						if (id1 == null) {
							id1 = ""; //$NON-NLS-1$
						}
						if (id2 == null) {
							id2 = ""; //$NON-NLS-1$
						}
						i = id1.compareTo(id2);
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