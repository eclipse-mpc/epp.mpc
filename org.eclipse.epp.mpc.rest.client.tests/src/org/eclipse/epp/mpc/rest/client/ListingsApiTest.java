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
package org.eclipse.epp.mpc.rest.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.eclipse.epp.mpc.rest.api.ListingsApi;
import org.eclipse.epp.mpc.rest.model.Listing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListingsApiTest extends AbstractRestClientTest {

	private IRestClient<ListingsApi> client;

	@BeforeEach
	void initClient() {
		client = restClientFactory.createRestClient(URI.create("http://localhost:8090"), ListingsApi.class);
	}

	@Test
	void getListing() {
		Listing listing = client.call().getListing("846fc9c0-7596-4efc-8874-d9543363bba3", null, null, null); //$NON-NLS-1$
		assertNotNull(listing);
		assertThat(listing.getBody(), not(isEmptyOrNullString()));
	}
}
