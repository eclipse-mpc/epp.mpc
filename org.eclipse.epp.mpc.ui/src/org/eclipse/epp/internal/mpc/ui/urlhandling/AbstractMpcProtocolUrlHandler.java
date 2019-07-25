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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;

// eg. eclipse-mpc:/install/<solution-name>
// or  eclipse-mpc://marketplace.eclipse.org/install/<solution-id>
public abstract class AbstractMpcProtocolUrlHandler implements UrlHandlerStrategy {

	protected static final String PARSED_URI = "parsed_uri"; //$NON-NLS-1$

	protected static final String MARKETPLACE_URL = "marketplace_url"; //$NON-NLS-1$

	protected static final String MPC_CATALOG = "mpc_catalog"; //$NON-NLS-1$

	protected static final String ACTION = "action"; //$NON-NLS-1$

	protected static final String PATH_PARAMETERS = "path_parameters"; //$NON-NLS-1$

	public boolean handles(String url) {
		return url != null && url.startsWith(MarketplaceUriSchemeHandler.ECLIPSE_MPC_SCHEME + ":"); //$NON-NLS-1$
	}

	protected Map<String, Object> doParse(String url) {
		URI parsedUri = MarketplaceUrlUtil.parseUri(url);
		if (parsedUri == null || !MarketplaceUriSchemeHandler.ECLIPSE_MPC_SCHEME.equals(parsedUri.getScheme())) {
			return null;
		}

		Map<String, Object> result = new HashMap<>();
		result.putAll(MarketplaceUrlUtil.parseQuery(parsedUri));
		result.put(PARSED_URI, parsedUri);

		String authority = parsedUri.getAuthority();
		String marketplaceLookupUrl;
		if (authority == null) {
			marketplaceLookupUrl = DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION;
		} else {
			marketplaceLookupUrl = "http://" + authority; //$NON-NLS-1$
		}
		CatalogDescriptor catalogDescriptor = MarketplaceUrlUtil.findCatalogDescriptor(marketplaceLookupUrl, true);
		result.put(MPC_CATALOG, catalogDescriptor);
		result.put(MARKETPLACE_URL, catalogDescriptor.getUrl());

		String action = null;
		IPath path = new Path(parsedUri.getPath());
		if (path.segmentCount() > 0) {
			action = path.segment(0).toLowerCase();
		}
		result.put(ACTION, action);

		IPath pathParameters = null;
		if (path.segmentCount() > 1) {
			pathParameters = path.removeFirstSegments(1);
		}
		result.put(PATH_PARAMETERS, pathParameters);

		return result;
	}
}