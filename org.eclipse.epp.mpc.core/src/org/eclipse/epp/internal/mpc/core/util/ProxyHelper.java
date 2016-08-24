/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Carsten Reckord
 */
public class ProxyHelper {
	private static ProxyAuthenticator authenticator;

	@SuppressWarnings("rawtypes")
	private static ServiceTracker proxyServiceTracker;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static synchronized void acquireProxyService() {
		if (proxyServiceTracker == null) {
			proxyServiceTracker = new ServiceTracker(MarketplaceClientCorePlugin.getBundle().getBundleContext(),
					IProxyService.class.getName(), null);
			proxyServiceTracker.open();
		}
		setAuthenticator();
	}

	public static void setAuthenticator() {
		Authenticator defaultAuthenticator = getDefaultAuthenticator();
		if (authenticator == null || authenticator != defaultAuthenticator) {
			if (defaultAuthenticator instanceof ProxyAuthenticator) {
				authenticator = (ProxyAuthenticator) defaultAuthenticator;
			} else {
				authenticator = new ProxyAuthenticator(defaultAuthenticator);
			}
			Authenticator.setDefault(authenticator);
		}
	}

	public static synchronized void releaseProxyService() {
		unsetAuthenticator();
		if (proxyServiceTracker != null) {
			proxyServiceTracker.close();
		}
	}

	public static void unsetAuthenticator() {
		Authenticator defaultAuthenticator = getDefaultAuthenticator();
		if (authenticator != null) {
			if (defaultAuthenticator == authenticator) {
				Authenticator.setDefault(authenticator.getDelegate());
			}
			authenticator = null;
		}
	}

	public static IProxyData getProxyData(String url) {
		final IProxyService proxyService = getProxyService();
		if (proxyService != null) {
			URI uri;
			try {
				uri = new URI(url);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			return doGetProxyData(proxyService, uri);
		}
		return null;
	}

	public static IProxyData getProxyData(URI uri) {
		final IProxyService proxyService = getProxyService();
		if (proxyService != null) {
			return doGetProxyData(proxyService, uri);
		}
		return null;
	}

	private static IProxyData doGetProxyData(final IProxyService proxyService, URI uri) {
		final IProxyData[] proxyData = proxyService.select(uri);
		if (proxyData != null && proxyData.length > 0 && proxyData[0] != null) {
			final IProxyData pd = proxyData[0];
			return pd;
		}
		return null;
	}

	protected static IProxyService getProxyService() {
		return proxyServiceTracker == null ? null : (IProxyService) proxyServiceTracker.getService();
	}

	private static Authenticator getDefaultAuthenticator() {
		try {
			final Field authenticatorField = Authenticator.class.getDeclaredField("theAuthenticator"); //$NON-NLS-1$
			final boolean accessible = authenticatorField.isAccessible();
			try {
				if (!accessible) {
					authenticatorField.setAccessible(true);
				}
				return (Authenticator) authenticatorField.get(null);
			} finally {
				if (!accessible) {
					authenticatorField.setAccessible(false);
				}
			}
		} catch (Exception e) {
			MarketplaceClientCore.getLog().log(new Status(IStatus.WARNING, MarketplaceClientCore.BUNDLE_ID, Messages.ProxyHelper_replacingAuthenticator, e));
		}
		return null;
	}

	private static class ProxyAuthenticator extends Authenticator {
		private final Authenticator delegate;

		public ProxyAuthenticator(Authenticator delegate) {
			if (delegate instanceof ProxyAuthenticator) {
				delegate = ((ProxyAuthenticator) delegate).getDelegate();
			}
			this.delegate = delegate;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			if (getRequestorType() == RequestorType.PROXY) {
				IProxyService proxyService = getProxyService();
				if (proxyService != null && proxyService.isProxiesEnabled()) {
					URL requestingURL = getRequestingURL();
					IProxyData[] proxies;
					if (requestingURL == null) {
						proxies = proxyService.getProxyData();
					} else {
						try {
							proxies = proxyService.select(requestingURL.toURI());
						} catch (URISyntaxException e) {
							proxies = proxyService.getProxyData();
						}
					}
					for (IProxyData proxyData : proxies) {
						// make sure we don't hand out credentials to the wrong proxy
						if (proxyData.isRequiresAuthentication() && proxyData.getPort() == getRequestingPort()
								&& proxyData.getHost().equals(getRequestingHost())) {
							String userId = proxyData.getUserId();
							String password = proxyData.getPassword();
							if (userId != null && password != null) {
								return new PasswordAuthentication(userId, password
										.toCharArray());
							}
						}
					}
				}
			}
			if (delegate != null) {
				// Pass on to previously registered authenticator
				// Eclipse UI bundle registers one to query credentials from user
				try {
					Authenticator.setDefault(delegate);
					String requestingHost = getRequestingHost();
					InetAddress requestingSite = getRequestingSite();
					int requestingPort = getRequestingPort();
					String requestingProtocol = getRequestingProtocol();
					String requestingPrompt = getRequestingPrompt();
					String requestingScheme = getRequestingScheme();
					URL requestingURL = getRequestingURL();
					RequestorType requestorType = getRequestorType();
					if (requestingSite == null) {
						try {
							requestingSite = InetAddress.getByName(requestingHost);
						} catch (Exception ex) {
							//ignore
						}
					}
					if (requestingPrompt == null) {
						//Help the Eclipse UI password dialog with its prompt
						String promptHost = requestingSite == null
								? String.format("%s:%s", requestingHost, requestingPort)
										: requestingHost == null ? requestingSite.getHostName() : requestingHost;
										String promptType = requestorType.toString().toLowerCase();
										requestingPrompt = MessageFormat.format("{0} authentication for {1} {2}", requestingScheme,
												promptType, promptHost);
					}
					return Authenticator.requestPasswordAuthentication(requestingHost, requestingSite,
							requestingPort, requestingProtocol, requestingPrompt, requestingScheme,
							requestingURL, requestorType);
				} finally {
					Authenticator.setDefault(this);
				}
			}
			return null;
		}

		public Authenticator getDelegate() {
			return delegate;
		}
	}
}
