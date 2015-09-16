/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * A controller that controls the multi-state install/uninstall button
 *
 * @author David Green
 */
@SuppressWarnings("rawtypes")
class ItemButtonController {
	private enum ButtonState {
		INSTALL(Messages.ItemButtonController_install, Operation.NONE, false), //
		UNINSTALL(Messages.ItemButtonController_uninstall, Operation.NONE, false), //
		INSTALL_PENDING(Messages.ItemButtonController_installPending, Operation.INSTALL, false), //
		UNINSTALL_PENDING(Messages.ItemButtonController_uninstallPending, Operation.UNINSTALL, false), //
		DISABLED(Messages.ItemButtonController_install, Operation.NONE, true), //
		UPDATE_DISABLED(Messages.ItemButtonController_update, Operation.NONE, true), //
		UPDATE(Messages.ItemButtonController_update, Operation.NONE, false), //
		UPDATE_PENDING(Messages.ItemButtonController_updatePending, Operation.UPDATE, false), //
		CHANGE(Messages.ItemButtonController_change, Operation.NONE, false), //
		CHANGE_PENDING(Messages.ItemButtonController_changePending, Operation.CHANGE, false);

		final String label;

		private final Operation operation;

		private final boolean disabled;

		private ButtonState(String label, Operation operation, boolean disabled) {
			this.label = label;
			this.operation = operation;
			this.disabled = disabled;
		}

		public ButtonState nextState() {
			switch (this) {
			case INSTALL:
				return INSTALL_PENDING;
			case INSTALL_PENDING:
				return INSTALL;
			case UNINSTALL:
				return UNINSTALL_PENDING;
			case UNINSTALL_PENDING:
				return UNINSTALL;
			case UPDATE:
				return UPDATE_PENDING;
			case UPDATE_PENDING:
				return UPDATE;
			case CHANGE:
				return CHANGE_PENDING;
			case CHANGE_PENDING:
				return CHANGE;
			}
			return this;
		}

		public ButtonState noActionState() {
			switch (this) {
			case INSTALL_PENDING:
				return INSTALL;
			case UNINSTALL_PENDING:
				return UNINSTALL;
			case UPDATE_PENDING:
				return UPDATE;
			case CHANGE_PENDING:
				return CHANGE;
			}
			return this;
		}

	}

	private final DiscoveryItem item;

	private final DropDownButton button;

	private ButtonState primaryState;

	private List<ButtonState> selectableStates;

	private final Menu menu;

	@SuppressWarnings("unused")
	private final MarketplaceViewer viewer;

