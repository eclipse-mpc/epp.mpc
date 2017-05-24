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
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;

public class RetryErrorActionItem extends AbstractUserActionItem {

	private static final int RETRY_ACTION_ID = 1;

	private static final int DETAILS_ACTION_ID = 0;

	public RetryErrorActionItem(Composite parent, MarketplaceDiscoveryResources resources,
			UserActionCatalogItem connector, MarketplaceViewer viewer) {
		super(parent, resources, connector, viewer);
	}

	private IStatus getError() {
		return MarketplaceClientCore.computeStatus((Throwable) connector.getData(), null);
	}

	@Override
	protected String getNameLabelText() {
		return Messages.UserFavoritesUnsupportedActionItem_unsupportedFavoritesLabel;
	}

	@Override
	protected String getDescriptionText() {
		IStatus error = getError();
		return NLS.bind(Messages.RetryErrorActionItem_failedToLoadMessage,
				error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
	}

	@Override
	protected Icon getIcon() {
		String path = REGISTRY_SCHEME + MarketplaceClientUiPlugin.ACTION_ICON_WARNING;
		return createIcon(path);
	}

	@Override
	protected String getSublineText() {
		return Messages.RetryErrorActionItem_subline;
	}

	@Override
	protected void createButtons(Composite parent) {
		createButton(parent, Messages.RetryErrorActionItem_showDetailsActionLabel,
				Messages.RetryErrorActionItem_showDetailsTooltip, DETAILS_ACTION_ID);
		createButton(parent, Messages.RetryErrorActionItem_retryActionLabel,
				Messages.RetryErrorActionItem_retryTooltip, RETRY_ACTION_ID);
	}

	@Override
	protected void buttonPressed(int id) {
		if (id == RETRY_ACTION_ID) {
			retry();
		} else {
			showDetails();
		}
	}

	protected void showDetails() {
		IStatus error = getError();
		ErrorDialog.openError(getShell(), Messages.RetryErrorActionItem_errorDetailsDialogTitle, getDescriptionText(),
				error);
	}

	protected void retry() {
		this.getViewer().reload();
	}

	@Override
	protected MarketplaceViewer getViewer() {
		return (MarketplaceViewer) super.getViewer();
	}
}
