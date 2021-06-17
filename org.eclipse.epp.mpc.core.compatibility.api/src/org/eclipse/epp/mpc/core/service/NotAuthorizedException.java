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
package org.eclipse.epp.mpc.core.service;

import java.net.URI;

import org.eclipse.userstorage.util.ProtocolException;

public class NotAuthorizedException extends ProtocolException {

	public NotAuthorizedException() {
	}

	public NotAuthorizedException(Throwable cause) {
		initCause(cause);
		setStackTrace(cause.getStackTrace());
	}

	public NotAuthorizedException(ProtocolException exception) {
		super(exception.getMethod(), exception.getURI(), exception.getProtocolVersion(), exception.getStatusCode(),
				exception.getReasonPhrase());
		initCause(exception);
		setStackTrace(exception.getStackTrace());
	}

	public NotAuthorizedException(String method, URI uri, String protocolVersion, int statusCode, String reasonPhrase) {
		super(method, uri, protocolVersion, statusCode, reasonPhrase);
	}

	@Override
	public String getMessage() {
		String reasonPhrase = getReasonPhrase();
		if (reasonPhrase != null && !"".equals(reasonPhrase)) {
			return reasonPhrase;
		}
		return super.getMessage();
	}

	@Override
	public String toString() {
		String s = getClass().getName();
		String message = super.getMessage();
		return (message != null) ? (s + ": " + message) : s;
	}

}
