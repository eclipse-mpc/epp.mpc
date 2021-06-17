/*******************************************************************************
 * Copyright (c) 2014, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

/**
 * Convenience class to access marketplace-related OSGi services.
 *
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public abstract class ServiceHelper {

	private static ServiceHelper getInstance() {
		return null;//WIP ServiceHelperImpl.getImplInstance();
	}

	protected abstract IMarketplaceServiceLocator doGetMarketplaceServiceLocator();

	protected abstract ITransportFactory doGetTransportFactory();

	protected abstract IMarketplaceUnmarshaller doGetMarketplaceUnmarshaller();

	public static IMarketplaceServiceLocator getMarketplaceServiceLocator() {
		ServiceHelper instance = getInstance();
		return instance == null ? null : instance.doGetMarketplaceServiceLocator();
	}

	public static ITransportFactory getTransportFactory() {
		ServiceHelper instance = getInstance();
		return instance == null ? null : instance.doGetTransportFactory();
	}

	public static IMarketplaceUnmarshaller getMarketplaceUnmarshaller() {
		ServiceHelper instance = getInstance();
		return instance == null ? null : instance.doGetMarketplaceUnmarshaller();
	}

}