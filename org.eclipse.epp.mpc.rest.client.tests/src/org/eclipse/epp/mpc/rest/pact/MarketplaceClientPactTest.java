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
package org.eclipse.epp.mpc.rest.pact;

//import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
//import static org.junit.Assert.*;
//
//import java.net.URI;
//
//import javax.ws.rs.client.ClientBuilder;
//import javax.ws.rs.client.WebTarget;
//
//import org.eclipse.epp.mpc.rest.api.ListingsApi;
//import org.eclipse.epp.mpc.rest.model.Listing;
//import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//
//import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
//import au.com.dius.pact.consumer.junit.PactProviderRule;
//import au.com.dius.pact.consumer.junit.PactVerification;
//import au.com.dius.pact.core.model.RequestResponsePact;
//import au.com.dius.pact.core.model.annotations.Pact;

//See pact.io
public class MarketplaceClientPactTest {

	private static final String LISTING_ID = "206";

//	@Rule
//	public PactProviderRule mockProvider = new PactProviderRule("marketplace-api", // Contract provider name
//			// "localhost", 8080, // Optional address, default localhost:<random port>
//			this);
//
//	private ListingsApi client;
//
//	@Before
//	public void initClient() {
//		WebTarget target = ClientBuilder.newBuilder()
//				.build().target(URI.create(mockProvider.getUrl()));
//		client = ProxyBuilder.builder(ListingsApi.class, target).build();
//	}
//
//	@Pact(/* provider = "marketplace-api", */ //Default, taken from first pact provider rule in class
//			consumer = "mpc")
//	public RequestResponsePact mylynFragment(PactDslWithProvider builder) {
//		return builder.given("a listing", "id", String.valueOf(LISTING_ID), "name", "Mylyn")
//				.uponReceiving("a request for the mylyn listing")
//				.method("GET").path("/listings/" + LISTING_ID)
//				.willRespondWith().status(200)
//				.body(newJsonBody(json -> {
//					//Postel's law ...
//					json.stringValue("id", LISTING_ID);
//					json.stringValue("title", "Mylyn");
//					json.stringType("teaser",
//							"Mylyn integrates defect and project management systems, build systems and other software development tools with Eclipse.");
//					json.array("organization", arr -> arr.stringType("Tasktop Inc."));
//					json.booleanType("foundation_member", true);
//					json.stringType("license", "EPL");
//				}).build()).toPact();
//	}
//
//	@Test
//	@PactVerification
//	//Or explicitly:
//	//@PactVerification("marketplace-api")
//	//@PactVerification(fragment = "mylynFragment")
//	//@PactVerification(value = "marketplace-api", fragment = "mylynFragment")
//	public void getListing() {
//		Listing listing = client.getListing(LISTING_ID, null, null, null);
//		assertNotNull(listing);
//		assertEquals(LISTING_ID, listing.getId());
//		assertEquals("Mylyn", listing.getTitle());
//	}
}
