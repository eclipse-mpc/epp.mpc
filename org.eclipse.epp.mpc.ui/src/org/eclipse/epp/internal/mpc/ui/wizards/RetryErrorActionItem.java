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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;

public class RetryErrorActionItem extends AbstractUserActionLinksItem {

	private static final String RETRY_ACTION_ID = "retry"; //$NON-NLS-1$

	private static final String DETAILS_ACTION_ID = "details"; //$NON-NLS-1$

	private final IStatus error;

	public RetryErrorActionItem(Composite parent, DiscoveryResources resources, IShellProvider shellProvider,
			UserActionCatalogItem element, MarketplaceViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
		this.error = MarketplaceClientCore.computeStatus((Throwable) element.getData(), null);
		createContent(new ActionLink(DETAILS_ACTION_ID, "Show details", "Show more details about the error"),
				new ActionLink(RETRY_ACTION_ID, "Retry", "Try loading this tab's contents again"));
	}

	@Override
	protected String getDescriptionText() {
		return NLS.bind("Failed to load contents: {0}",
				error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
	}

	@Override
	protected void actionPerformed(Object data) {
		if (RETRY_ACTION_ID.equals(data)) {
			retry();
		} else if (DETAILS_ACTION_ID.equals(data)) {
			showDetails();
		} else {
			throw new IllegalArgumentException(NLS.bind("Unsupported link: {0}", data));
		}
	}

	protected void showDetails() {
		ErrorDialog.openError(getShell(), "Error details", getDescriptionText(), error);
	}

	protected void retry() {
		this.viewer.reload();
	}
}
