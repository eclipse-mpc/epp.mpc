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
package org.eclipse.epp.mpc.rest.client.internal.resteasy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.client.WebTarget;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.mpc.rest.client.IRestClient;
import org.eclipse.epp.mpc.rest.client.IRestClientFactory;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component
public class DefaultMarketplaceRestClientFactoryImpl implements IRestClientFactory {

	private static final class ProgressEntityWrapper extends HttpEntityWrapper {
		private final IProgressMonitor monitor;

		private final long contentLength;

		private ProgressEntityWrapper(HttpEntity wrappedEntity, IProgressMonitor monitor, long contentLength) {
			super(wrappedEntity);
			this.monitor = monitor;
			this.contentLength = contentLength;
		}

		@Override
		public InputStream getContent() throws IOException {
			SubMonitor contentStreamMonitor = SubMonitor.convert(monitor, ""/*TODO*/, (int) contentLength);
			InputStream content = this.wrappedEntity.getContent();
			return new ProgressInputStream(content, contentLength, contentStreamMonitor);
		}
	}

	private static final class ProgressInputStream extends CountingInputStream {
		private final long contentLength;

		private final SubMonitor contentStreamMonitor;

		private long mark = 0;

		private ProgressInputStream(InputStream in, long contentLength, SubMonitor contentStreamMonitor) {
			super(in);
			this.contentLength = contentLength;
			this.contentStreamMonitor = contentStreamMonitor;
		}

		@Override
		protected synchronized void afterRead(int r) {
			super.afterRead(r);
			if (r > 0) {
				contentStreamMonitor.worked(r);
			}
		}

		@Override
		public synchronized void mark(final int readlimit) {
			super.mark(readlimit);
			this.mark = getByteCount();
		}

		@Override
		public synchronized void reset() throws IOException {
			super.reset();
			contentStreamMonitor.setWorkRemaining((int) (contentLength - this.mark));
		}

		@Override
		public int read(final byte[] bts) throws IOException {
			int available = available();
			if (available > 0 && available < bts.length) {
				return super.read(bts, 0, available);
			}
			return super.read(bts);
		}

		@Override
		public int read(final byte[] bts, final int off, final int len) throws IOException {
			int readLen = len;
			int available = available();
			if (available > 0 && available < len) {
				readLen = available;
			}
			return super.read(bts, off, readLen);
		}

		@Override
		public long skip(final long ln) throws IOException {
			long skipped = super.skip(ln);
			if (skipped > 0) {
				contentStreamMonitor.worked((int) skipped);
			}
			return skipped;
		}
	}

	private final HttpContextInjector contextInjector = new HttpContextInjector();

	private ResteasyFactoryService resteasyService;

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setResteasyService(ResteasyFactoryService resteasyService) {
		this.resteasyService = resteasyService;
	}

	public void unsetResteasyService(ResteasyFactoryService resteasyService) {
		if (this.resteasyService == resteasyService) {
			this.resteasyService = null;
		}
	}

	@Override
	public <E> IRestClient<E> createRestClient(URI baseUri, Class<E> endpointClass) {
		//FIXME use shared apache and resteasy client for all requests
		HttpResponseInterceptor itcp = createProgressInterceptor();
		HttpClient httpClient = HttpClientBuilder.create().addInterceptorFirst(itcp).build();

		ResteasyClient client = resteasyService.newClientBuilder()
				.httpEngine(new ApacheHttpClient43Engine(httpClient, contextInjector))
				.build();

		WebTarget target = client.target(baseUri)
				.property(ApacheHttpClientRestClientImpl.CONTEXT_PROVIDER_KEY, contextInjector);

		return new ApacheHttpClientRestClientImpl<>(endpointClass, target, null);
	}

	private HttpResponseInterceptor createProgressInterceptor() {
		HttpResponseInterceptor itcp = (HttpResponse response, HttpContext context) -> {
			HttpClientContext clientContext = HttpClientContext.adapt(context);
			IProgressMonitor monitor = clientContext.getAttribute(ApacheHttpClientRestClientImpl.CONTEXT_MONITOR_KEY,
					IProgressMonitor.class);
			HttpEntity entity = response.getEntity();
			if (monitor != null && entity != null && entity.getContentLength() != 0) {
				long contentLength = entity.getContentLength();
				response.setEntity(new ProgressEntityWrapper(entity, monitor, contentLength));
			}
		};
		return itcp;
	}

//	@Activate
//	protected void activate(HttpClientFactory clientFactory) {
//
//	}
}