	public ItemButtonController(MarketplaceViewer marketplaceViewer, DiscoveryItem discoveryItem, DropDownButton button) {
		this.item = discoveryItem;
		this.button = button;
		this.viewer = marketplaceViewer;

		updateButtonState();
		menu = createMenu(button.getButton());
		updateAppearance();
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.ARROW) {
					menuClicked();
				} else {
					buttonClicked(primaryState);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private Menu createMenu(Button button) {
		final Menu menu = new Menu(button);
		createMenuItems(menu);
		button.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				menu.dispose();
			}
		});
		return menu;
	}

	private void createMenuItems(Menu menu) {
		if (selectableStates != null) {
			if (primaryState != null) {
				createMenuItem(menu, primaryState);
			}
			for (ButtonState state : selectableStates) {
				createMenuItem(menu, state);
			}
		}
	}

	private void createMenuItem(Menu menu, ButtonState state) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setData(state);
		menuItem.setText(state.label);
		menuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonClicked((ButtonState) e.widget.getData());
			}
		});
	}

	private void updateMenuItems() {
		MenuItem[] items = menu.getItems();
		for (MenuItem menuItem : items) {
			menuItem.dispose();
		}
		createMenuItems(menu);
	}

	private void menuClicked() {
		List<ButtonState> selectableStates = getSelectableStates();
		if (selectableStates.isEmpty()) {
			buttonClicked(primaryState);
			return;
		}
		Rectangle rect = button.getButton().getBounds();
		Point p = button.getButton().toDisplay(rect.x, rect.y + rect.height);
		menu.setLocation(p.x - rect.x, p.y - rect.y);
		menu.setVisible(true);
	}

	private void buttonClicked(ButtonState primary) {
		if (primary != ButtonState.DISABLED) {
			primary = primary.nextState();
			item.maybeModifySelection(primary.operation);
		}
		refresh();
	}

	private void updateButtonState() {
		primaryState = ButtonState.DISABLED;
		MarketplaceNodeCatalogItem catalogItem = (MarketplaceNodeCatalogItem) item.getData();
		if (catalogItem.getInstallableUnits().isEmpty()) {
			primaryState = ButtonState.DISABLED;
		} else {
			Operation operation = item.getSelectedOperation();
			boolean installed = isItemInstalled();
			selectableStates = Collections.emptyList();
			if (installed) {
				switch (operation) {
				case UPDATE:
					primaryState = ButtonState.UPDATE_PENDING;
					break;
				case UNINSTALL:
					primaryState = ButtonState.UNINSTALL_PENDING;
					break;
				case CHANGE:
					primaryState = ButtonState.CHANGE_PENDING;
					break;
				case NONE:
					primaryState = ButtonState.UPDATE;
					if (hasUpdateAvailable()) {
						selectableStates = hasOptionalFeatures() ? Arrays.asList(ButtonState.CHANGE,
								ButtonState.UNINSTALL) : Collections.singletonList(ButtonState.UNINSTALL);
					} else {
						primaryState = hasOptionalFeatures() ? ButtonState.CHANGE : ButtonState.UNINSTALL;
						selectableStates = hasOptionalFeatures() ? Collections.singletonList(ButtonState.UNINSTALL)
								: Collections.<ButtonState> emptyList();
					}
					break;
				}
			} else {
				switch (operation) {
				case INSTALL:
					primaryState = ButtonState.INSTALL_PENDING;
					break;
				case NONE:
					primaryState = ButtonState.INSTALL;
					break;
				}
				if (!isItemAvailable()) {
					primaryState = ButtonState.DISABLED;
				}
			}
		}
	}

	private List<ButtonState> getSelectableStates() {
		if (selectableStates == null) {
			updateButtonState();
		}
		return selectableStates;
	}

	private boolean hasUpdateAvailable() {
		Boolean available = ((MarketplaceNodeCatalogItem) item.getData()).getUpdateAvailable();
		return available == null || available.booleanValue();
	}

	private boolean hasOptionalFeatures() {
		Boolean optional = ((MarketplaceNodeCatalogItem) item.getData()).getHasOptionalFeatures();
		return isItemInstalled() && (optional == null || optional.booleanValue());
	}

	private boolean isItemInstalled() {
		return ((CatalogItem) item.getData()).isInstalled();
	}

	private boolean isItemAvailable() {
		Boolean available = ((CatalogItem) item.getData()).getAvailable();
		return available == null || available;
	}

	private void updateAppearance() {
		boolean relayout = false;
		Button control = button.getButton();
		if (!primaryState.label.equals(button.getText())) {
			if (primaryState == ButtonState.INSTALL) { //bold
				button.setFont(JFaceResources.getFontRegistry().getBold("")); //$NON-NLS-1$
			} else if (primaryState.noActionState() == primaryState) { // no "pending" state
				button.setFont(JFaceResources.getFontRegistry().defaultFont());
			} else { //"pending" state - italic
				button.setFont(JFaceResources.getFontRegistry().getItalic("")); //$NON-NLS-1$
			}
			button.setText(primaryState.label);

			Point preferredSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			int preferredWidth = preferredSize.x + 10;//Give a bit of extra padding for bold or italic font
			((GridData) control.getLayoutData()).minimumWidth = preferredWidth;

			relayout = true;
		}
		control.setEnabled(!primaryState.disabled);
		// button image? Due to platform limitations we can't set the button color

		boolean menu = !getSelectableStates().isEmpty();
		if (menu != button.isShowArrow()) {
			relayout = true;
			button.setShowArrow(menu);
		}
		if (relayout) {
			item.layout(true, false);
		}
	}

	public void refresh() {
		updateButtonState();
		updateMenuItems();
		updateAppearance();
	}

}
