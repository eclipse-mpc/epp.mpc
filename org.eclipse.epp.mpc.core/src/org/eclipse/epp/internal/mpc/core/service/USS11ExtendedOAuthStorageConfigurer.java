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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.oauth.EclipseOAuthCredentialsProvider;
import org.eclipse.userstorage.spi.ICredentialsProvider;

class USS11ExtendedOAuthStorageConfigurer extends USS11OAuthStorageConfigurer {

	static class Factory extends USS11OAuthStorageConfigurer.Factory {
		static Boolean hasInteractiveMethods = null;

		static Method setInteractiveMethod = null;

		static Method isInteractiveMethod = null;

		@Override
		boolean isApplicable() {
			if (!super.isApplicable()) {
				return false;
			}
			synchronized (Factory.class) {
				if (hasInteractiveMethods != null) {
					return hasInteractiveMethods;
				}
				try {
					Class<?> providerClass = Class
							.forName("org.eclipse.userstorage.oauth.EclipseOAuthCredentialsProvider");
					Method setInteractiveMethod = providerClass.getMethod("setInteractive", boolean.class);
					Method isInteractiveMethod = providerClass.getMethod("isInteractive");
					hasInteractiveMethods = true;
					Factory.setInteractiveMethod = setInteractiveMethod;
					Factory.isInteractiveMethod = isInteractiveMethod;
					return true;
				} catch (Exception ex) {
					disable();
					return false;
				}
			}
		}

		@Override
		StorageConfigurer doCreate() {
			return new USS11ExtendedOAuthStorageConfigurer();
		}

		static void disable() {
			synchronized (Factory.class) {
				hasInteractiveMethods = false;
				Factory.setInteractiveMethod = null;
				Factory.isInteractiveMethod = null;
			}
		}
	}

	@Override
	ICredentialsProvider createCredentialsProvider() {
		return createOAuthCredentialsProvider();
	}

	@Override
	public Object setInteractive(IStorage storage, boolean interactive) throws CoreException {
		try {
			return super.setInteractive(storage, interactive);
		} catch (RuntimeException ex) {
			StorageConfigurer configurer = StorageConfigurer.reset();
			if (configurer != this) {
				configurer.configure(storage);
				return configurer.setInteractive(storage, interactive);
			}
			throw new CoreException(new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID,
					"Failed to change interactive authentication mode", ex));
		}
	}

	@Override
	boolean setInteractive(ICredentialsProvider credentialsProvider, boolean interactive) {
		EclipseOAuthCredentialsProvider oauthProvider = (EclipseOAuthCredentialsProvider) credentialsProvider;
		Method setInteractiveMethod;
		Method isInteractiveMethod;
		synchronized (Factory.class) {
			if (!Boolean.TRUE.equals(Factory.hasInteractiveMethods)) {
				throw new UnsupportedOperationException();
			}
			setInteractiveMethod = Factory.setInteractiveMethod;
			isInteractiveMethod = Factory.isInteractiveMethod;
		}
		boolean oldInteractive = invoke(oauthProvider, isInteractiveMethod, Boolean.class);
		invoke(oauthProvider, setInteractiveMethod, Void.class, interactive);
		return oldInteractive;
	}

	private static <T> T invoke(Object target, Method method, Class<T> returnType, Object... parameters) {
		try {
			Object result = method.invoke(target, parameters);
			return returnType.cast(result);
		} catch (IllegalAccessException e) {
			Factory.disable();
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			Factory.disable();
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}
}
