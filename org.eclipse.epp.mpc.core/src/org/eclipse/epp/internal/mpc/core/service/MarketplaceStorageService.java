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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.eclipse.userstorage.spi.ISettings;
import org.eclipse.userstorage.util.FileStorageCache;
import org.eclipse.userstorage.util.Settings;
import org.osgi.framework.BundleContext;

@SuppressWarnings("restriction")
public class MarketplaceStorageService implements IMarketplaceStorageService {

	private static final String DEFAULT_STORAGE_SERVICE_NAME = Messages.MarketplaceStorageService_defaultStorageServiceName;

	static final String DEFAULT_APPLICATION_TOKEN = "MZ04RMOpksKN5GpxKXafq2MSjSP"; //$NON-NLS-1$

	private String applicationToken = DEFAULT_APPLICATION_TOKEN;

	private URI serviceUri;

	private StorageFactory storageFactory;

	private IStorage storage;

	private IStorageService.Dynamic customStorageService;

	private List<LoginListener> loginListeners;

	public URI getServiceUri() {
		return serviceUri;
	}

	public synchronized void setServiceUri(URI serviceUri) {
		if ((this.serviceUri == null && serviceUri != null)
				|| (this.serviceUri != null && !this.serviceUri.equals(serviceUri))) {
			this.serviceUri = serviceUri;
			storageFactory = null;
			storage = null;
		}
	}

	public synchronized StorageFactory getStorageFactory() {
		if (storageFactory == null) {
			storageFactory = createStorageFactory();
		}
		return storageFactory;
	}

	public synchronized void setStorageFactory(StorageFactory storageFactory) {
		this.storageFactory = storageFactory;
	}

	protected StorageFactory createStorageFactory() {
		if (serviceUri == null) {
			return StorageFactory.DEFAULT;
		}
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(applicationToken, serviceUri.toString());
		ISettings storageFactorySettings = new Settings.MemorySettings(settingsMap);
		return new StorageFactory(storageFactorySettings);
	}

	protected IStorage createStorage() {
		IStorage storage = getStorageFactory().create(applicationToken,
				new FileStorageCache.SingleApplication(this.applicationToken));
		storage.setCredentialsProvider(ICredentialsProvider.CANCEL);
		return storage;
	}

	public synchronized IStorage getStorage() {
		if (storage == null) {
			storage = createStorage();
		}
		return storage;
	}

	public void setStorage(IStorage storage) {
		this.storage = storage;
	}

	public IBlob getBlob(String key) {
		return getStorage().getBlob(key);
	}

	public String getRegisteredUser() {
		Credentials credentials = getStorageCredentials();
		return credentials == null ? null : credentials.getUsername();
	}

	public <T> T runWithLogin(Callable<T> c) throws Exception {
		final IStorage storage = getStorage();
		final ICredentialsProvider originalCredentialsProvider = storage.getCredentialsProvider();
		storage.setCredentialsProvider(null);
		String oldUser = getCurrentUser();
		try {
			T result = c.call();
			return result;
		} finally {
			storage.setCredentialsProvider(originalCredentialsProvider);
			String newUser = getCurrentUser();
			notifyLoginChanged(oldUser, newUser);
		}
	}

	private void notifyLoginChanged(String oldUser, String newUser) {
		if ((newUser == null && oldUser != null) || (newUser != null && !newUser.equals(oldUser))) {
			List<LoginListener> loginListeners = this.loginListeners;
			if (loginListeners != null && !loginListeners.isEmpty()) {
				for (LoginListener loginListener : loginListeners) {
					loginListener.loginChanged(oldUser, newUser);
				}
			}
		}
	}

	public synchronized void addLoginListener(LoginListener listener) {
		if (loginListeners == null) {
			loginListeners = new CopyOnWriteArrayList<LoginListener>();
		}
		if (!loginListeners.contains(listener)) {
			loginListeners.add(listener);
		}
	}

	public synchronized void removeLoginListener(LoginListener listener) {
		if (loginListeners != null) {
			loginListeners.remove(listener);
		}
	}

	private String getCurrentUser() {
		Credentials credentials = getStorageCredentials();
		if (credentials != null && credentials.getPassword() != null) {
			return credentials.getUsername();
		}
		return null;
	}

	private Credentials getStorageCredentials() {
		StorageService service = (StorageService) getStorage().getService();
		Credentials credentials = service.getCredentials();
		return credentials;
	}

	public void activate(BundleContext context, Map<?, ?> properties) {
		Object serviceUrlValue = ServiceUtil.getOverridablePropertyValue(properties, STORAGE_SERVICE_URL_PROPERTY);
		if (serviceUrlValue != null) {
			URI serviceUri = URI.create(serviceUrlValue.toString());
			String serviceName = getProperty(properties, STORAGE_SERVICE_NAME_PROPERTY, DEFAULT_STORAGE_SERVICE_NAME);
			registerStorageService(serviceUri, serviceName);
			setServiceUri(serviceUri);
		}
		String applicationToken = getProperty(properties, APPLICATION_TOKEN_PROPERTY, DEFAULT_APPLICATION_TOKEN);
		this.applicationToken = applicationToken;
	}

	private void registerStorageService(URI serviceUri, String serviceName) {
		customStorageService = null;
		IStorageService service = IStorageService.Registry.INSTANCE.getService(serviceUri);
		if (service == null) {
			customStorageService = IStorageService.Registry.INSTANCE.addService(serviceName, serviceUri);
		}
	}

	private static String getProperty(Map<?, ?> properties, String key, String defaultValue) {
		Object value = properties.get(key);
		if (value == null) {
			return defaultValue;
		}
		return value.toString();
	}

	public void deactivate() {
		if (customStorageService != null) {
			customStorageService.remove();
		}
		customStorageService = null;
	}
}
