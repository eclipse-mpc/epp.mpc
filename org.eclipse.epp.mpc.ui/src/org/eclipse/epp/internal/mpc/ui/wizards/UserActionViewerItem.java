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

import org.eclipse.equinox.internal.p2.ui.discovery.wizards.AbstractDiscoveryItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

public abstract class UserActionViewerItem<E> extends AbstractDiscoveryItem<E> {

	protected final MarketplaceViewer viewer;
	protected final DiscoveryResources resources;
	protected final IShellProvider shellProvider;

	public UserActionViewerItem(Composite parent, DiscoveryResources resources, IShellProvider shellProvider, E element,
			MarketplaceViewer viewer) {
		super(parent, SWT.NULL, resources, element);
		this.resources = resources;
		this.shellProvider = shellProvider;
		this.viewer = viewer;
	}

	protected void createContent() {
		Composite parent = this;

		GridLayoutFactory.swtDefaults().applyTo(parent);

		Control link = createActionLink(parent);
		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				actionPerformed();
			}
		});

		GridDataFactory.swtDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(link);
	}

	protected Control createActionLink(Composite parent) {
		Link link = new Link(parent, SWT.NONE);
		link.setBackground(null);
		String linkText = getLinkText();
		if (linkText == null || "".equals(linkText)) { //$NON-NLS-1$
			throw new IllegalArgumentException();
		}
		link.setText(linkText);
		String toolTipText = getLinkToolTipText();
		if (toolTipText != null && !"".equals(toolTipText)) { //$NON-NLS-1$
			link.setToolTipText(toolTipText);
		}
		return link;
	}

	protected abstract String getLinkText();

	protected String getLinkToolTipText() {
		return getToolTipText();
	}

	protected abstract void actionPerformed();

	@Override
	protected void refresh() {
		// ignore
	}
}