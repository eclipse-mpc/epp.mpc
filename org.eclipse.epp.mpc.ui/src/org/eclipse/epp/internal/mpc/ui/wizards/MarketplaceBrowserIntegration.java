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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;

/**
 * Browser integration for the marketplace that intercepts calls to install buttons and causes them to open the
 * marketplace wizard.
 * 
 * @author dgreen
 */
public class MarketplaceBrowserIntegration implements LocationListener, OpenWindowListener {

	private final List<CatalogDescriptor> catalogDescriptors;

	private final CatalogDescriptor catalogDescriptor;

	public MarketplaceBrowserIntegration(List<CatalogDescriptor> catalogDescriptors, CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptors == null || catalogDescriptors.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		this.catalogDescriptors = new ArrayList<CatalogDescriptor>(catalogDescriptors);
		this.catalogDescriptor = catalogDescriptor;
	}

	public void open(WindowEvent event) {
		// if the user shift-clicks the button this can happen
		System.out.println("event:" + event.data);
	}

	public void changing(LocationEvent event) {
		System.out.println("event:" + event.location);
	}

	public void changed(LocationEvent event) {
		// nothing to do.
	}

}
