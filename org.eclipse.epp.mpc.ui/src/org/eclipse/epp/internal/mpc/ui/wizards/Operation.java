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

import org.eclipse.epp.internal.mpc.ui.operations.ProvisioningOperation.OperationType;

/**
 * Represents kinds of provisioning operations supported by the wizard
 * 
 * @author David Green
 */
public enum Operation {
	INSTALL(OperationType.INSTALL, Messages.Operation_install), //
	UNINSTALL(OperationType.UNINSTALL, Messages.Operation_uninstall), //
	CHECK_FOR_UPDATES(OperationType.UPDATE, Messages.Operation_update), // 
	NONE(null, null);

	private final OperationType operationType;

	private final String label;

	private Operation(OperationType operationType, String label) {
		this.operationType = operationType;
		this.label = label;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public String getLabel() {
		return label;
	}
}
