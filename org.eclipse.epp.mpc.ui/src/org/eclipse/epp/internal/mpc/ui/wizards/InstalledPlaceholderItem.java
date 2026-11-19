/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

/**
 * Placeholder shown on the Installed tab when no solutions installed from the Marketplace were found, in the spirit
 * of the hints in an empty editor area.
 */
class InstalledPlaceholderItem extends ControlListItem<Object> {

	static final Object ELEMENT = new Object();

	InstalledPlaceholderItem(Composite parent, Runnable showSearch) {
		super(parent, SWT.NONE, ELEMENT);
		LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), this);
		setBackgroundMode(SWT.INHERIT_FORCE);
		GridLayoutFactory.fillDefaults().margins(10, 40).spacing(0, 12).applyTo(this);

		Label icon = new Label(this, SWT.NONE);
		ResourceLocator.imageDescriptorFromBundle(InstalledPlaceholderItem.class, "icons/marketplace_placeholder.svg") //$NON-NLS-1$
				.ifPresent(marketplaceIcon -> icon.setImage(
						resources.create(ImageDescriptor.createWithFlags(marketplaceIcon, SWT.IMAGE_GRAY))));
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).grab(true, false).applyTo(icon);

		Label message = new Label(this, SWT.CENTER | SWT.WRAP);
		message.setText(Messages.InstalledPlaceholderItem_message);
		message.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR));
		GridDataFactory.fillDefaults().grab(true, false).hint(1, SWT.DEFAULT).applyTo(message);

		if (showSearch != null) {
			Link search = new Link(this, SWT.NONE);
			search.setText(Messages.InstalledPlaceholderItem_search);
			search.addListener(SWT.Selection, e -> showSearch.run());
			GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).grab(true, false).applyTo(search);
		}
	}

	@Override
	public void updateColors(int index) {
		// neither alternating nor selection colors, blend into the list
		setBackground(getParent().getBackground());
	}

	@Override
	protected void refresh() {
		// static content
	}
}
