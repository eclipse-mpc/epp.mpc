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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.util.ServiceUtil;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.oauth.EclipseOAuthCredentialsProvider;
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

	private static final String[] MIGRATE_SECURE_STORAGE_KEYS = { "username", "password", "termsOfUseAgreed" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

	private String applicationToken = DEFAULT_APPLICATION_TOKEN;

	private URI serviceUri;

	private StorageFactory storageFactory;

	private IStorage storage;

	private IStorageService.Dynamic customStorageService;

	private List<LoginListener> loginListeners;

	private EclipseOAuthCredentialsProvider credentialsProvider;

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
		credentialsProvider = new EclipseOAuthCredentialsProvider(new MPCOAuthParameters());
		credentialsProvider.setInteractive(false);
		storage.setCredentialsProvider(credentialsProvider);
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
		String oldUser = getCurrentUser();
		boolean wasInteractive = credentialsProvider.isInteractive();
		credentialsProvider.setInteractive(true);
		try {
			T result = c.call();
			return result;
		} finally {
			credentialsProvider.setInteractive(wasInteractive);
			String newUser = getCurrentUser();//TODO the OAuth token might change with the user staying the same - this would cause an unnecessary refresh event
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
		ICredentialsProvider provider = getStorage().getCredentialsProvider();
		if (provider == null) {
			return null;
		}
		IStorageService service = getStorage().getService();
		return provider.hasCredentials(service) ? provider.getCredentials(service) : null;
	}

	public void activate(BundleContext context, Map<?, ?> properties) {
		Object serviceUrlValue = ServiceUtil.getOverridablePropertyValue(properties, STORAGE_SERVICE_URL_PROPERTY);
		if (serviceUrlValue != null) {
			URI serviceUri = URI.create(serviceUrlValue.toString());
			String serviceName = getProperty(properties, STORAGE_SERVICE_NAME_PROPERTY, DEFAULT_STORAGE_SERVICE_NAME);
			IStorageService registered = registerStorageService(serviceUri, serviceName);
			setServiceUri(registered.getServiceURI());
		}
		String applicationToken = getProperty(properties, APPLICATION_TOKEN_PROPERTY, DEFAULT_APPLICATION_TOKEN);
		this.applicationToken = applicationToken;
	}

	private IStorageService registerStorageService(URI serviceUri, String serviceName) {
		customStorageService = null;
		URI normalizedUri = normalizePath(serviceUri);
		URI denormalizedUri = normalizePath(serviceUri, false);
		IStorageService service = IStorageService.Registry.INSTANCE.getService(normalizedUri);
		IStorageService extraService = IStorageService.Registry.INSTANCE.getService(denormalizedUri);
		if (service == null) {
			if (extraService != null) {
				return extraService;
			}
			customStorageService = IStorageService.Registry.INSTANCE.addService(serviceName, normalizedUri);
			service = customStorageService;
			return service;
		}
		if (extraService != null && extraService != service) {
			service = cleanupExtraStorageService(service, extraService);
		}

		return service;
	}

	private static IStorageService cleanupExtraStorageService(IStorageService service, IStorageService extraService) {
		IStorageService.Dynamic removeDynamic;
		IStorageService keepService;
		if (extraService instanceof IStorageService.Dynamic) {
			removeDynamic = (IStorageService.Dynamic) extraService;
			keepService = service;
		} else if (service instanceof IStorageService.Dynamic) {
			removeDynamic = (IStorageService.Dynamic) service;
			keepService = extraService;
		} else {
			return service;
		}
		if (removeDynamic instanceof StorageService && keepService instanceof StorageService) {
			StorageService removeImpl = (StorageService) removeDynamic;
			StorageService keepImpl = (StorageService) keepService;
			ISecurePreferences removeSecurePreferences = removeImpl.getSecurePreferences();
			ISecurePreferences keepSecurePreferences = keepImpl.getSecurePreferences();
			try {
				copySecurePreferences(removeSecurePreferences, keepSecurePreferences);
			} catch (Exception e) {
				MarketplaceClientCore.error(NLS.bind("Failed to migrate secure storage values from {0} to {1}",
						removeDynamic.getServiceURI(), keepService.getServiceURI()), e);
			}
		}
		removeDynamic.remove();
		return keepService;
	}

	private static void copySecurePreferences(ISecurePreferences source, ISecurePreferences target)
			throws StorageException, IOException {
		Set<String> sourceKeys = new HashSet<String>(Arrays.asList(source.keys()));
		Set<String> targetKeys = new HashSet<String>(Arrays.asList(target.keys()));
		boolean changed = false;
		for (String key : MIGRATE_SECURE_STORAGE_KEYS) {
			if (sourceKeys.contains(key) && !targetKeys.contains(key)) {
				boolean encrypted = source.isEncrypted(key);
				target.put(key, source.get(key, null), encrypted);
				changed = true;
			}
		}
		if (changed) {
			target.flush();
		}
	}

	private static URI normalizePath(URI uri) {
		return normalizePath(uri, true);
	}

	private static URI normalizePath(URI uri, boolean trailingSlash) {
		if (uri.isOpaque()) {
			return uri;
		}
		String path = uri.getPath();
		String normalizedPath;
		if (path == null) {
			if (trailingSlash) {
				normalizedPath = "/"; //$NON-NLS-1$
			} else {
				return uri;
			}
		} else if (path.endsWith("/")) { //$NON-NLS-1$
			if (trailingSlash) {
				return uri;
			} else {
				normalizedPath = path.length() == 1 ? null : path.substring(0, path.length() - 1);
			}
		} else {
			if (trailingSlash) {
				normalizedPath = path + "/"; //$NON-NLS-1$
			} else {
				return uri;
			}
		}
		try {
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), normalizedPath,
					uri.getQuery(), uri.getFragment());
		} catch (URISyntaxException e) {
			return uri;
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
