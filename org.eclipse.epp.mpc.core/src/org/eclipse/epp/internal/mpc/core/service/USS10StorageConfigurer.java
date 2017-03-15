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
package org.eclipse.epp.internal.mpc.core.service;

import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.osgi.framework.Version;

class USS10StorageConfigurer extends StorageConfigurer {
	static class Factory extends StorageConfigurer.Factory {

		@Override
		boolean isApplicable() {
			Version ussVersion = getUSSVersion();
			return ussVersion.getMajor() == 1 && ussVersion.getMinor() == 0;
		}

		@Override
		StorageConfigurer doCreate() {
			return new USS10StorageConfigurer();
		}

	}

	@Override
	public void configure(IStorage storage) {
		storage.setCredentialsProvider(ICredentialsProvider.CANCEL);
	}

	@Override
	public Object setInteractive(IStorage storage, boolean interactive) {
		ICredentialsProvider credentialsProvider = storage.getCredentialsProvider();
		//Use default credentials provider for interactive mode
		storage.setCredentialsProvider(interactive ? null : ICredentialsProvider.CANCEL);
		return credentialsProvider;
	}

	@Override
	void restoreInteractive(IStorage storage, Object restoreValue) {
		storage.setCredentialsProvider((ICredentialsProvider) restoreValue);
	}

}
