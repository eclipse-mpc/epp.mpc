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

import java.lang.reflect.Field;

import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author David Green
 * @author Benjamin Muskalla
 */
@SuppressWarnings("restriction")
class P2TransportFactory extends AbstractP2TransportFactory {

	private static final String SERVICE_NAME_FIELD = "SERVICE_NAME"; //$NON-NLS-1$

	private static final String TRANSPORT_CLASS = "org.eclipse.equinox.internal.p2.repository.Transport"; //$NON-NLS-1$

	@Override
	public Transport getTransportService() throws Exception {
		BundleContext bundleContext = Platform.getBundle(MarketplaceClientCore.BUNDLE_ID).getBundleContext();
		ServiceReference<?> serviceReference = bundleContext.getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (serviceReference != null) {
			IProvisioningAgent agent = (IProvisioningAgent) bundleContext.getService(serviceReference);
			if (agent != null) {
				return (Transport) agent.getService(getTransportServiceName());
			}
		}
		return null;
	}

	private String getTransportServiceName() throws Exception {
		Bundle repoBundle = Platform.getBundle(P2_REPOSITORY_BUNDLE);
		Class<?> clazz = repoBundle.loadClass(TRANSPORT_CLASS);
		Field nameField;
		try {
			nameField = clazz.getDeclaredField(SERVICE_NAME_FIELD);
		} catch (NoSuchFieldException e) {
			return null;
		}
		return (String) nameField.get(null);
	}

}
