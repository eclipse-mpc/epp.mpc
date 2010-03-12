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

import java.net.URISyntaxException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUI;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.AbstractDiscoveryItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.statushandlers.StatusManager;

@SuppressWarnings("restriction")
public class BrowseCatalogItem extends AbstractDiscoveryItem<CatalogDescriptor> {

	private final MarketplaceViewer viewer;

	private final DiscoveryResources resources;

	private final IShellProvider shellProvider;

	public BrowseCatalogItem(Composite parent, DiscoveryResources resources, IShellProvider shellProvider,
			CatalogDescriptor element, MarketplaceViewer viewer) {
		super(parent, SWT.NULL, resources, element);
		this.resources = resources;
		this.shellProvider = shellProvider;
		this.viewer = viewer;
		createContent();
	}

	private void createContent() {
		Composite parent = this;

		GridLayoutFactory.swtDefaults().applyTo(parent);

		Link link = new Link(parent, SWT.NULL);
		link.setText("<a>Browse for more solutions</a>");
		link.setToolTipText(NLS.bind("Open {0} in a browser", getData().getUrl()));
		link.setBackground(null);
		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				openMarketplace();
			}
		});

		GridDataFactory.swtDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(link);
	}

	protected void openMarketplace() {
		CatalogDescriptor catalogDescriptor = getData();

		try {
			WorkbenchUtil.openUrl(catalogDescriptor.getUrl().toURI().toString(), IWorkbenchBrowserSupport.AS_EXTERNAL);
		} catch (URISyntaxException e) {
			String message = String.format("Cannot open browser");
			IStatus status = new Status(IStatus.ERROR, MarketplaceClientUI.BUNDLE_ID, IStatus.ERROR, message, e);
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		}
	}

	@Override
	protected void refresh() {
		// nothing to do
	}

}
