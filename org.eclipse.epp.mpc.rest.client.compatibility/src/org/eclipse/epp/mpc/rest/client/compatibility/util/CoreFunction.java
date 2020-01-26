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
package org.eclipse.epp.mpc.rest.client.compatibility.util;

import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

@FunctionalInterface
public interface CoreFunction<T, R> {
	R apply(T t) throws CoreException;

	default Function<T, R> wrap() {
		return t -> {
			try {
				return apply(t);
			} catch (CoreException ex) {
				throw new RuntimeCoreException(ex);
			}
		};
	}
}