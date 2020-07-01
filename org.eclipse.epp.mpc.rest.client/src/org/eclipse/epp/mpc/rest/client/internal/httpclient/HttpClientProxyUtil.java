/*
 * Copyright (c) 2015, 2018 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.epp.mpc.rest.client.internal.httpclient;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Objects;

import org.apache.http.HttpHost;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This code is based on {@link org.eclipse.userstorage.internal.util.ProxyUtil} and has been copied here to avoid
 * classloader issues with the imported Apache HttpClient packages (see bug 497729).
 *
 * @author Eike Stepper
 * @author Carsten Reckord
 */
@SuppressWarnings("restriction")
public final class HttpClientProxyUtil {
	private static final String PROP_HTTP_AUTH_NTLM_DOMAIN = "http.auth.ntlm.domain";

	private static final String ENV_USER_DOMAIN = "USERDOMAIN";

	private static final char BACKSLASH = '\\';

	private static final char SLASH = '/';

	private static String workstation;

	private HttpClientProxyUtil() {
	}

//	public static Executor proxyAuthentication(Executor executor, URI uri)
//			throws IOException {
//		IProxyData proxy = ProxyHelper.getProxyData(uri);
//		if (proxy != null) {
//			HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
//			String proxyUserID = proxy.getUserId();
//			if (proxyUserID != null) {
//				String domainUserID = getNTLMUserName(proxyUserID);
//				String password = proxy.getPassword();
//				String domain = getNTLMUserDomain(proxyUserID);
//				if (domain != null || !proxyUserID.equals(domainUserID)) {
//					String workstation = getNTLMWorkstation();
//					executor.auth(new AuthScope(proxyHost, AuthScope.ANY_REALM, "ntlm"),
//							new NTCredentials(domainUserID, password, workstation, domain));
//				}
//				return executor.auth(new AuthScope(proxyHost, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME),
//						new UsernamePasswordCredentials(proxyUserID, password));
//			}
//		}
//
//		return executor;
//	}

	static String getNTLMWorkstation() {
		if (workstation != null) {
			return "".equals(workstation) ? null : workstation; //$NON-NLS-1$
		}
		String dnsHostname = getDnsHostname();
		if (dnsHostname != null) {
			workstation = dnsHostname;
			return dnsHostname;
		}
		String hostName = getLocalEnvHostname();
		if (hostName != null) {
			workstation = hostName;
			return hostName;
		}
		workstation = ""; //$NON-NLS-1$
		return null;
	}

	private static String getLocalEnvHostname() {
		String computerName = System.getenv("COMPUTERNAME");
		String hostName = System.getenv("HOSTNAME");
		if (computerName != null) {
			if (hostName != null && !computerName.equals(hostName)) {
				if (isOsWindows()) {
					hostName = computerName;
				}
			} else {
				hostName = computerName;
			}
		}
		return hostName;
	}

	private static String getDnsHostname() {
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			if (!localHost.isLoopbackAddress()) {
				String hostName = localHost.getHostName();
				if (hostName != null && !"".equals(hostName) && !"localhost".equals(hostName)) {
					return hostName;
				}
			}
		} catch (UnknownHostException e) {
		}
		return null;
	}

	private static boolean isOsWindows() {
		Bundle bundle = FrameworkUtil.getBundle(HttpClientProxyUtil.class);
		String os = bundle.getBundleContext().getProperty("osgi.os");
		return "win32".equals(os);
	}

	static String getNTLMUserDomain(String userName) {
		if (userName != null) {
			int pos = userName.indexOf(BACKSLASH);
			if (pos != -1) {
				return userName.substring(0, pos);
			}
			pos = userName.indexOf(SLASH);
			if (pos != -1) {
				return userName.substring(0, pos);
			}
		}
		String domain = System.getProperty(PROP_HTTP_AUTH_NTLM_DOMAIN);
		if (domain != null) {
			return domain;
		}

		//FIXME some systems seem to need the DNS domain name instead of the NetBIOS name (from USERDNSDOMAIN env variable)
		domain = System.getenv(ENV_USER_DOMAIN);
		if (domain != null) {
			return domain;
		}

		return null;
	}

	static String getNTLMUserName(String userName) {
		if (userName != null) {
			int pos = userName.indexOf(BACKSLASH);
			if (userName.charAt(pos + 1) == BACKSLASH) {
				pos++;
			}
			if (pos != -1) {
				return userName.substring(pos + 1);
			}
			pos = userName.indexOf(SLASH);
			if (userName.charAt(pos + 1) == SLASH) {
				pos++;
			}
			if (pos != -1) {
				return userName.substring(pos + 1);
			}
		}

		return userName;
	}

	static HttpHost getProxyHost(IProxyService proxyService, URI uri) {
		IProxyData proxy = getProxyData(proxyService, uri);
		if (proxy != null) {
			if (IProxyData.HTTPS_PROXY_TYPE.equals(proxy.getType())
					|| IProxyData.HTTP_PROXY_TYPE.equals(proxy.getType())) {
				return new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getType());
			}
			//SOCKS proxies are handled by Java on the socket level
			return null;
		}

		return null;
	}

	static IProxyData getProxyData(IProxyService proxyService, String url) {
		Objects.requireNonNull(proxyService, "proxyService");
		Objects.requireNonNull(url, "url");
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return doGetProxyData(proxyService, uri);
	}

	static IProxyData getProxyData(IProxyService proxyService, URI uri) {
		Objects.requireNonNull(proxyService, "proxyService");
		Objects.requireNonNull(uri, "uri");
		return doGetProxyData(proxyService, uri);
	}

	private static IProxyData doGetProxyData(final IProxyService proxyService, URI uri) {
		if (uri.getHost() == null || uri.getScheme() == null) {
			return null;
		}
		final IProxyData[] proxyData = proxyService.select(uri);
		if (proxyData == null) {
			return null;
		}
		for (IProxyData pd : proxyData) {
			if (pd != null && pd.getHost() != null) {
				return pd;
			}
		}
		return null;
	}
}
