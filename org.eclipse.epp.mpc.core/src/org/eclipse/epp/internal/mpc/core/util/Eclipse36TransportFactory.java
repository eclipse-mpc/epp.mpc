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

import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.osgi.framework.Bundle;

/**
 * @author David Green
 * @author Benjamin Muskalla
 */
@SuppressWarnings("restriction")
class Eclipse36TransportFactory extends AbstractP2TransportFactory {

	private static final String GET_INSTANCE_METHOD = "getInstance"; //$NON-NLS-1$

	private static final String REPOSITORY_TRANSPORT_CLASS = "org.eclipse.equinox.internal.p2.repository.RepositoryTransport"; //$NON-NLS-1$

	@Override
	protected Transport getTransportService() throws Exception {
		try {
			Bundle bundle = Platform.getBundle(P2_REPOSITORY_BUNDLE);
			Class<?> clazz = bundle.loadClass(REPOSITORY_TRANSPORT_CLASS);
			Method method = clazz.getDeclaredMethod(GET_INSTANCE_METHOD);
			Object transportInstance = method.invoke(null);
			return (Transport) transportInstance;
		} catch (Exception e) {
			return null;
		}
	}

}
