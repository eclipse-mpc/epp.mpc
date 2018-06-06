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
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class FavoritesCatalog extends MarketplaceCatalog {

	@Override
	public synchronized IStatus performDiscovery(IProgressMonitor monitor) {
		return super.performDiscovery(monitor);
	}

	@Override
	public IStatus checkForUpdates(IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}
}
