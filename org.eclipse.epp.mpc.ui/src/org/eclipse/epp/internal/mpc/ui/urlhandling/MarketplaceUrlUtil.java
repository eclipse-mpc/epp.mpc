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
package org.eclipse.epp.internal.mpc.ui.urlhandling;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;

public class MarketplaceUrlUtil {

	static final String MPC_STATE = "mpc_state"; //$NON-NLS-1$

	static final String MPC_INSTALL = "mpc_install"; //$NON-NLS-1$

	private static final String PARAM_SPLIT_REGEX = "&"; //$NON-NLS-1$

	private static final String EQUALS_REGEX = "="; //$NON-NLS-1$

	public static Map<String, String> parseQuery(String url) {
		return parseQuery(parseUri(url));
	}

	public static Map<String, String> parseQuery(URI uri) {
		String query = uri == null ? null : uri.getQuery();
		if (query == null) {
			return Collections.emptyMap();
		}
		Map<String, String> values = new LinkedHashMap<>();
		String[] params = query.split(PARAM_SPLIT_REGEX);
		for (String param : params) {
			String[] keyValue = param.split(EQUALS_REGEX);
			if (keyValue.length == 2) {
				String key = keyValue[0];
				String value = keyValue[1];
				values.put(key, value);
			}
		}
		return values;
	}

	public static URI parseUri(String url) {
		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public static String getMpcState(Map<String, String> query) {
		return query.get(MarketplaceUrlUtil.MPC_STATE);
	}

	public static String getInstallId(Map<String, String> query) {
		return query.get(MarketplaceUrlUtil.MPC_INSTALL);
	}

	public static CatalogDescriptor findCatalogDescriptor(String url, boolean allowUnknown) {
		CatalogDescriptor descriptor = CatalogRegistry.getInstance().findCatalogDescriptor(url);
		if (descriptor == null && allowUnknown) {
			try {
				descriptor = new CatalogDescriptor(URLUtil.toURL(url), MarketplaceUrlHandler.DESCRIPTOR_HINT);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return descriptor;
	}

}
