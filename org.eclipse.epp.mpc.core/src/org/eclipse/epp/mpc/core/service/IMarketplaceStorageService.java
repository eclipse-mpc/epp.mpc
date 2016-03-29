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
package org.eclipse.epp.mpc.core.service;

import java.net.URI;
import java.util.concurrent.Callable;

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.StorageFactory;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @author Carsten Reckord
 */
public interface IMarketplaceStorageService {

	String APPLICATION_TOKEN_PROPERTY = "applicationToken"; //$NON-NLS-1$

	String STORAGE_SERVICE_NAME_PROPERTY = "serviceName"; //$NON-NLS-1$

	String STORAGE_SERVICE_URL_PROPERTY = "serviceUrl"; //$NON-NLS-1$

	public static interface LoginListener {
		void loginChanged(String oldUser, String newUser);
	}

	URI getServiceUri();

	StorageFactory getStorageFactory();

	IStorage getStorage();

	IBlob getBlob(String key);

	String getRegisteredUser();

	void addLoginListener(LoginListener listener);

	<T> T runWithLogin(Callable<T> c) throws Exception;

	void removeLoginListener(LoginListener listener);

}