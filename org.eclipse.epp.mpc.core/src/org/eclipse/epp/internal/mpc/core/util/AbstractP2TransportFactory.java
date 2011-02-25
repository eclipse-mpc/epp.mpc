/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;



import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.equinox.internal.p2.repository.Transport;

@SuppressWarnings("restriction")
public abstract class AbstractP2TransportFactory extends TransportFactory {

	private static final String STREAM_METHOD = "stream"; //$NON-NLS-1$

	protected static final String P2_REPOSITORY_BUNDLE = "org.eclipse.equinox.p2.repository"; //$NON-NLS-1$

	@Override
	protected InputStream invokeStream(URI location, IProgressMonitor monitor) throws Exception {
		Transport repositoryTransport = getTransportService();
		Method streamMethod = repositoryTransport.getClass()
		.getMethod(STREAM_METHOD, URI.class, IProgressMonitor.class);
		Object stream = streamMethod.invoke(repositoryTransport, location, monitor);
		return (InputStream) stream;
	}

	protected abstract Transport getTransportService() throws Exception;

	@Override
	protected boolean isAvailable() {
		try {
			return getTransportService() != null;
		} catch (Exception e) {
			MarketplaceClientCore.error(e);
			return false;
		}
	}

}
