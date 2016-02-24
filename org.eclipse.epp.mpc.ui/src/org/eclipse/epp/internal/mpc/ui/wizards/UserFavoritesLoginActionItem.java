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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;

public class UserFavoritesLoginActionItem extends UserActionViewerItem {
	public UserFavoritesLoginActionItem(Composite parent, DiscoveryResources resources, IShellProvider shellProvider,
			Object element, MarketplaceViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
		createContent();
	}

	@Override
	protected String getLinkText() {
		return "<a>Log in to view your favorites</a>";
	}

	@Override
	protected void actionPerformed() {
		final MarketplaceCatalog catalog = viewer.getCatalog();
//		new Job("Logging in") {
//			{
//				setUser(false);
//				setSystem(true);
//				setPriority(Job.INTERACTIVE);
//			}
//
//			@Override
//			protected IStatus run(IProgressMonitor monitor) {
		catalog.userFavorites(true, new NullProgressMonitor());
//				getDisplay().asyncExec(new Runnable() {
//					public void run() {
		viewer.setContentType(ContentType.FAVORITES);
//					}
//				});
//				return Status.OK_STATUS;
//			}
//		}.schedule();
	}

}
