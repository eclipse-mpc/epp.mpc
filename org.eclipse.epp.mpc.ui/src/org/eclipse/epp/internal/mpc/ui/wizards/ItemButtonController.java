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

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;

/**
 * A controller that controls the multi-state install/uninstall button
 * 
 * @author David Green
 */
class ItemButtonController {
	private enum ButtonState {
		INSTALL("Install"), UNINSTALL("Uninstall"), INSTALL_PENDING("Pending"), UNINSTALL_PENDING("Pending"), DISABLED(
				"Install");

		final String label;

		private ButtonState(String label) {
			this.label = label;
		}
	}

	private final DiscoveryItem item;

	private final Button button;

	private ButtonState buttonState;

	private Color originalBackground;

	private MarketplaceViewer viewer;

	public ItemButtonController(MarketplaceViewer marketplaceViewer, DiscoveryItem discoveryItem, Button button) {
		this.item = discoveryItem;
		this.button = button;
		this.viewer = marketplaceViewer;
		originalBackground = button.getBackground();
		updateButtonState();
		updateAppearance();
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (buttonState != ButtonState.DISABLED) {
					item.maybeModifySelection(!item.isSelected());
				}
				refresh();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private void updateButtonState() {
		CatalogItem catalogItem = (CatalogItem) item.getData();
		if (catalogItem.getInstallableUnits().isEmpty()) {
			buttonState = ButtonState.DISABLED;
		} else {
			boolean installed = isItemInstalled();
			if (catalogItem.isSelected()) {
				if (installed) {
					buttonState = ButtonState.UNINSTALL_PENDING;
				} else {
					buttonState = ButtonState.INSTALL_PENDING;
				}
			} else {
				if (installed) {
					buttonState = ButtonState.UNINSTALL;
				} else {
					buttonState = ButtonState.INSTALL;
				}
			}
		}
	}

	private boolean isItemInstalled() {
		return ((CatalogItem) item.getData()).isInstalled();
	}

	private void updateAppearance() {
		button.setText(buttonState.label);
		if (buttonState == ButtonState.DISABLED) {
			button.setEnabled(false);
		}
		// FIXME button image? Due to platform limitations we can't set the button color

		item.layout(true, true);
	}

	public void refresh() {
		updateButtonState();
		updateAppearance();
	}

}
