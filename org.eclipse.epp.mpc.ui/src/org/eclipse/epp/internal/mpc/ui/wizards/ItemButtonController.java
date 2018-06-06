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
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
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

		public static ButtonState forOperation(Operation operation) {
			if (operation == Operation.NONE) {
				return DISABLED;
			}
			for (ButtonState buttonState : ButtonState.values()) {
				if (buttonState.operation == operation) {
					return buttonState.noActionState();
				}
			}
			return DISABLED;
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
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.widget.isDisposed()) {
					return;
				}
				if (e.detail == SWT.ARROW) {
					menuClicked();
				} else {
					buttonClicked(primaryState);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private Menu createMenu(Button button) {
		final Menu menu = new Menu(button);
		createMenuItems(menu);
		button.addDisposeListener(e -> menu.dispose());
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
				if (e.widget.isDisposed()) {
					return;
				}
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
		selectableStates = Collections.emptyList();
		MarketplaceNodeCatalogItem catalogItem = (MarketplaceNodeCatalogItem) item.getData();
		if (catalogItem.getInstallableUnits().isEmpty()) {
			return;
		}
		List<Operation> availableOperations = catalogItem.getAvailableOperations();
		if (availableOperations.isEmpty()) {
			return;
		}
		Operation selectedOperation = item.getSelectedOperation();
		Operation primaryOperation = selectedOperation;
		switch (selectedOperation) {
		case UPDATE:
			primaryState = ButtonState.UPDATE_PENDING;
			break;
		case UNINSTALL:
			primaryState = ButtonState.UNINSTALL_PENDING;
			break;
		case CHANGE:
			primaryState = ButtonState.CHANGE_PENDING;
			break;
		case INSTALL:
			primaryState = ButtonState.INSTALL_PENDING;
			break;
		case NONE:
			primaryOperation = availableOperations.get(0);
			primaryState = ButtonState.forOperation(primaryOperation);
			break;
		}
		if (availableOperations.size() > 1) {
			selectableStates = new ArrayList<>(availableOperations.size() - 1);
			for (Operation operation : availableOperations) {
				if (operation != primaryOperation) {
					ButtonState selectableState = ButtonState.forOperation(operation);
					if (selectableState != ButtonState.DISABLED) {
						selectableStates.add(selectableState);
					}
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
		if (item.isDisposed()) {
			return;
		}
		updateButtonState();
		updateMenuItems();
		updateAppearance();
	}

}
