/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.epp.internal.mpc.ui.operations.ProfileChangeOperationComputer.OperationType;
import org.eclipse.osgi.util.NLS;

/**
 * Represents kinds of provisioning operations supported by the wizard
 *
 * @author David Green
 * @deprecated will be replaced completely by {@link org.eclipse.epp.mpc.ui.Operation} in the future.
 */
@Deprecated
public enum Operation {
	INSTALL(OperationType.INSTALL, org.eclipse.epp.mpc.ui.Operation.INSTALL), //
	UNINSTALL(OperationType.UNINSTALL, org.eclipse.epp.mpc.ui.Operation.UNINSTALL), //
	CHECK_FOR_UPDATES(OperationType.UPDATE, org.eclipse.epp.mpc.ui.Operation.UPDATE), //
	NONE(null, org.eclipse.epp.mpc.ui.Operation.NONE);

	private final OperationType operationType;

	private final org.eclipse.epp.mpc.ui.Operation operation;

	private Operation(OperationType operationType, org.eclipse.epp.mpc.ui.Operation operation) {
		this.operationType = operationType;
		this.operation = operation;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public String getLabel() {
		return operation.getLabel();
	}

	public org.eclipse.epp.mpc.ui.Operation getOperation() {
		return operation;
	}

	public static Operation map(org.eclipse.epp.mpc.ui.Operation operation) {
		if (operation == null) {
			return null;
		}
		switch (operation) {
		case INSTALL:
			return INSTALL;
		case UNINSTALL:
			return UNINSTALL;
		case UPDATE:
			return CHECK_FOR_UPDATES;
		case NONE:
			return NONE;
		default:
			throw new IllegalArgumentException(NLS.bind(Messages.Operation_unknownOperation, operation));
		}
	}

	public static org.eclipse.epp.mpc.ui.Operation mapBack(Operation operation) {
		if (operation == null) {
			return null;
		}
		switch (operation) {
		case INSTALL:
			return org.eclipse.epp.mpc.ui.Operation.INSTALL;
		case UNINSTALL:
			return org.eclipse.epp.mpc.ui.Operation.UNINSTALL;
		case CHECK_FOR_UPDATES:
			return org.eclipse.epp.mpc.ui.Operation.UPDATE;
		case NONE:
			return org.eclipse.epp.mpc.ui.Operation.NONE;
		default:
			throw new IllegalArgumentException(NLS.bind(Messages.Operation_unknownOperation, operation));
		}
	}

	public static <T> Map<T, Operation> mapAll(Map<T, org.eclipse.epp.mpc.ui.Operation> operations) {
		if (operations == null) {
			return null;
		}
		Map<T, Operation> mappedOperations = new LinkedHashMap<T, Operation>();
		Set<Map.Entry<T, org.eclipse.epp.mpc.ui.Operation>> entrySet = operations.entrySet();
		for (Map.Entry<T, org.eclipse.epp.mpc.ui.Operation> entry : entrySet) {
			mappedOperations.put(entry.getKey(), map(entry.getValue()));
		}
		return mappedOperations;
	}

	public static <T> Map<T, org.eclipse.epp.mpc.ui.Operation> mapAllBack(Map<T, Operation> operations) {
		if (operations == null) {
			return null;
		}
		Map<T, org.eclipse.epp.mpc.ui.Operation> mappedOperations = new LinkedHashMap<T, org.eclipse.epp.mpc.ui.Operation>();
		Set<Map.Entry<T, Operation>> entrySet = operations.entrySet();
		for (Map.Entry<T, Operation> entry : entrySet) {
			mappedOperations.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().getOperation());
		}
		return mappedOperations;
	}
}
