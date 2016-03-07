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

public interface IMarketplaceStorageService {

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