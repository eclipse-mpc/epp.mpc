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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public abstract class UserFavoritesAbstractImportActionItem extends UserActionViewerItem<UserActionCatalogItem> {

	public UserFavoritesAbstractImportActionItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider,
			UserActionCatalogItem element, MarketplaceViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
		createContent();
	}

	@Override
	protected void createContent() {
		Composite parent = this;
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(parent);

		int vAlignLinks = SWT.CENTER;
		String descriptionText = getDescriptionText();
		if (descriptionText != null) {
			Label descriptionLabel = new Label(parent, SWT.CENTER);
			descriptionLabel.setText(descriptionText);
			GridDataFactory.swtDefaults()
			.grab(true, false)
			.align(SWT.CENTER, SWT.END)
			.applyTo(descriptionLabel);
			vAlignLinks = SWT.BEGINNING;
		}
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				actionPerformed(event.data);
			}
		};
		Composite linkParent = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.CENTER, vAlignLinks).applyTo(linkParent);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(linkParent);

		Control link = createImportActionLink(linkParent);
		link.addListener(SWT.Selection, listener);
		GridDataFactory.swtDefaults().align(SWT.END, vAlignLinks).applyTo(link);

		Label separator = new Label(linkParent, SWT.CENTER);
		separator.setText(" | "); //$NON-NLS-1$
		GridDataFactory.swtDefaults().align(SWT.CENTER, vAlignLinks).applyTo(link);

		link = createSecondaryActionLink(linkParent);
		link.addListener(SWT.Selection, listener);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.BEGINNING, vAlignLinks).applyTo(link);
	}

	protected abstract Control createSecondaryActionLink(Composite parent);

	protected Control createImportActionLink(Composite parent) {
		return createActionLink(parent);
	}

	protected String getDescriptionText() {
		return null;
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

	@Override
	protected final String getLinkText() {
		return "<a href=\"import\">Import Favorites...</a>"; //$NON-NLS-1$
	}

	@Override
	protected String getLinkToolTipText() {
		return "Import another user's favorites into yours."; //$NON-NLS-1$
	}
}
