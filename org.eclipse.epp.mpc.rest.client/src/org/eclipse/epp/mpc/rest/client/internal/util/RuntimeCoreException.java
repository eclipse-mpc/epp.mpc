/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.rest.client.internal.util;

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

public class RuntimeCoreException extends RuntimeException {

	public RuntimeCoreException(CoreException cause) {
		super(Objects.requireNonNull(cause, "cause")); //$NON-NLS-1$
	}

	@Override
	public synchronized CoreException getCause() {
		return (CoreException) super.getCause();
	}

	@Override
	public synchronized RuntimeCoreException initCause(Throwable cause) {
		return initCause((CoreException) cause);
	}

	public RuntimeCoreException initCause(CoreException cause) {
		return (RuntimeCoreException) super.initCause(cause);
	}

	public IStatus getStatus() {
		return getCause().getStatus();
	}

	@Override
	public String getMessage() {
		return getCause().getMessage();
	}

	@Override
	public String getLocalizedMessage() {
		return getCause().getLocalizedMessage();
	}
}
