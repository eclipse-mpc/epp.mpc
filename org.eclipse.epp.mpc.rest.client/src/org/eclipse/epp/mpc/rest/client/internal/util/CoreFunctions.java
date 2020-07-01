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

import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

public final class CoreFunctions {

	private CoreFunctions() {
	}

	public static <T> T unwrap(CoreCallable<T> call) throws CoreException {
		try {
			return call.call();
		} catch (RuntimeCoreException ex) {
			throw ex.getCause();
		}
	}

	public static <T, R> Function<T, R> wrap(CoreFunction<T, R> f) {
		return f.wrap();
	}
}
