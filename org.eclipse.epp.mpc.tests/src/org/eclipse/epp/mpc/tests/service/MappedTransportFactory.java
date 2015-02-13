/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.service;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class MappedTransportFactory implements ITransportFactory {

	private final Map<URI, URI> mapping = new HashMap<URI, URI>();

	private ServiceRegistration<ITransportFactory> registration;

	private ITransportFactory delegate;

	public ITransport getTransport() {
		return new ITransport() {

			private final ITransport delegateTransport = delegate.getTransport();

			public InputStream stream(URI location, IProgressMonitor monitor) throws FileNotFoundException,
			ServiceUnavailableException, CoreException {
				URI mapped = mapping.get(location);
				if (mapped == null) {
					if (location.getQuery() != null || location.getFragment() != null) {
						try {
							URI stripped = new URI(location.getScheme(), location.getHost(), location.getPath(), null);
							mapped = mapping.get(stripped);
						} catch (URISyntaxException e) {
						}
					}
				}
				if (mapped == null) {
					mapped = location;
				}
				return delegateTransport.stream(mapped, monitor);
			}
		};
	}

	public MappedTransportFactory map(URI url, URI mapped) {
		mapping.put(url, mapped);
		return this;
	}

	public MappedTransportFactory map(String url, String mapped) {
		try {
			mapping.put(new URI(url), new URI(mapped));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		return this;
	}

	public Map<URI, URI> getMapping() {
		return mapping;
	}

	public ITransportFactory getDelegate() {
		return delegate;
	}

	public void setDelegate(ITransportFactory delegate) {
		this.delegate = delegate;
	}

	public void register() throws IllegalStateException {
		ITransportFactory transportFactory = ServiceHelper.getTransportFactory();
		if (transportFactory != this) {
			unregister();
			ITransportFactory delegate = transportFactory;
			if (delegate instanceof MappedTransportFactory) {
				delegate = ((MappedTransportFactory) delegate).getDelegate();
			}
			this.delegate = delegate;
			Dictionary<String, Object> maxServiceRanking = new Hashtable<String, Object>(Collections
					.singletonMap(Constants.SERVICE_RANKING, Integer.MAX_VALUE));
			registration = MarketplaceClientCorePlugin.getBundle().getBundleContext().registerService(
					ITransportFactory.class, this,
					maxServiceRanking);
			transportFactory = ServiceHelper.getTransportFactory();
			if (transportFactory != this) {
				throw new IllegalStateException("Factory not selected after registration");
			}
		}
	}

	public void unregister() {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}

	public static synchronized MappedTransportFactory get() {
		ITransportFactory transportFactory = ServiceHelper.getTransportFactory();
		if (transportFactory instanceof MappedTransportFactory) {
			return (MappedTransportFactory) transportFactory;
		}
		MappedTransportFactory factory = new MappedTransportFactory();
		factory.register();
		return factory;
	}
}
