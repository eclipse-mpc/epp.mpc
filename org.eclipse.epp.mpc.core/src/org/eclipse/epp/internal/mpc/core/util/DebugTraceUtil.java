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
package org.eclipse.epp.internal.mpc.core.util;

import java.text.MessageFormat;

import org.eclipse.osgi.service.debug.DebugTrace;

public class DebugTraceUtil {

	public static void trace(DebugTrace trace, String option, String message) {
		if (trace != null) {
			trace.trace(option, message);
		}
	}

	public static void trace(DebugTrace trace, String option, String message, Object... parameters) {
		if (trace != null) {
			String formattedMessage = message;
			Throwable exception = null;
			if (parameters != null && parameters.length > 0) {
				formattedMessage = MessageFormat.format(message, parameters);
				for (int i = parameters.length - 1; i >= 0; i--) {
					Object p = parameters[i];
					if (p instanceof Throwable) {
						exception = (Throwable) p;
						break;
					}
				}
			}
			trace.trace(option, formattedMessage, exception);
		}
	}
}
