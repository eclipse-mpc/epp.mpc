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
package org.eclipse.epp.internal.mpc.ui.catalog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class FavoritesCatalog extends MarketplaceCatalog {

	@Override
	public IStatus performDiscovery(IProgressMonitor monitor) {
		return super.performDiscovery(monitor);
	}

	@Override
	public IStatus checkForUpdates(IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}
}
