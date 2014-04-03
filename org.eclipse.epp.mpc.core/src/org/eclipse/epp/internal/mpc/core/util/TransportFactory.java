/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCorePlugin;
import org.eclipse.epp.internal.mpc.core.service.ServiceUnavailableException;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Factory to retrieve Transport instances of p2. Will delegate to version-dependent implementations.
 *
 * @author David Green
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public abstract class TransportFactory implements ITransportFactory {

	private static final String[] factoryClasses = new String[] { //
		"org.eclipse.epp.internal.mpc.core.util.P2TransportFactory", // //$NON-NLS-1$
		"org.eclipse.epp.internal.mpc.core.util.Eclipse36TransportFactory", // //$NON-NLS-1$
	"org.eclipse.epp.internal.mpc.core.util.JavaPlatformTransportFactory" }; //$NON-NLS-1$

	private static TransportFactory instance;

	/**
	 * @deprecated use registered {@link ITransportFactory} OSGi service
	 * @see ServiceHelper#getTransportFactory()
	 */
	@Deprecated
	public static synchronized TransportFactory instance() {
		if (instance == null) {
			List<ITransportFactory> availableFactories = listAvailableFactories();
			if (availableFactories.isEmpty()) {
				throw new IllegalStateException();
			}
			instance = (TransportFactory) availableFactories.get(0);
		}
		return instance;
	}

	public static org.eclipse.epp.mpc.core.service.ITransport createTransport() {
		//search for registered factory service
		BundleContext context = MarketplaceClientCorePlugin.getBundle().getBundleContext();
		ServiceReference<ITransportFactory> serviceReference = context.getServiceReference(ITransportFactory.class);
		if (serviceReference != null) {
			ITransportFactory transportService = context.getService(serviceReference);
			if (transportService != null) {
				try {
					return transportService.getTransport();
				} finally {
					context.ungetService(serviceReference);
				}
			}
		}
		//fall back to legacy transports
		return instance().getTransport();
	}

	public static List<ITransportFactory> listAvailableFactories() {
		List<ITransportFactory> factories = new ArrayList<ITransportFactory>();
		for (String factoryClass : factoryClasses) {
			TransportFactory factory;
			try {
				factory = (TransportFactory) Class.forName(factoryClass, true, TransportFactory.class.getClassLoader())
						.newInstance();
			} catch (Throwable t) {
				// ignore
				continue;
			}
			if (factory.isAvailable()) {
				factories.add(factory);
			}
		}
		return factories;
	}

	public ITransport getTransport() {
		return new ITransport() {

			public InputStream stream(URI location, IProgressMonitor monitor) throws FileNotFoundException,
			org.eclipse.epp.mpc.core.service.ServiceUnavailableException, CoreException {
				try {
					return invokeStream(location, monitor);
				} catch (Exception e) {
					handleStreamExceptions(e);
				}
				return null;
			}

		};
	}

	protected abstract boolean isAvailable();

	protected abstract InputStream invokeStream(URI location, IProgressMonitor monitor) throws Exception;

	protected void handleStreamExceptions(Exception e) throws ServiceUnavailableException, CoreException,
	FileNotFoundException {
		if (e instanceof InvocationTargetException) {
			InvocationTargetException targetException = (InvocationTargetException) e;
			Throwable cause = targetException.getCause();
			if (cause instanceof CoreException) {
				CoreException coreCause = (CoreException) cause;
				handleServiceUnavailable(coreCause);
				throw coreCause;
			} else if (cause instanceof FileNotFoundException) {
				throw (FileNotFoundException) cause;
			}

		} else {
			throw new CoreException(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, e.getMessage(), e));
		}
	}


	protected static void handleServiceUnavailable(CoreException e) throws ServiceUnavailableException {
		if (e.getStatus().getCode() == 1002) {
			Throwable cause = e.getCause();
			if (cause != null && cause.getMessage() != null && cause.getMessage().indexOf("503") != -1) { //$NON-NLS-1$
				throw new ServiceUnavailableException(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, 503,
						Messages.DefaultMarketplaceService_serviceUnavailable503, e));
			}
		}
	}
}