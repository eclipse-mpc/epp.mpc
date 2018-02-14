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
import java.net.URI;
import java.net.URISyntaxException;

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
//	private static ProxyAuthenticator authenticator;

	@SuppressWarnings("rawtypes")
	private static ServiceTracker proxyServiceTracker;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static synchronized void acquireProxyService() {
		if (proxyServiceTracker == null) {
			proxyServiceTracker = new ServiceTracker(MarketplaceClientCorePlugin.getBundle().getBundleContext(),
					IProxyService.class.getName(), null);
			proxyServiceTracker.open();
		}
//		installAuthenticator();
	}

	public static void installAuthenticator() {
//		synchronized (Authenticator.class) {
//			Authenticator defaultAuthenticator = getDefaultAuthenticator();
//			if (authenticator == null || authenticator != defaultAuthenticator) {
//				if (defaultAuthenticator instanceof ProxyAuthenticator) {
//					authenticator = (ProxyAuthenticator) defaultAuthenticator;
//				} else {
//					authenticator = new ProxyAuthenticator(defaultAuthenticator);
//				}
//				authenticator.install();
//			}
//		}
	}

	public static synchronized void releaseProxyService() {
//		uninstallAuthenticator();
		if (proxyServiceTracker != null) {
			proxyServiceTracker.close();
		}
	}

	public static void uninstallAuthenticator() {
//		synchronized (Authenticator.class) {
//			if (authenticator != null) {
//				authenticator.uninstall();
//				authenticator = null;
//			}
//		}
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

	protected static IProxyService getProxyService() {
		return proxyServiceTracker == null ? null : (IProxyService) proxyServiceTracker.getService();
	}

	static Authenticator getDefaultAuthenticator() {
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
}
