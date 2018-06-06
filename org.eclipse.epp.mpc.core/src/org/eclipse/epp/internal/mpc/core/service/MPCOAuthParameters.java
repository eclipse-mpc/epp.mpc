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
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import org.eclipse.userstorage.oauth.OAuthParameters;

final class MPCOAuthParameters extends OAuthParameters {

	private static final String SERVICE_NAME = "mpc"; //$NON-NLS-1$

	private static final String CLIENT_ID = "1e8b68d6e5015c2bcf8e03d44dd97fc431f17e394860f08f1a05978c"; //$NON-NLS-1$

	private static final String CLIENT_SECRET = "26e9f81e5d02d4a019e042d012a861f34a446c35cb84070e80278d8e"; //$NON-NLS-1$

	private static final String CLIENT_KEY = "9d08c11f742f53a2cd6348d373fd1fa0b079694199f760b315a12a"; //$NON-NLS-1$

	// FIXME: these should be the uss_project_* alternatives
	//private static final String[] DEFAULT_MPC_SCOPES = { "profile", "uss_project_retrieve", "uss_project_update", "uss_project_delete" };
	private static final String[] DEFAULT_MPC_SCOPES = { "profile", "uss_retrieve", "uss_update" }; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

	@Override
	protected String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	protected String getDefaultClientId() {
		return CLIENT_ID;
	}

	@Override
	protected String getDefaultClientSecret() {
		return CLIENT_SECRET;
	}

	@Override
	protected String getDefaultClientKey() {
		return CLIENT_KEY;
	}

	@Override
	protected String[] getDefaultScopes() {
		return DEFAULT_MPC_SCOPES;
	}
}
