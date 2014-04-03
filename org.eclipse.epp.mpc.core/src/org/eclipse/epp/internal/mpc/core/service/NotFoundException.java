/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

/**
 * @author David Green
 * @deprecated unused, will be removed in a future version
 */
@Deprecated
@SuppressWarnings("serial")
public class NotFoundException extends Exception {

	public NotFoundException(String message) {
		super(message);
	}

}
