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

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.epp.internal.mpc.core.service.messages"; //$NON-NLS-1$


	public static String DefaultMarketplaceService_cannotCompleteRequest_reason;

	public static String DefaultMarketplaceService_categoryNotFound;

	public static String DefaultMarketplaceService_invalidLocation;

	public static String DefaultMarketplaceService_marketNotFound;

	public static String DefaultMarketplaceService_mustConfigureBaseUrl;

	public static String DefaultMarketplaceService_nodeNotFound;

	public static String DefaultMarketplaceService_parseError;

	public static String DefaultMarketplaceService_retrievingDataFrom;

	public static String DefaultMarketplaceService_unexpectedResponse;

	public static String DefaultMarketplaceService_unexpectedResponseContent;


	public static String DefaultMarketplaceService_UnsupportedSearchString;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
