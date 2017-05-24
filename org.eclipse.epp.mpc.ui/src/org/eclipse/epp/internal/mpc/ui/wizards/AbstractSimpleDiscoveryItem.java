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

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public abstract class AbstractSimpleDiscoveryItem<T extends CatalogItem> extends AbstractMarketplaceDiscoveryItem<T> {

	public AbstractSimpleDiscoveryItem(Composite parent, int style, MarketplaceDiscoveryResources resources,
			IMarketplaceWebBrowser browser, final T connector, CatalogViewer viewer) {
		super(parent, style, resources, browser, connector, viewer);
	}

	@Override
	protected boolean alignIconWithName() {
		return true;
	}

	protected void createButtons(Composite parent) {
		// ignore
	}

	@Override
	protected abstract Icon getIcon();

	@Override
	protected abstract String getDescriptionText();

	@Override
	protected abstract String getNameLabelText();

	protected String getSublineText() {
		return null;
	}

	@Override
	protected void createDescription(Composite parent) {
		String descriptionText = getDescriptionText();
		if (descriptionText != null && !"".equals(descriptionText)) { //$NON-NLS-1$
			super.createDescription(parent);
		}
	}

	@Override
	protected void createTagsLabel(Composite parent) {
		// we'll abuse this to create a 2-row container for action buttons that shows up
		// below the description and to the right of the icon, instead of creating an extra
		// row below
		createButtonBar(parent);
	}

	protected void createButtonBar(Composite parent) {
		String sublineText = getSublineText();
		String descriptionText = getDescriptionText();
		int usedRows = 0;//name and button bar
		if (descriptionText != null && !"".equals(descriptionText)) { //$NON-NLS-1$
			usedRows++;
		}
		if (sublineText != null && !"".equals(sublineText)) { //$NON-NLS-1$
			usedRows++;
		}
		int availableRows = 3;
		createButtonBar(parent, availableRows - usedRows);
	}

	@Override
	protected void createProviderLabel(Composite parent) {
		createSublineLabel(parent);
	}

	protected StyledText createSublineLabel(Composite parent) {
		String sublineText = getSublineText();
		if (sublineText == null) {
			return null;
		}

		StyledText subline = StyledTextHelper.createStyledTextLabel(parent);

		subline.setEditable(false);
		GridDataFactory.fillDefaults()
		.indent(DESCRIPTION_MARGIN_LEFT, DESCRIPTION_MARGIN_TOP)
		.span(3, 1)
		.align(SWT.BEGINNING, SWT.CENTER)
		.grab(true, false)
		.applyTo(subline);
		// always disabled color to make it less prominent
		subline.setForeground(resources.getColorDisabled());
		subline.setText(sublineText);
		StyleRange range = new StyleRange(0, subline.getText().length(), subline.getForeground(), null, SWT.ITALIC);
		subline.setStyleRange(range);
		return subline;
	}

	protected void createButtonBar(Composite parent, int vSpan) {
		Label spacer = createButtonBarSpacer(parent);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).span(1, vSpan).applyTo(spacer);

		Composite buttonContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults()
		.indent(0, BUTTONBAR_MARGIN_TOP)
		.align(SWT.END, SWT.END)
		.grab(false, false)
		.span(2, vSpan)
		.applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().numColumns(0).equalWidth(true).applyTo(buttonContainer);

		createButtons(buttonContainer);
	}

	protected Button createButton(Composite parent, String text, String tooltipText, int id) {
		((GridLayout) parent.getLayout()).numColumns++;
		Button button = new Button(parent, SWT.PUSH | SWT.BOLD);
		button.setText(text);
		if (tooltipText != null) {
			button.setToolTipText(tooltipText);
		}
		button.setFont(JFaceResources.getFontRegistry().getBold("")); //$NON-NLS-1$
		button.setData(Integer.valueOf(id));
		createButtonLayoutData(button, getPixelConverter()).align(SWT.END, SWT.END)
		.grab(false, false)
		.applyTo(button);

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonPressed(((Integer) e.widget.getData()).intValue());
			}
		});
		return button;
	}

	protected void buttonPressed(int id) {

	}

	@Override
	protected void createInstallButtons(Composite parent) {
		// ignore
	}

	@Override
	protected void createInstallInfo(Composite parent) {
		// ignore
	}

	@Override
	protected void createSocialButtons(Composite parent) {
		// ignore
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