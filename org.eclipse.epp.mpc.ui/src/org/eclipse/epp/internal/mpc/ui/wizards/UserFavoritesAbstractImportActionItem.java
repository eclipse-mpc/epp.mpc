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

import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;

public abstract class UserFavoritesAbstractImportActionItem extends AbstractUserActionLinksItem {

	public UserFavoritesAbstractImportActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider,
			UserActionCatalogItem element, MarketplaceViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
		createContent(new ActionLink("import", "Import Favorites...", "Import another user's favorites into yours."),
				createSecondaryActionLink());
	}

	protected void importFavorites() {
		//TODO
	}

	@Override
	protected final void actionPerformed(Object data) {
		if ("import".equals(data)) { //$NON-NLS-1$
			importFavorites();
		} else {
			secondaryActionPerformed();
		}
	}

	protected abstract void secondaryActionPerformed();

	protected abstract ActionLink createSecondaryActionLink();
}
