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

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
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
		checkbox = new Button(checkboxContainer, SWT.CHECK | SWT.INHERIT_FORCE);
		checkbox.setSelection(connector.isSelected());
		checkbox.setText(""); //$NON-NLS-1$
		checkbox.setData("connectorId", connector.getId()); //$NON-NLS-1$
		checkbox.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				boolean selected = checkbox.getSelection();
				getViewer().modifySelection(connector, selected);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(checkbox);
		super.createIconControl(checkboxContainer);

		GridLayoutFactory.fillDefaults().spacing(1, 1).margins(0, 0).numColumns(2).applyTo(checkboxContainer);
		GridData containerLayoutData = (GridData) checkboxContainer.getLayoutData();
		int xHint = containerLayoutData.widthHint + checkbox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
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
