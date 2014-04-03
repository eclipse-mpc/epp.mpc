/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.DefaultCatalogService;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.osgi.util.NLS;
import org.junit.Test;

/**
 * @author Carsten Reckord
 */
public class MarketplaceClientUiTest {

	private static final String OFFLINE_HINT_TEXT = "Please check your Internet connection and retry";
	private static final String UNREACHABLE_MARKETPLACE_URL = "http://marketplace.eclipse.invalid";

	@Test
	public void testStatusHandling() {
		String errorMessage = "Something went seriously wrong";
		String contextMessage = "Cannot open marketplace";

		//Wrapped, unspecified exception - should be unwrapped and reported as-is
		IStatus status = MarketplaceClientUi.computeStatus(new InvocationTargetException(new RuntimeException(
				errorMessage)), contextMessage);
		String expectedMessage = NLS.bind("{0}: {1}", contextMessage, errorMessage);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertEquals(expectedMessage, status.getMessage());

		//UnknownHostException - should be treated as a hint for broken internet connection
		status = MarketplaceClientUi.computeStatus(wrapInCoreException(new InvocationTargetException(
				wrapInCoreException(new UnknownHostException("marketplace.eclipse.org")))), contextMessage);
		expectedMessage = NLS.bind("{0}: {1}", contextMessage, OFFLINE_HINT_TEXT);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertTrue(status.getMessage().startsWith(expectedMessage));

		//same with NoRouteToHostException and ConnectException
		status = MarketplaceClientUi.computeStatus(new NoRouteToHostException("marketplace.eclipse.org"),
				contextMessage);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertTrue(status.getMessage().startsWith(expectedMessage));
		status = MarketplaceClientUi.computeStatus(new ConnectException("marketplace.eclipse.org"), contextMessage);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertTrue(status.getMessage().startsWith(expectedMessage));

		//Wrapped HTTP 503 in ECF transport - unwrap and report as-is
		errorMessage = "Service temporarily unavailable";
		status = MarketplaceClientUi.computeStatus(wrapInCoreException(new InvocationTargetException(new CoreException(
				new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, 503, errorMessage,
						wrapInCoreException(new RuntimeException("Root cause")))))), contextMessage);
		expectedMessage = NLS.bind("{0}: {1}", contextMessage, errorMessage);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertTrue(status.getMessage().startsWith(expectedMessage));
	}

	private Exception wrapInCoreException(Throwable rootCause) {
		return new CoreException(new Status(IStatus.ERROR, "org.eclipse.epp.mpc.tests", "Nested Core Exception",
				rootCause));
	}

	@Test
	public void testOfflineCatalogServiceErrors() throws Exception {
		// since NoRouteToHost and ConnectExceptions are hard to fake and happen at pretty much the same place,
		// we only simulate the UnknownHostException case here (by actually using an invalid host address)
		final DefaultCatalogService catalogService = (DefaultCatalogService) ServiceLocator.getInstance()
				.getCatalogService();
		catalogService.setBaseUrl(new URL(UNREACHABLE_MARKETPLACE_URL));
		try {
			ServiceLocator.setInstance(new ServiceLocator() {
				@Override
				public ICatalogService getCatalogService() {
					return catalogService;
				}
			});
			IStatus status = new MarketplaceWizardCommand().installRemoteCatalogs();
			assertEquals(IStatus.ERROR, status.getSeverity());
			assertTrue(status.getMessage().contains(": " + OFFLINE_HINT_TEXT));
		} finally {
			ServiceLocator.setInstance(new ServiceLocator());
		}
	}

	@Test
	public void testOfflineCatalogErrors() throws Exception {
		final MarketplaceCatalog catalog = new MarketplaceCatalog();
		catalog.getDiscoveryStrategies().add(
				new MarketplaceDiscoveryStrategy(new CatalogDescriptor(new URL(UNREACHABLE_MARKETPLACE_URL),
						"Unreachable Marketplace")));
		IStatus status = catalog.performDiscovery(new NullProgressMonitor());
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertTrue(status.getMessage().startsWith(OFFLINE_HINT_TEXT));
	}
}
