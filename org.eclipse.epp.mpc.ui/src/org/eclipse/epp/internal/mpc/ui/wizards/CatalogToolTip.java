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

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.util.GradientToolTip;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

/**
 * @author Benjamin Muskalla
 */
public class CatalogToolTip extends GradientToolTip {

	private Image image;

	private final CatalogDescriptor catalogDescriptor;

	public static void attachCatalogToolTip(Control control, CatalogDescriptor catalogDescriptor) {
		new CatalogToolTip(control, catalogDescriptor);
	}

	private CatalogToolTip(Control control, CatalogDescriptor catalogDescriptor) {
		super(control);
		this.catalogDescriptor = catalogDescriptor;
		hookDispose(control);
	}

	private void hookDispose(Control control) {
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				if (image != null) {
					image.dispose();
				}
			}
		});
	}

	@Override
	protected Composite createToolTipArea(Event event, Composite parent) {
		image = catalogDescriptor.getIcon().createImage();
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).equalWidth(false).applyTo(parent);
		createIcon(parent);
		createLabel(parent);
		createDescription(parent);
		return parent;
	}

	private void createDescription(Composite parent) {
		Label descriptionLabel = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(100, SWT.DEFAULT).applyTo(descriptionLabel);
		descriptionLabel.setBackground(null);
		descriptionLabel.setText(catalogDescriptor.getDescription());
	}

	private void createLabel(Composite parent) {
		Label nameLabel = new Label(parent, SWT.NULL);
		FontDescriptor h1FontDescriptor = createFontDescriptor(SWT.BOLD, 1.35f);
		nameLabel.setFont(h1FontDescriptor.createFont(image.getDevice()));
		nameLabel.setText(catalogDescriptor.getLabel());
		nameLabel.setBackground(null);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameLabel);
	}

	private void createIcon(Composite parent) {
		Label iconLabel = new Label(parent, SWT.NULL);
		iconLabel.setImage(image);
		iconLabel.setBackground(null);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).span(1, 2).applyTo(iconLabel);
	}

	private FontDescriptor createFontDescriptor(int style, float heightMultiplier) {
		Font baseFont = JFaceResources.getDialogFont();
		FontData[] fontData = baseFont.getFontData();
		FontData[] newFontData = new FontData[fontData.length];
		for (int i = 0; i < newFontData.length; i++) {
			newFontData[i] = new FontData(fontData[i].getName(), (int) (fontData[i].getHeight() * heightMultiplier),
					fontData[i].getStyle() | style);
		}
		return FontDescriptor.createFrom(newFontData);
	}
}