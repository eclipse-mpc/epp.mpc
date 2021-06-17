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
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.mpc.rest.api.ListingsApi;
import org.junit.jupiter.api.Test;

class RestClientFactoryTest extends AbstractRestClientTest {

	@Test
	void createRestClient_returns_client_for_base_url() {
		IRestClient<ListingsApi> restClient = restClientFactory.createRestClient(URI.create("http://localhost:8090"),
				ListingsApi.class);
		assertEquals("http://localhost:8090", restClient.getBaseUri().toString());
	}

	@Test
	void createRestClient_returns_client_for_api() {
		IRestClient<ListingsApi> restClient = restClientFactory.createRestClient(URI.create("http://localhost:8090"),
				ListingsApi.class);
		ListingsApi endpoint = restClient.call();
		assertThat(endpoint, instanceOf(ListingsApi.class));
	}

	@Test
	void createRestClient_returns_client_with_progress_for_api() {
		IRestClient<ListingsApi> restClient = restClientFactory.createRestClient(URI.create("http://localhost:8090"),
				ListingsApi.class);
		ListingsApi endpoint = restClient.call(new NullProgressMonitor());
		assertThat(endpoint, instanceOf(ListingsApi.class));
	}
}
