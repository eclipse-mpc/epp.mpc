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
package org.eclipse.epp.internal.mpc.core.util;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.osgi.util.NLS;

class ProxyAuthenticator extends Authenticator {
	private static final String EGIT_AUTHENTICATOR_CLASS = "org.eclipse.egit.core.EclipseAuthenticator"; //$NON-NLS-1$

	private final Authenticator delegate;

	private final Authenticator previousAuthenticator;

	public ProxyAuthenticator(Authenticator delegate) {
		if (delegate instanceof ProxyAuthenticator) {
			delegate = ((ProxyAuthenticator) delegate).getDelegate();
		}
		this.previousAuthenticator = delegate;
		this.delegate = fixDelegate(delegate);
	}

	private static Authenticator fixDelegate(Authenticator delegate) {
		if (delegate == null) {
			return null;
		}
		if (EGIT_AUTHENTICATOR_CLASS.equals(delegate.getClass().getName())) {
			Authenticator replacement = getPluggedInNonEGitAuthenticator();
			if (replacement != null) {
				return replacement;
			}
		}
		return delegate;
	}

	private static Authenticator getPluggedInNonEGitAuthenticator() {
		@SuppressWarnings("restriction")
		IExtension[] extensions = RegistryFactory.getRegistry()
				.getExtensionPoint(org.eclipse.core.internal.net.Activator.ID,
						org.eclipse.core.internal.net.Activator.PT_AUTHENTICATOR)
				.getExtensions();
		if (extensions.length == 0) {
			return null;
		}
		IConfigurationElement config = null;
		for (IExtension extension : extensions) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			if (configs.length == 0) {
				continue;
			}
			String className = configs[0].getAttribute("class"); //$NON-NLS-1$
			if (className != null && !EGIT_AUTHENTICATOR_CLASS.equals(className)) {
				config = configs[0];
				break;
			}
		}
		if (config != null) {
			try {
				return (Authenticator) config.createExecutableExtension("class");//$NON-NLS-1$
			} catch (CoreException ex) {
				MarketplaceClientCore.error(NLS.bind("Unable to instantiate authenticator {0}", //$NON-NLS-1$
						(new Object[] { config.getDeclaringExtension().getUniqueIdentifier() })), ex);
			}
		}
		return null;
	}

	private boolean hostMatches(final IProxyData proxy) {
		String proxyHost = proxy.getHost();
		if (proxyHost == null) {
			return false;
		}
		try {
			InetAddress requestingAddress = getRequestingSite();
			if (requestingAddress != null) {
				final InetAddress proxyAddress = InetAddress.getByName(proxyHost);
				return proxyAddress.equals(requestingAddress);
			}
		} catch (UnknownHostException err) {
			return false;
		}
		String requestingHost = getRequestingHost();
		if (requestingHost != null && requestingHost.equals(proxyHost)) {
			return true;
		}
		return false;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		IProxyService proxyService = ProxyHelper.getProxyService();
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
						&& hostMatches(proxyData)) {
					String userId = proxyData.getUserId();
					String password = proxyData.getPassword();
					if (userId != null && password != null) {
						return new PasswordAuthentication(userId, password.toCharArray());
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
					String promptHost = requestingSite == null ? String.format("%s:%s", requestingHost, requestingPort) //$NON-NLS-1$
							: requestingHost == null ? requestingSite.getHostName() : requestingHost;
							String promptType = requestorType.toString().toLowerCase();
							requestingPrompt = MessageFormat.format(Messages.ProxyAuthenticator_prompt, requestingScheme,
									promptType, promptHost);
				}
				return Authenticator.requestPasswordAuthentication(requestingHost, requestingSite, requestingPort,
						requestingProtocol, requestingPrompt, requestingScheme, requestingURL, requestorType);
			} finally {
				Authenticator.setDefault(this);
			}
		}
		return null;
	}

	public Authenticator getDelegate() {
		return delegate;
	}

	public void install() {
		Authenticator.setDefault(this);
	}

	public void uninstall() {
		synchronized (Authenticator.class) {
			Authenticator defaultAuthenticator = ProxyHelper.getDefaultAuthenticator();
			if (defaultAuthenticator == this) {
				Authenticator.setDefault(previousAuthenticator);
			}
		}
	}
}