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
import java.text.MessageFormat;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

class ProxyAuthenticator extends Authenticator {
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
							&& proxyData.getHost().equals(getRequestingHost())) {
						String userId = proxyData.getUserId();
						String password = proxyData.getPassword();
						if (userId != null && password != null) {
							return new PasswordAuthentication(userId, password.toCharArray());
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
					String promptHost = requestingSite == null ? String.format("%s:%s", requestingHost, requestingPort)
							: requestingHost == null ? requestingSite.getHostName() : requestingHost;
							String promptType = requestorType.toString().toLowerCase();
							requestingPrompt = MessageFormat.format("{0} authentication for {1} {2}", requestingScheme,
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
				Authenticator.setDefault(delegate);
			}
		}
	}
}