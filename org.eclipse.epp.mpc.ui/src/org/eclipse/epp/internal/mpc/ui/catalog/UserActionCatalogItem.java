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

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

public class UserActionCatalogItem extends CatalogItem {
	public static enum UserAction {
		BROWSE, LOGIN, CREATE_FAVORITES, FAVORITES_UNSUPPORTED, RETRY_ERROR, INFO, OPEN_FAVORITES;
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
