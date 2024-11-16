/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.epp.mpc.core.service.ServiceUnavailableException;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class FallbackTransportFactory implements ITransportFactory {

	private static final class FallbackTransport implements ITransport {
		private final ITransport primaryTransport;

		private final ITransport fallbackTransport;

		private boolean primaryDisabled = false;

		FallbackTransport(ITransport primaryTransport, ITransport fallbackTransport) {
			super();
			try {
				if (PKIContext.INSTANCE.isEnabled) {
					primaryDisabled = true;
				}
			} catch (Exception e) {

				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.primaryTransport = primaryTransport;
			this.fallbackTransport = fallbackTransport;
		}

		private final Set<String> reportedProblems = new HashSet<>();

		private int connectionAttempts;

		private int connectionFailures;

		@Override
		public InputStream stream(URI location, IProgressMonitor monitor)
				throws FileNotFoundException, ServiceUnavailableException, CoreException {
			connectionAttempts++;
			if (connectionAttempts > 10 && connectionFailures / (double) connectionAttempts > 0.75) {
				MarketplaceClientCore.getLog()
				.log(new Status(IStatus.INFO, MarketplaceClientCore.BUNDLE_ID,
						NLS.bind(Messages.FallbackTransportFactory_disablingTransport, primaryTransport)));
				primaryDisabled = true;
			}
			if (primaryTransport == null || primaryDisabled) {
				return fallbackTransport.stream(location, monitor);
			}
			InputStream stream;
			try {
				stream = primaryTransport.stream(location, monitor);
				if (stream == null) {
					throw new NullPointerException();
				}
			} catch (FileNotFoundException ex) {
				InputStream fallbackStream = primaryFailed(location, monitor, ex);
				if (fallbackStream == null) {
					throw ex;
				}
				return fallbackStream;
			} catch (ServiceUnavailableException ex) {
				InputStream fallbackStream = primaryFailed(location, monitor, ex);
				if (fallbackStream == null) {
					throw ex;
				}
				return fallbackStream;
			} catch (CoreException ex) {
				InputStream fallbackStream = primaryFailed(location, monitor, ex);
				if (fallbackStream == null) {
					throw ex;
				}
				return fallbackStream;
			} catch (RuntimeException ex) {
				InputStream fallbackStream = primaryFailed(location, monitor, ex);
				if (fallbackStream == null) {
					throw ex;
				}
				return fallbackStream;
			}
			try {
				BufferedInputStream buffered = new BufferedInputStream(stream);
				tryBuffer(buffered);
				return buffered;
			} catch (IOException ex) {
				InputStream fallbackStream = primaryFailed(location, monitor, ex);
				if (fallbackStream == null) {
					throw new CoreException(MarketplaceClientCore.computeStatus(ex, null));
				}
				return fallbackStream;
			}
		}

		private static void tryBuffer(BufferedInputStream buffered) throws IOException {
			buffered.mark(128);
			try {
				buffered.read(new byte[128]);
			} finally {
				buffered.reset();
			}
		}

		private InputStream primaryFailed(URI location, IProgressMonitor monitor, Exception ex)
				throws FileNotFoundException, ServiceUnavailableException, CoreException {
			connectionFailures++;
			if (fallbackTransport != null) {
				boolean fallbackSucceeded = false;
				try (InputStream fallbackStream = fallbackTransport.stream(location, monitor)) {
					BufferedInputStream buffered = new BufferedInputStream(fallbackStream);
					tryBuffer(buffered);
					fallbackSucceeded = true;
					String problemKey = ex.getClass().getName() + ": " + ex.getMessage() + "\n\t" //$NON-NLS-1$//$NON-NLS-2$
							+ ex.getStackTrace()[0];
					if (reportedProblems.add(problemKey)) {
						MarketplaceClientCore.getLog()
						.log(MarketplaceClientCore.computeStatus(ex,
								NLS.bind(Messages.FallbackTransportFactory_fallbackStream, primaryTransport,
										fallbackTransport)));
					}

					return buffered;
				} catch (Exception fallbackEx) {
					ex.addSuppressed(fallbackEx);
				} finally {
					if (!fallbackSucceeded) {
						//fallback didn't work either - probably something unrelated to transport going on, so don't count this as a transport failure
						connectionFailures--;
					}
				}
			}
			return null;
		}

		public ITransport getPrimaryTransport() {
			return primaryTransport;
		}

		public ITransport getFallbackTransport() {
			return fallbackTransport;
		}
	}

	private ITransportFactory primaryFactory;

	private ITransportFactory secondaryFactory;

	private FallbackTransport transport;

	public FallbackTransportFactory() {
		super();
		// ignore
	}

	@Override
	public synchronized ITransport getTransport() {
		ITransportFactory delegateFactory = getFallbackFactory();
		ITransport primaryTransport = primaryFactory.getTransport();
		if (delegateFactory == null) {
			return primaryTransport;
		}

		ITransport secondaryTransport = delegateFactory.getTransport();

		if (transport == null || transport.getPrimaryTransport() != primaryTransport
				|| transport.getFallbackTransport() != secondaryTransport) {
			transport = new FallbackTransport(primaryTransport, secondaryTransport);
		}
		return transport;
	}

	public ITransportFactory getFallbackFactory() {
		ITransportFactory delegateFactory = this.secondaryFactory;
		if (delegateFactory == null) {
			BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
			try {
				String disabledTransportsFilter = TransportFactory.computeDisabledTransportsFilter();
				Collection<ServiceReference<ITransportFactory>> serviceReferences = bundleContext.getServiceReferences(
						ITransportFactory.class, "".equals(disabledTransportsFilter) ? null : disabledTransportsFilter); //$NON-NLS-1$
				if (!serviceReferences.isEmpty()) {
					for (ServiceReference<ITransportFactory> serviceReference : serviceReferences) {
						ITransportFactory service = bundleContext.getService(serviceReference);
						if (service != this && service != primaryFactory
								&& !"org.eclipse.epp.mpc.tests.service.MappedTransportFactory" //$NON-NLS-1$
								.equals(service.getClass().getName())) {
							delegateFactory = service;
							break;
						} else {
							bundleContext.ungetService(serviceReference);
						}
					}
				}
			} catch (InvalidSyntaxException e) {
				//impossible
			}
		}
		return delegateFactory;
	}

	public ITransportFactory getPrimaryFactory() {
		return primaryFactory;
	}

	public void setPrimaryFactory(ITransportFactory primaryFactory) {
		this.primaryFactory = primaryFactory;
	}

	public void bindPrimaryFactory(ITransportFactory factory) {
		setPrimaryFactory(factory);
	}

	public void unbindPrimaryFactory(ITransportFactory factory) {
		if (primaryFactory == factory) {
			setPrimaryFactory(null);
		}
	}

	public ITransportFactory getSecondaryFactory() {
		return secondaryFactory;
	}

	public void setSecondaryFactory(ITransportFactory secondaryFactory) {
		this.secondaryFactory = secondaryFactory;
	}
}
