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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.mpc.rest.api.ListingsApi;
import org.eclipse.epp.mpc.rest.model.Listing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListingsApiTest extends AbstractRestClientTest {

	private IRestClient<ListingsApi> client;

	private static String listingId;

	@BeforeEach
	void initClient() {
		client = restClientFactory.createRestClient(URI.create("http://localhost:8090"), ListingsApi.class);
	}

	@Test
	void getListings() {
		listingId = null;
		List<Listing> listings = client.call()
				.getListings(null, null, null, null, null, null, null, null, null, null, null, null, null);
		assertNotNull(listings);
		assertThat(listings, not(empty()));
		listingId = listings.get(0).getId();
	}

	@Test
	void getListing() {
		if (listingId == null) {
			getListings();
		}
		Listing listing = client.call().getListing(listingId, null, null, null);
		assertNotNull(listing);
		assertThat(listing.getBody(), not(isEmptyOrNullString()));
	}

	@Test
	void getListingsWithProgress() {
		IProgressMonitor monitor = Mockito.mock(IProgressMonitor.class);
		List<Listing> listings = client.call(monitor)
				.getListings(null, null, null, null, null, null, null, null, null, null, null, null, null);
		assertNotNull(listings);
		assertThat(listings, not(empty()));
		verify(monitor).beginTask(any(), anyInt());
		verify(monitor, atLeastOnce()).worked(anyInt());
		verify(monitor).done();
	}

	@Test
	void callingWithProgressTwiceFails() {
		IProgressMonitor monitor = Mockito.mock(IProgressMonitor.class);
		ListingsApi monitoredCall = client.call(monitor);
		monitoredCall.getListings(null, null, null, null, null, null, null, null, null, null, null, null, null);
		assertThrows(IllegalStateException.class, () -> monitoredCall.getListings(null, null, null, null, null, null,
				null, null, null, null, null, null, null));
	}
}
