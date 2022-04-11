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
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceUnmarshaller;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.core.service.UnmarshalException;
import org.eclipse.osgi.util.NLS;

public class RemoteMarketplaceService<T> {

	protected URL baseUrl;

	public static final String API_URI_SUFFIX = "api/p"; //$NON-NLS-1$

	private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

	private static final int RETRY_COUNT = 3;

	protected final ITransport transport;

	protected final IMarketplaceUnmarshaller unmarshaller;

	private Map<String, String> requestMetaParameters;

	public RemoteMarketplaceService() {
		this.transport = TransportFactory.createTransport();
		IMarketplaceUnmarshaller unmarshaller = ServiceHelper.getMarketplaceUnmarshaller();
		if (unmarshaller == null) {
			//no unmarshaller registered, create a default instance
			unmarshaller = new MarketplaceUnmarshaller();
		}
		this.unmarshaller = unmarshaller;
	}

	protected IStatus createErrorStatus(String message, Throwable t) {
		return createStatus(IStatus.ERROR, message, t);
	}

	protected IStatus createErrorStatus(String messageTemplate, Object... parameters) {
		return createStatus(IStatus.ERROR, messageTemplate, parameters);
	}

	protected IStatus createStatus(int severity, String message, Throwable t) {
		return new Status(severity, MarketplaceClientCore.BUNDLE_ID, 0, message, t);
	}

	protected IStatus createStatus(int severity, String messageTemplate, Object... parameters) {
		String message = messageTemplate;
		Throwable exception = null;
		if (parameters != null && parameters.length > 0) {
			message = messageTemplate == null ? null : MessageFormat.format(messageTemplate, parameters);
			exception = findException(parameters);
		}
		return createStatus(severity, message, exception);
	}

	private static Throwable findException(Object... parameters) {
		if (parameters == null || parameters.length == 0) {
			return null;
		}
		for (int i = parameters.length - 1; i >= 0; i--) {
			if (parameters[i] instanceof Throwable) {
				return (Throwable) parameters[i];
			}
		}
		return null;
	}

	private void checkConfiguration() {
		if (baseUrl == null) {
			throw new IllegalStateException(Messages.DefaultMarketplaceService_mustConfigureBaseUrl);
		}
	}

	public URL getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(URL baseUrl) {
		this.baseUrl = baseUrl;
	}

	protected T processRequest(String relativeUrl, IProgressMonitor monitor) throws CoreException {
		return processRequest(relativeUrl, true, monitor);
	}

	protected T processRequest(String relativeUrl, boolean withMetaParams, IProgressMonitor monitor)
			throws CoreException {
		URI baseUri;
		try {
			baseUri = baseUrl.toURI();
		} catch (URISyntaxException e) {
			// should never happen
			throw new IllegalStateException(e);
		}

		return processRequest(baseUri.toString(), relativeUrl, withMetaParams, monitor);
	}

	protected T processRequest(String baseUri, String relativePath, IProgressMonitor monitor) throws CoreException {
		return processRequest(baseUri, relativePath, true, monitor);
	}

	@SuppressWarnings({ "unchecked" })
	protected T processRequest(String baseUri, String relativePath, boolean withMetaParams, IProgressMonitor monitor)
			throws CoreException {
		checkConfiguration();
		if (baseUri == null || relativePath == null) {
			throw new IllegalArgumentException();
		}

		String uri = URLUtil.appendPath(baseUri, relativePath);
		if (withMetaParams) {
			uri = addMetaParameters(uri);
		}

		URI location;
		try {
			location = new URI(uri);
		} catch (URISyntaxException e) {
			String message = NLS.bind(Messages.DefaultMarketplaceService_invalidLocation, uri);
			throw new CoreException(createErrorStatus(message, e));
		}

		int retry = 0;
		SubMonitor progress = SubMonitor.convert(monitor,
				NLS.bind(Messages.DefaultMarketplaceService_retrievingDataFrom, baseUri), 100);
		try {
			while (true) {
				progress.setWorkRemaining(100);
				try (InputStream in = transport.stream(location, progress.newChild(70));) {
					try {
						progress.setWorkRemaining(100);
						progress.worked(30);

						return (T) unmarshaller.unmarshal(in, Object.class, progress.newChild(70));//FIXME having T.class available here would be great...
					} catch (UnmarshalException e) {
						MarketplaceClientCore.error(
								NLS.bind(Messages.DefaultMarketplaceService_parseError, location.toString()), e);
						throw e;
					}
				} catch (Exception e) {
					if (e.getCause() instanceof OperationCanceledException) {
						throw new CoreException(Status.CANCEL_STATUS);
					}
					String causeMessage = e.getMessage();
					String message = NLS.bind(Messages.DefaultMarketplaceService_cannotCompleteRequest_reason,
							location.toString(), causeMessage);
					if (MarketplaceClientCore.isFailedDownloadException(e)) {
						if (++retry < RETRY_COUNT) {
							// retry on unreliable connections
							MarketplaceClientCore.getLog().log(createStatus(IStatus.INFO, message, e));
							continue;
						}
						IStatus connectionProblemStatus = MarketplaceClientCore.createConnectionProblemStatus(e);
						causeMessage = connectionProblemStatus.getMessage();
						e = new CoreException(connectionProblemStatus);
						//rebind with updated message
						message = NLS.bind(Messages.DefaultMarketplaceService_cannotCompleteRequest_reason, location.toString(),
								causeMessage);
					}
					throw new CoreException(createErrorStatus(message, e));
				}
			}
		} finally {
			monitor.done();
		}
	}

	public String addMetaParameters(String uri) {
		if (requestMetaParameters != null) {
			try {
				boolean hasQueryString = uri.indexOf('?') != -1;
				for (Map.Entry<String, String> param : requestMetaParameters.entrySet()) {
					if (param.getKey() == null) {
						continue;
					}
					if (hasQueryString) {
						uri += '&';
					} else {
						hasQueryString = true;
						uri += '?';
					}
					uri += URLEncoder.encode(param.getKey(), UTF_8);
					uri += '=';
					if (param.getValue() != null) {
						uri += URLEncoder.encode(param.getValue(), UTF_8);
					}
				}
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
		return uri;
	}

	/**
	 * The meta-parameters to be included in API requests, or null if there are none. Typically clients will use this
	 * facility to pass client meta-data to the server. For example, metadata might include the client identity,
	 * operating system, etc.
	 */
	public Map<String, String> getRequestMetaParameters() {
		return requestMetaParameters;
	}

	/**
	 * The meta-parameters to be included in API requests
	 *
	 * @param requestMetaParameters
	 *            the parameters or null if there should be none
	 */
	public void setRequestMetaParameters(Map<String, String> requestMetaParameters) {
		this.requestMetaParameters = requestMetaParameters;
	}

	protected static String urlEncode(String urlPart) {
		try {
			return URLEncoder.encode(urlPart, UTF_8);
		} catch (UnsupportedEncodingException e) {
			// should never happen
			throw new IllegalStateException(e);
		}
	}

	public void activate(Map<?, ?> properties) {
		if (properties != null) {
			URL url = ServiceUtil.getUrl(properties, IMarketplaceService.BASE_URL, null);
			if (url != null) {
				setBaseUrl(url);
			}
		}
	}

}
