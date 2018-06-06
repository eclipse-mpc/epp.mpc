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