/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.http.NoHttpResponseException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 */
public class MarketplaceClientCore {

	public static final String BUNDLE_ID = "org.eclipse.epp.mpc.core"; //$NON-NLS-1$

	private static final String STREAM_CLOSED_MESSAGE = "Stream closed"; //$NON-NLS-1$

	private static final String PIPE_CLOSED_MESSAGE = "Pipe closed"; //$NON-NLS-1$

	private static final String PIPE_BROKEN_MESSAGE = "Pipe broken"; //$NON-NLS-1$

	public static ILog getLog() {
		return Platform.getLog(MarketplaceClientCorePlugin.getBundle());
	}

	public static void error(String message, Throwable exception) {
		if (message == null) {
			message = NLS.bind(Messages.MarketplaceClientCore_unexpectedException, exception.getMessage());
		}
		getLog().log(new Status(IStatus.ERROR, BUNDLE_ID, IStatus.ERROR, message, exception));
	}

	public static void error(Throwable exception) {
		error(null, exception);
	}

	public static IStatus computeStatus(Exception e, String message) {
		Throwable cause = e;
		if (e instanceof InvocationTargetException) {
			cause = e.getCause();
		}
		IStatus statusCause = computeWellknownProblemStatus(e);
		if (statusCause == null) {
			if (cause instanceof CoreException) {
				statusCause = ((CoreException) cause).getStatus();
			} else {
				statusCause = new Status(IStatus.ERROR, BUNDLE_ID, cause.getMessage(), cause);
			}
		}
		if (statusCause.getMessage() != null) {
			message = NLS.bind(Messages.MarketplaceClientCore_message_message2, message, statusCause.getMessage());
		}
		IStatus status = new MultiStatus(BUNDLE_ID, 0, new IStatus[] { statusCause }, message, cause);
		return status;
	}

	public static IStatus computeWellknownProblemStatus(Throwable exception) {
		IStatus status = null;
		while (exception != null) {
			if (exception instanceof FileNotFoundException) {
				// exception message is the URL
				status = new Status(IStatus.ERROR, BUNDLE_ID,
						NLS.bind(Messages.MarketplaceClientCore_notFound, exception.getMessage()), exception);
				break;
			}
			// name resolution didn't work - possibly offline...
			if (exception instanceof UnknownHostException) {
				status = new Status(IStatus.ERROR, BUNDLE_ID,
						NLS.bind(Messages.MarketplaceClientCore_unknownHost, exception.getMessage()), exception);
				break;
			}
			// could be a previously resolved name, but now unreachable because we're offline...
			if (exception instanceof NoRouteToHostException) {
				status = new Status(IStatus.ERROR, BUNDLE_ID,
						NLS.bind(Messages.MarketplaceClientCore_unknownHost, exception.getMessage()), exception);
				break;
			}
			// some oddly configured networks throw timeouts instead of DNS or routing errors
			if (exception instanceof ConnectException) {
				status = createConnectionProblemStatus(exception);
				break;
			}
			// no specific details on this one, but could still point to network issues
			if (exception instanceof SocketException) {
				status = createConnectionProblemStatus(exception);
				break;
			}
			if (exception instanceof SocketTimeoutException) {
				status = createConnectionProblemStatus(exception);
				break;
			}
			if (exception instanceof NoHttpResponseException) {
				status = createConnectionProblemStatus(exception);
				break;
			}
			if (exception instanceof CoreException) {
				IStatus exceptionStatus = ((CoreException) exception).getStatus();
				if (MarketplaceClientCore.BUNDLE_ID.equals(exceptionStatus.getPlugin())
						&& exceptionStatus.getCode() == 503) {
					//received service unavailable error from P2 transport
					status = new Status(IStatus.ERROR, BUNDLE_ID, exceptionStatus.getMessage(), exception);
					break;
				}
			}
			Throwable cause = exception.getCause();
			if (cause != exception) {
				exception = cause;
			} else {
				break;
			}
		}
		return status;
	}

	public static IStatus createConnectionProblemStatus(Throwable exception) {
		IStatus status = new Status(IStatus.ERROR, BUNDLE_ID,
				NLS.bind(Messages.MarketplaceClientCore_connectionProblem, exception.getMessage()), exception);
		return status;
	}

	public static boolean isStreamClosedException(Throwable exception) {
		return isIOExceptionWithMessage(exception, STREAM_CLOSED_MESSAGE);
	}

	public static boolean isPipeClosedException(Throwable exception) {
		return isIOExceptionWithMessage(exception, PIPE_CLOSED_MESSAGE);
	}

	public static boolean isPipeBrokenException(Throwable exception) {
		return isIOExceptionWithMessage(exception, PIPE_BROKEN_MESSAGE);
	}

	public static boolean isFailedDownloadException(Throwable exception) {
		return isStreamClosedException(exception) || isPipeClosedException(exception)
				|| isPipeBrokenException(exception);
	}

	private static boolean isIOExceptionWithMessage(Throwable exception, String message) {
		while (exception != null) {
			if (exception instanceof IOException) {
				IOException ioException = (IOException) exception;
				return message.equals(ioException.getMessage());
			}
			Throwable cause = exception.getCause();
			if (cause != exception) {
				exception = cause;
			} else {
				break;
			}
		}
		return false;
	}
}
