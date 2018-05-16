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
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractUserActionItem extends AbstractSimpleDiscoveryItem<UserActionCatalogItem> {

	public AbstractUserActionItem(Composite parent, MarketplaceDiscoveryResources resources,
			UserActionCatalogItem connector, CatalogViewer viewer) {
		super(parent, SWT.NONE, resources, null, connector, viewer);
	}

	@Override
	protected void createContent() {
		setBackgroundMode(SWT.INHERIT_DEFAULT);
		setBackground(this.getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		super.createContent();
	}

	@Override
	protected void createSeparator(Composite parent) {
		// ignore
	}

	@Override
	protected String getItemClass() {
		return "NotificationItem";
	}
}