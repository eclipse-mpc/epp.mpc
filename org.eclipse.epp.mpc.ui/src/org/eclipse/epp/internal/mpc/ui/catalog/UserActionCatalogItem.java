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

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

public class UserActionCatalogItem extends CatalogItem {
	public static enum UserAction {
		BROWSE, RETRY_ERROR, INFO, UPDATE;
	}

	private UserAction userAction;

	public UserActionCatalogItem() {
		super();
		// ignore
	}

	public UserAction getUserAction() {
		return userAction;
	}

	public void setUserAction(UserAction userAction) {
		this.userAction = userAction;
	}
}
