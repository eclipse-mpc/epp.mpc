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
package org.eclipse.epp.internal.mpc.core.service;

import java.net.URI;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.userstorage.internal.Session;
import org.eclipse.userstorage.util.ProtocolException;

public abstract class AbstractDataStorageService {

	public static class NotAuthorizedException extends ProtocolException {

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

		public NotAuthorizedException(String method, URI uri, String protocolVersion, int statusCode,
				String reasonPhrase) {
			super(method, uri, protocolVersion, statusCode, reasonPhrase);
		}
	}

	private IMarketplaceStorageService storageService;

	public IMarketplaceStorageService getStorageService() {
		return storageService;
	}

	public void setStorageService(IMarketplaceStorageService storageService) {
		this.storageService = storageService;
	}

	protected static Exception processProtocolException(Exception exception) {
		for (Throwable ex = exception; ex != null; ex = ex.getCause()) {
			if (ex instanceof ProtocolException) {
				ProtocolException protocolException = (ProtocolException) ex;
				return processProtocolException(protocolException);
			}
			if (ex instanceof OperationCanceledException) {
				OperationCanceledException oce = (OperationCanceledException) ex;
				return processProtocolException(oce);
			}
		}
		return exception;
	}

	protected static ProtocolException processProtocolException(OperationCanceledException ex) {
		return new NotAuthorizedException(ex);
	}

	protected static ProtocolException processProtocolException(ProtocolException ex) {
		if (ex.getStatusCode() == Session.AUTHORIZATION_REQUIRED || ex.getStatusCode() == Session.FORBIDDEN) {
			return new NotAuthorizedException(ex);
		}
		return ex;
	}

	public void bindStorageService(IMarketplaceStorageService storageService) {
		setStorageService(storageService);
	}

	public void unbindStorageService(IMarketplaceStorageService storageService) {
		if (getStorageService() == storageService) {
			setStorageService(null);
		}
	}
}