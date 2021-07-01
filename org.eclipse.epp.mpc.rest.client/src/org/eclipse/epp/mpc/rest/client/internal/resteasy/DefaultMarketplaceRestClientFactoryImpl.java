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
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
			InputStream content = this.wrappedEntity.getContent();
			return new ProgressInputStream(content, contentLength, monitor);
		}
	}

	private static final class ProgressInputStream extends CountingInputStream {
		private final long contentLength;

		private final IProgressMonitor realMonitor;

		private final SubMonitor monitor;

		private boolean done;

		private long mark = 0;

		private ProgressInputStream(InputStream in, long contentLength, IProgressMonitor contentStreamMonitor) {
			super(in);
			this.contentLength = contentLength;
			this.realMonitor = contentStreamMonitor;
			this.monitor = SubMonitor.convert(contentStreamMonitor, ""/*TODO*/, (int) contentLength);
		}

		@Override
		protected synchronized void afterRead(int r) {
			super.afterRead(r);
			if (r > 0) {
				monitor.worked(r);
			} else if (r == -1) {
				monitorDone();
			}
		}

		private void monitorDone() {
			if (!done) {
				done = true;
				monitor.done();
				//SubMonitor does not delegate done() call
				realMonitor.done();
			}
		}

		@Override
		protected void handleIOException(IOException e) throws IOException {
			try {
				super.handleIOException(e);
			} finally {
				monitorDone();
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
			monitor.setWorkRemaining((int) (contentLength - this.mark));
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
				monitor.worked((int) skipped);
			}
			return skipped;
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			} finally {
				monitorDone();
			}
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
				.register(createJacksonProvider())
				.build();

		WebTarget target = client.target(baseUri)
				.property(ApacheHttpClientRestClientImpl.CONTEXT_PROVIDER_KEY, contextInjector);

		return new ApacheHttpClientRestClientImpl<>(endpointClass, target, null);
	}

	private ResteasyJackson2Provider createJacksonProvider() {
		ObjectMapper objectMapper = createObjectMapper();

		ResteasyJackson2Provider resteasyJacksonProvider = new ResteasyJackson2Provider() {
			//subclass to allow re-registration over the original
		};
		resteasyJacksonProvider.setMapper(objectMapper);
		return resteasyJacksonProvider;
	}

	private ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		objectMapper.enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS);
		objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
		objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.disable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
		objectMapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		objectMapper.disable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
		objectMapper.configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
		objectMapper.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);
		objectMapper.configure(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS.mappedFeature(), true);
		objectMapper.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature(), true);

		//ignore null values
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
		return objectMapper;
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
