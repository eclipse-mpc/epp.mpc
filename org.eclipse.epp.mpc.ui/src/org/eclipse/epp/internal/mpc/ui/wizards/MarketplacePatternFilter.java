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

import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.ui.discovery.util.PatternFilter;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author David Green
 */
class MarketplacePatternFilter extends PatternFilter {

	public MarketplacePatternFilter() {
		setIncludeLeadingWildcard(true);
	}

	@Override
	protected Object[] getChildren(Object element) {
		if (element instanceof CatalogCategory) {
			return ((CatalogCategory) element).getItems().toArray();
		}
		return super.getChildren(element);
	}

	@Override
	protected boolean isLeafMatch(Viewer filteredViewer, Object element) {
		// always match, since filtering is performed server-side
		return true;
	}

}
