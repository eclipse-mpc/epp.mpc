/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListItem;
import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListViewer;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

public class CatalogListViewer extends ControlListViewer {

	private DiscoveryResources resources;

	private ImageRegistry imageRegistry;

	public CatalogListViewer(Composite parent, int style) {
		super(parent, style);
		resources = new DiscoveryResources(parent.getDisplay());
		imageRegistry = new ImageRegistry();

		getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				resources.dispose();
				imageRegistry.dispose();
			}
		});
	}

	@Override
	protected ControlListItem doCreateItem(Composite parent, Object element) {
		MarketplaceCatalogConfiguration configuration = (MarketplaceCatalogConfiguration) getInput();
		final CatalogListItem item = new CatalogListItem(parent, SWT.NULL, resources, imageRegistry,
				(CatalogDescriptor) element);
		item.setSelected(element == configuration.getCatalogDescriptor());
		return item;
	}

}
