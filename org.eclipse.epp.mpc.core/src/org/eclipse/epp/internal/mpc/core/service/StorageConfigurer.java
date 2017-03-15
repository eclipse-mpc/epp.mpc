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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.userstorage.IStorage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public abstract class StorageConfigurer {

	public static final String PROP_FACTORIES = StorageConfigurer.class.getName() + ".factories";
	private static final String[] DEFAULT_FACTORIES = { "org.eclipse.epp.internal.mpc.core.service.USS11ExtendedOAuthStorageConfigurer$Factory",
			"org.eclipse.epp.internal.mpc.core.service.USS11OAuthStorageConfigurer$Factory",
	"org.eclipse.epp.internal.mpc.core.service.USS10StorageConfigurer$Factory" };

	abstract static class Factory {
		abstract boolean isApplicable();

		abstract StorageConfigurer doCreate();

		public StorageConfigurer create() {
			if (isApplicable()) {
				return doCreate();
			}
			return null;
		}
	}

	static Version getUSSVersion() {
		Bundle bundle = FrameworkUtil.getBundle(IStorage.class);
		return bundle.getVersion();
	}

	private static StorageConfigurer instance;

	private static StorageConfigurer create() throws CoreException {
		String[] factories = getFactories();

		MultiStatus errors = new MultiStatus(MarketplaceClientCore.BUNDLE_ID, 0,
				"Failed to create user storage configurer", null);
		for (String factoryName : factories) {
			try {
				Class<? extends Factory> factoryClass = Class.forName(factoryName).asSubclass(Factory.class);
				Factory factory = factoryClass.newInstance();
				StorageConfigurer configurer = factory.create();
				if (configurer != null) {
					return configurer;
				}
			} catch (Exception e) {
				errors.add(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, factoryName, e));
			}
		}
		try {
			return new USS10StorageConfigurer();
		} catch (Throwable t) {
			errors.add(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, "Fallback", t));
		}
		throw new CoreException(errors);
	}

	private static String[] getFactories() {
		String[] factories = DEFAULT_FACTORIES;
		String factoriesValue = System.getProperty(PROP_FACTORIES);
		if (factoriesValue != null && !factoriesValue.isEmpty()) {
			String[] givenFactories = factoriesValue.split("\\s+"); //$NON-NLS-1$
			List<String> givenFactoriesList = new ArrayList<String>(Arrays.asList(givenFactories));
			Set<String> supportedFactories = new HashSet<String>(Arrays.asList(DEFAULT_FACTORIES));
			givenFactoriesList.retainAll(supportedFactories);
			factories = givenFactoriesList.toArray(new String[givenFactoriesList.size()]);
		}
		return factories;
	}

	public static synchronized StorageConfigurer get() throws CoreException {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	public static synchronized StorageConfigurer reset() throws CoreException {
		StorageConfigurer oldInstance = instance;
		instance = null;
		StorageConfigurer newInstance = get();
		if (oldInstance != null && newInstance.getClass() == oldInstance.getClass()) {
			instance = oldInstance;
		}
		return instance;
	}

	public static synchronized void unset() {
		instance = null;
	}

	public void setShellProvider(IStorage storage, Object value) {
		//do nothing
	}

	public abstract void configure(IStorage storage) throws CoreException;

	public abstract Object setInteractive(IStorage storage, boolean interactive) throws CoreException;

	abstract void restoreInteractive(IStorage storage, Object restoreValue) throws CoreException;

	public <T> T runWithLogin(IStorage storage, Callable<T> c) throws Exception {
		final Object restoreValue = setInteractive(storage, true);
		try {
			T result = c.call();
			return result;
		} finally {
			restoreInteractive(storage, restoreValue);
		}
	}
}
