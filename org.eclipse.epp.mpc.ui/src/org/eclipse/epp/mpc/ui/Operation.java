/*******************************************************************************
 * Copyright (c) 2014, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;


/**
 * Represents kinds of provisioning operations supported by the wizard
 */
public enum Operation {
	INSTALL(Messages.Operation_install), //
	UNINSTALL(Messages.Operation_uninstall), //
	UPDATE(Messages.Operation_update), //
	CHANGE(Messages.Operation_change), //
	NONE(null);

	private final String label;

	private Operation(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
