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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.eclipse.epp.mpc.rest.api.CatalogsApi;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Test;

/**
 * @author carsten.reckord
 */
//FIXME WIP At the moment, this is more a sandbox than an actual test class, useful to try out some stuff without launching an RCP...
public class AsyncClientTest {

	@Test
	public void testCreateSyncClient() {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target("http://example.com/base/uri");
		CatalogsApi proxy = ProxyBuilder.builder(CatalogsApi.class, target).build();
		System.out.println(proxy.getClass());
	}

	@Test
	public void testCreateAsyncClient() {
		ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
		//		Client client = builder.useAsyncHttpEngine()
		//				.register(org.jboss.resteasy.client.jaxrs.internal.CompletionStageRxInvokerProvider.class)
		//				.build();
		Client client = builder.register(
				org.jboss.resteasy.client.jaxrs.internal.CompletionStageRxInvokerProvider.class).build();
		WebTarget target = client.target("http://example.com/base/uri");
		CatalogsApi proxy = ProxyBuilder.builder(CatalogsApi.class, target).build();
		System.out.println(proxy.getClass());
	}
}
