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

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class CatalogListItem extends ControlListItem<CatalogDescriptor> {

	private Label nameLabel;

	private Label descriptionLabel;

	public CatalogListItem(Composite parent, int style, DiscoveryResources resources, ImageRegistry imageRegistry,
			CatalogDescriptor element) {
		super(parent, style, element);
		createContent(resources, imageRegistry);
	}

	private void createContent(DiscoveryResources resources, ImageRegistry imageRegistry) {
		CatalogDescriptor catalogDescriptor = getData();

		Composite parent = this;

		// GridDataFactory.fillDefaults().span(2, 1).applyTo(categoryHeaderContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).equalWidth(false).applyTo(parent);

		Label iconLabel = new Label(parent, SWT.NULL);
		if (catalogDescriptor.getIcon() != null) {
			String iconKey = catalogDescriptor.getUrl().toString() + "#icon"; //$NON-NLS-1$
			imageRegistry.put(iconKey, catalogDescriptor.getIcon());
			iconLabel.setImage(imageRegistry.get(iconKey));
		}
		iconLabel.setBackground(null);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).span(1, 2).applyTo(iconLabel);
		registerChild(iconLabel);

		nameLabel = new Label(parent, SWT.NULL);
		nameLabel.setFont(resources.getHeaderFont());
		nameLabel.setText(catalogDescriptor.getLabel());
		nameLabel.setBackground(null);
		registerChild(nameLabel);

		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameLabel);

		if (catalogDescriptor.getDescription() != null) {
			descriptionLabel = new Label(parent, SWT.WRAP);
			GridDataFactory.fillDefaults()
					.grab(true, false)
					.span(2, 1)
					.hint(100, SWT.DEFAULT)
					.applyTo(descriptionLabel);
			descriptionLabel.setBackground(null);
			descriptionLabel.setText(catalogDescriptor.getDescription());
			registerChild(descriptionLabel);
		}

	}

	@Override
	protected void refresh() {
		// nothing to do
	}

}
