/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Indicates that a service is temporarily unavailable, equivalent to HTTP status code 503
 *
 * @author David Green
 */
@SuppressWarnings("serial")
public class ServiceUnavailableException extends CoreException {

	public ServiceUnavailableException(IStatus status) {
		super(status);
	}

}
