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

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class FavoritesDiscoveryItem extends AbstractMarketplaceDiscoveryItem<MarketplaceNodeCatalogItem> {

	private Button checkbox;

	public FavoritesDiscoveryItem(Composite parent, int style, MarketplaceDiscoveryResources resources,
			MarketplaceNodeCatalogItem connector, FavoritesViewer viewer) {
		super(parent, style, resources, null, connector, viewer);
	}

	@Override
	protected FavoritesViewer getViewer() {
		return (FavoritesViewer) super.getViewer();
	}

	@Override
	protected void createContent() {
		// ignore
		super.createContent();
	}

	@Override
	protected boolean alignIconWithName() {
		return true;
	}

	@Override
	protected void createIconControl(Composite checkboxContainer) {
		((GridLayout) checkboxContainer.getLayout()).numColumns++;
		checkbox = new Button(checkboxContainer, SWT.CHECK | SWT.INHERIT_FORCE);
		checkbox.setSelection(connector.isSelected());
		checkbox.setText(""); //$NON-NLS-1$
		checkbox.setData("connectorId", connector.getId()); //$NON-NLS-1$
		checkbox.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selected = checkbox.getSelection();
				getViewer().modifySelection(connector, selected);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		connector.addPropertyChangeListener(evt -> {
			if ("selected".equals(evt.getPropertyName())) { //$NON-NLS-1$
				final Button checkbox = FavoritesDiscoveryItem.this.checkbox;

				if (checkbox == null || checkbox.isDisposed()) {
					return;
				}
				final boolean selected = Boolean.TRUE.equals(evt.getNewValue());
				try {
					checkbox.getDisplay().syncExec(() -> {
						if (checkbox == null || checkbox.isDisposed()) {
							return;
						}
						checkbox.setSelection(selected);
					});
				} catch (SWTException ex) {
					//disposed - ignore
				}
			}
		});
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(checkbox);
		super.createIconControl(checkboxContainer);

		GridLayout containerLayout = ((GridLayout) checkboxContainer.getLayout());
		GridData containerLayoutData = (GridData) checkboxContainer.getLayoutData();
		int xHint = containerLayoutData.widthHint + checkbox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
				+ containerLayout.horizontalSpacing;
		GridDataFactory.createFrom(containerLayoutData)
		.hint(xHint, containerLayoutData.heightHint)
		.minSize(xHint, containerLayoutData.minimumHeight)
		.applyTo(checkboxContainer);
	}

	@Override
	protected void createInstallButtons(Composite parent) {
		//no install buttons
	}

	@Override
	protected void createSocialButtons(Composite parent) {
		//no social stuff
	}

	@Override
	protected void createInstallInfo(Composite parent) {
		//no install info
	}

	@Override
	protected void searchForProvider(String searchTerm) {
		// ignore

	}

	@Override
	protected void searchForTag(String tag) {
		// ignore

	}
}
