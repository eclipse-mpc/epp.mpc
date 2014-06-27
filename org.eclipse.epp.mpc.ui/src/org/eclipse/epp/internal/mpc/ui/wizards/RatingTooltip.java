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

import org.eclipse.core.runtime.Assert;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

class RatingTooltip extends ToolTip {

	private static final String RATING_TIP_SHOWN_PREFERENCE = "ratingTipShown"; //$NON-NLS-1$

	private final Control parent;

	private final Runnable continueRunnable;

	public RatingTooltip(Control control, Runnable continueRunnable) {
		super(control, ToolTip.RECREATE, true);
		Assert.isNotNull(continueRunnable);
		this.parent = control;
		this.continueRunnable = continueRunnable;
		setHideOnMouseDown(false); // required for buttons to work
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		Shell shell = parent.getShell();
		setData(Shell.class.getName(), shell);
		DiscoveryItem.setWidgetId(shell, DiscoveryItem.WIDGET_ID_RATING);
		Color backgroundColor = parent.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		final Composite container = new Composite(parent, SWT.NULL);
		container.setBackground(backgroundColor);
		GridLayoutFactory.swtDefaults().numColumns(2).spacing(8, 5).applyTo(container);

		final Button continueButton = new Button(container, SWT.NONE);
		continueButton.setBackground(backgroundColor);
		continueButton.setText(Messages.RatingTooltip_Continue);
		continueButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				final Button button = (Button) ((Accessible) e.getSource()).getControl();
				final String text = button.getText();
				e.result = text.replace('>', ' ');
			}
		});

		Point prefSize = continueButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		GridDataFactory.fillDefaults()
		.align(SWT.FILL, SWT.TOP)
		//make it look a bit larger
		.hint(SWT.DEFAULT, (int) (prefSize.y * 1.2))
		.applyTo(continueButton);

		Label label = new Label(container, SWT.WRAP);
		label.setBackground(backgroundColor);
		label.setText(Messages.RatingTooltip_Note);

		//make label go roughly over 3 lines if it's not incredibly short
		prefSize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		int prefWidth = Math.max(180, (int) (prefSize.x * 0.4));
		GridDataFactory.fillDefaults()
		.span(1, 2)
		.align(SWT.BEGINNING, SWT.TOP)
		.grab(false, true)
		.hint(prefWidth, SWT.DEFAULT)
		.applyTo(label);

		final Button dontShowCheckBox = new Button(container, SWT.CHECK);
		dontShowCheckBox.setSelection(true);
		dontShowCheckBox.setBackground(backgroundColor);
		dontShowCheckBox.setText(Messages.RatingTooltip_Dont_show_again);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(dontShowCheckBox);

		continueButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dontShowCheckBox.getSelection()) {
					MarketplaceClientUiPlugin.getInstance()
					.getPreferenceStore()
					.setValue(RATING_TIP_SHOWN_PREFERENCE, true);
				}
				continueRunnable.run();
				RatingTooltip.this.hide();
			}
		});

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				if (!continueButton.isDisposed()) {
					continueButton.setFocus();
				}
			}
		});

		return container;
	}

	@Override
	protected void afterHideToolTip(Event event) {
		setData(Shell.class.getName(), null);
	}

	public void show() {
		Rectangle bounds = parent.getBounds();
		show(new Point(0, bounds.height));
	}

	public static boolean shouldShowRatingTooltip() {
		return !MarketplaceClientUiPlugin.getInstance().getPreferenceStore().getBoolean(RATING_TIP_SHOWN_PREFERENCE);
	}
}
