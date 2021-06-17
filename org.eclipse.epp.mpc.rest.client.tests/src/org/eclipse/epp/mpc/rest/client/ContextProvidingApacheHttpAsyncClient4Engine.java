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

//import org.apache.http.concurrent.FutureCallback;
//import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
//import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
//import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
//import org.apache.http.protocol.HttpContext;
//import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpAsyncClient4Engine;
//import org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider;

//FIXME WIP At the moment, this is more a sandbox than an actual test class, useful to try out some stuff without launching an RCP...
public class ContextProvidingApacheHttpAsyncClient4Engine { //extends ApacheHttpAsyncClient4Engine {

//	public ContextProvidingApacheHttpAsyncClient4Engine(CloseableHttpAsyncClient client, boolean closeHttpClient,
//			HttpContextProvider contextProvider) {
//		super(wrap(client, contextProvider), closeHttpClient);
//	}
//
//	private static CloseableHttpAsyncClient wrap(CloseableHttpAsyncClient client, HttpContextProvider contextProvider) {
//		if (contextProvider == null) {
//			return client;
//		}
//		CloseableHttpAsyncClient wrapper = new CloseableHttpAsyncClient() {
//
//			@Override
//			public <T> Future<T> execute(final HttpAsyncRequestProducer requestProducer,
//					final HttpAsyncResponseConsumer<T> responseConsumer, final FutureCallback<T> callback) {
//				return execute(requestProducer, responseConsumer, contextProvider.getContext(), callback);
//			}
//
//			@Override
//			public <T> Future<T> execute(HttpAsyncRequestProducer requestProducer,
//					HttpAsyncResponseConsumer<T> responseConsumer, HttpContext context, FutureCallback<T> callback) {
//				return client.execute(requestProducer, responseConsumer, context, callback);
//			}
//
//			@Override
//			public void close() throws IOException {
//				client.close();
//			}
//
//			@Override
//			public boolean isRunning() {
//				return client.isRunning();
//			}
//
//			@Override
//			public void start() {
//				client.start();
//			}
//		};
//		return wrapper;
//	}
}
