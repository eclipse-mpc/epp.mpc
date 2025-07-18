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
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * A means of knowing about how nodes map to IUs and visa versa. Can handle nodes from multiple marketplaces, and does a
 * best-effort job at persisting information across sessions.
 *
 * @author David Green
 */
public class MarketplaceInfo {

	public static final String MPC_NODE_IU_PROPERTY = "org.eclipse.epp.mpc.node"; //$NON-NLS-1$

	public static final String MPC_FEATURE_IU = "org.eclipse.epp.mpc.feature.group"; //$NON-NLS-1$

	public static final String MPC_FEATURE_SITE = "http://download.eclipse.org/mpc"; //$NON-NLS-1$

	public static final String MPC_NODE_PATH = "/content/eclipse-marketplace-client"; //$NON-NLS-1$

	private static final String P2_FEATURE_GROUP_SUFFIX = ".feature.group"; //$NON-NLS-1$

	private static final String PERSISTENT_FILE = MarketplaceInfo.class.getSimpleName() + ".xml"; //$NON-NLS-1$

	private Map<String, List<String>> nodeKeyToIU = new HashMap<>();

	private Map<String, List<String>> iuToNodeKey = new HashMap<>();

	public MarketplaceInfo() {
	}

	public MarketplaceInfo(MarketplaceInfo info) {
		this();
		nodeKeyToIU.putAll(info.getNodeKeyToIU());
		iuToNodeKey.putAll(info.getIuToNodeKey());
	}

	public Map<String, List<String>> getNodeKeyToIU() {
		return nodeKeyToIU;
	}

	public void setNodeKeyToIU(Map<String, List<String>> nodeKeyToIU) {
		this.nodeKeyToIU = nodeKeyToIU;
	}

	public Map<String, List<String>> getIuToNodeKey() {
		return iuToNodeKey;
	}

	public void setIuToNodeKey(Map<String, List<String>> iuToNodeKey) {
		this.iuToNodeKey = iuToNodeKey;
	}

	/**
	 * Calculate the known catalog nodes that might be installed. Since no remote query should happen, this only checks
	 * if any one of the IUs for a node are installed.
	 *
	 * @param repositoryUrl
	 *            the catalog url for which installed nodes should be computed
	 * @param installedIus
	 *            all of the currently installed IUs
	 * @return a set of node ids, or an empty set if there are no known installed nodes
	 * @deprecated use {@link #computeInstalledNodes(URL, Map)} instead
	 */
	@Deprecated
	public synchronized Set<INode> computeInstalledNodes(URL repositoryUrl, Set<String> installedIus) {
		Set<INode> nodes = new HashSet<>();

		String keyPrefix = computeUrlKey(repositoryUrl) + '#';
		for (Map.Entry<String, List<String>> entry : nodeKeyToIU.entrySet()) {
			if (entry.getKey().startsWith(keyPrefix)) {
				List<String> ius = nodeKeyToIU.get(entry.getKey());
				if (computeInstalled(installedIus, ius, false)) {
					String nodeId = entry.getKey().substring(keyPrefix.length());
					Node node = new Node();
					node.setId(nodeId);
					nodes.add(node);
				}
			}
		}

		return nodes;
	}

	/**
	 * Calculate the known catalog nodes that might be installed. Since no remote query should happen, this only checks
	 * if any one of the IUs for a node are installed.
	 *
	 * @param repositoryUrl
	 *            the catalog url for which installed nodes should be computed
	 * @param installedIus
	 *            all of the currently installed IUs
	 * @return a set of node ids, or an empty set if there are no known installed nodes
	 */
	public synchronized Set<INode> computeInstalledNodes(URL repositoryUrl, Map<String, IInstallableUnit> installedIus) {
		Set<INode> nodes = new HashSet<>();

		String keyPrefix = computeUrlKey(repositoryUrl) + '#';
		for (Map.Entry<String, List<String>> entry : nodeKeyToIU.entrySet()) {
			if (entry.getKey().startsWith(keyPrefix)) {
				List<String> ius = nodeKeyToIU.get(entry.getKey());
				if (computeInstalled(installedIus.keySet(), ius, false)) {
					String nodeId = entry.getKey().substring(keyPrefix.length());
					INode node = QueryHelper.nodeById(nodeId);
					nodes.add(node);
				}
			}
		}
		for (IInstallableUnit iu : installedIus.values()) {
			String nodeUrlsValue = iu.getProperty(MPC_NODE_IU_PROPERTY);
			if (nodeUrlsValue == null) {
				continue;
			}
			String[] nodeUrls = nodeUrlsValue == null ? null : nodeUrlsValue.split("(\\s*,\\s*|\\s+)"); //$NON-NLS-1$
			for (String nodeUrl : nodeUrls) {
				if (nodeUrl.startsWith(repositoryUrl.toString())) {
					INode node = QueryHelper.nodeByUrl(nodeUrl);
					nodes.add(node);
				}
			}
		}

		return nodes;
	}

	/**
	 * Compute if the given node is installed. The given node must be fully realized, including its
	 * {@link INode#getIus() ius}.
	 * <p>
	 * <i>NOTE: This method is kept for backwards compatibility only.</i><br />
	 * It checks if the node has an update url and if that url is contained in the list of known repositories. Otherwise
	 * it assumes that the node is <b>not</b> installed, regardless of installed features. Please use
	 * {@link #computeInstalled(Set, INode)} instead to get a reliable answer.
	 *
	 * @param knownRepositories
	 * @deprecated use {@link #computeInstalled(Set, INode)} instead
	 */
	@Deprecated
	public boolean computeInstalled(Set<String> installedFeatures, Set<URI> knownRepositories, INode node) {
		String updateurl = node.getUpdateurl();
		if (updateurl == null) {
			// don't consider installed if there's no update site
			return false;
		}
		boolean installed = computeInstalled(installedFeatures, node);
		if (installed && knownRepositories != null) {
			// don't consider installed if the repository is not known/trusted
			try {
				URI uri = URLUtil.toURI(node.getUpdateurl());
				if (!knownRepositories.contains(uri)) {
					return false;
				}
			} catch (URISyntaxException e) {
				return false;
			}
		}
		return installed;
	}

	/**
	 * Compute if the given node is installed. The given node must be fully realized, including its
	 * {@link Node#getIus() ius}.
	 */
	public boolean computeInstalled(Set<String> installedFeatures, INode node) {
		if (node.getIus() != null && !node.getIus().getIuElements().isEmpty()) {
			boolean all = true;
			Set<String> ius = new HashSet<>();
			for (IIu iu : node.getIus().getIuElements()) {
				if (!iu.isOptional()) {
					ius.add(iu.getId());
				}
			}
			if (ius.isEmpty()) {
				all = false;
				for (IIu iu : node.getIus().getIuElements()) {
					ius.add(iu.getId());
				}
			}
			return computeInstalled(installedFeatures, ius, all);
		}
		return false;
	}

	private boolean computeInstalled(Set<String> installedIus, Collection<String> ius, boolean all) {
		int installCount = 0;
		for (String iu : ius) {
			if (computeInstalled(installedIus, iu)) {
				++installCount;
			}
		}
		return all ? installCount == ius.size() : installCount > 0;
	}

	public void computeInstalled(Set<String> installedFeatures, MarketplaceNodeCatalogItem catalogItem) {
		List<MarketplaceNodeInstallableUnitItem> installableUnitItems = catalogItem.getInstallableUnitItems();
		boolean installed = false;
		if (installableUnitItems != null) {
			boolean anyInstalled = false;
			boolean requiredInstalled = true;
			for (MarketplaceNodeInstallableUnitItem installableUnitItem : installableUnitItems) {
				boolean iuInstalled = computeInstalled(installedFeatures, installableUnitItem.getId());
				installableUnitItem.setInstalled(iuInstalled);
				if (iuInstalled) {
					anyInstalled = true;
				} else if (!installableUnitItem.isOptional()) {
					requiredInstalled = false;
				}
			}
			installed = requiredInstalled && anyInstalled;
		}
		catalogItem.setInstalled(installed);
	}

	private boolean computeInstalled(Set<String> installedIus, String iu) {
		return installedIus.contains(iu) || installedIus.contains(iu + P2_FEATURE_GROUP_SUFFIX);
	}

	public synchronized void map(URL marketUrl, INode node) {
		String itemKey = computeItemKey(marketUrl, node);
		if (node.getIus() != null && !node.getIus().getIuElements().isEmpty()) {
			List<String> ius = new ArrayList<>();
			Set<String> uniqueIus = new HashSet<>();
			List<IIu> iuElements = node.getIus().getIuElements();
			for (IIu iIu : iuElements) {
				if (uniqueIus.add(iIu.getId())) {
					ius.add(iIu.getId());
				}
			}
			nodeKeyToIU.put(itemKey, ius);
			for (String iu : ius) {
				List<String> catalogNodes = iuToNodeKey.get(iu);
				if (catalogNodes != null) {
					if (!catalogNodes.contains(itemKey)) {
						catalogNodes.add(itemKey);
					}
				} else {
					catalogNodes = new ArrayList<>(1);
					catalogNodes.add(itemKey);
					iuToNodeKey.put(iu, catalogNodes);
				}
			}
		} else {
			List<String> ius = nodeKeyToIU.remove(itemKey);
			if (ius != null) {
				for (String iu : ius) {
					List<String> catalogNodes = iuToNodeKey.get(iu);
					if (catalogNodes != null) {
						catalogNodes.remove(itemKey);
						if (catalogNodes.isEmpty()) {
							iuToNodeKey.remove(iu);
						}
					}
				}
			}
		}
	}

	private String computeItemKey(URL marketUrl, INode item) {
		return computeUrlKey(marketUrl) + '#' + item.getId();
	}

	private String computeUrlKey(URL url) {
		try {
			return url.toURI().toString();
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	public static boolean isMPCNode(INode item) {
		if (item.getUpdateurl() != null) {
			return item.getUpdateurl().startsWith(MPC_FEATURE_SITE);
		}

		if (item.getUrl() != null) {
			return item.getUrl().endsWith(MPC_NODE_PATH) || item.getUrl().endsWith(MPC_NODE_PATH + "/"); //$NON-NLS-1$
		}

		return false;
	}

	public static MarketplaceInfo getInstance() {

		MarketplaceInfo info = new MarketplaceInfo();
		MarketplaceInfo loaded = info.load();
		return loaded != null ? loaded : info;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected MarketplaceInfo load() {
		try {
			RegistryFile registryFile = createRegistryFile();
			File loadFile = registryFile.load();
			if (loadFile != null && loadFile.canRead()) {
				return doLoad(loadFile);
			}
		} catch (Exception ex) {
			//Never fail due to this
			MarketplaceClientUi.error(ex);
		}
		return null;
	}

	protected MarketplaceInfo doLoad(File loadFile) {
		synchronized (MarketplaceInfo.class) {
			try (InputStream in = new BufferedInputStream(new FileInputStream(loadFile));
					XMLDecoder decoder = new XMLDecoder(in)) {
				Object object = decoder.readObject();
				return (MarketplaceInfo) object;
			} catch (Throwable t) {
				// ignore, fallback
				IStatus status = new Status(IStatus.WARNING, MarketplaceClientUi.BUNDLE_ID,
						Messages.MarketplaceInfo_LoadError, t);
				MarketplaceClientUi.getLog().log(status);
				//try to delete broken file
				loadFile.delete();
				return null;
			}
		}
	}

	public void save() {
		RegistryFile registryFile = createRegistryFile();
		File saveFile = registryFile.save();
		if (saveFile != null) {
			synchronized (MarketplaceInfo.class) {
				save(saveFile);
			}
		}
	}

	public void save(File registryFile) {
		try {
			File container = registryFile.getParentFile();
			if (container != null && !container.exists()) {
				container.mkdirs();
			}
			OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(registryFile));
			try {
				XMLEncoder encoder = new XMLEncoder(outputStream);
				encoder.writeObject(this);
				encoder.close();
			} finally {
				outputStream.close();
			}
		} catch (Throwable t) {
			// fail safe
			MarketplaceClientUi.error(t);
		}
	}

	/**
	 * compute the registry file
	 * <p>
	 * This method is only protected for testing purposes. Do not override or call directly.
	 *
	 * @return the registry file, or null if there's no persistent registry.
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected RegistryFile createRegistryFile() {
		List<File> files = new ArrayList<>();
		File configFile = computeConfigurationAreaRegistryFile();
		if (configFile != null) {
			files.add(configFile);
		}
		File dataFile = computeBundleRegistryFile();
		if (dataFile != null) {
			files.add(dataFile);
		}

		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		File userHomeFile = new File(userHome);
		if (userHomeFile.exists()) {
			File userConfigFile = computeUserHomeRegistryFile(userHomeFile);
			File legacyConfigFile = computeLegacyUserHomeRegistryFile(userHomeFile);
			files.add(userConfigFile);
			files.add(legacyConfigFile);
		}
		return new RegistryFile(files.toArray(new File[files.size()]));
	}

	/**
	 * This method is only protected for testing purposes. Do not override or call directly.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected File computeConfigurationAreaRegistryFile() {
		File configurationArea = getConfigurationArea();
		if (configurationArea == null) {
			return null;
		}
		File mpcArea = new File(configurationArea, MarketplaceClientUi.BUNDLE_ID);
		File dataFile = new File(mpcArea, PERSISTENT_FILE);
		return dataFile;
	}

	protected File getConfigurationArea() {
		Location configurationLocation = getConfigurationLocation();
		URL url = configurationLocation == null ? null : configurationLocation.getURL();
		if (url == null) {
			return null;
		}
		File configurationArea;
		try {
			url = FileLocator.resolve(url);
			if (!"file".equals(url.getProtocol())) { //$NON-NLS-1$
				return null;
			}
			String path = url.getPath();
			try {
				path = URLDecoder.decode(path, StandardCharsets.UTF_8);
			} catch (IllegalArgumentException e) {
				//ignore
			}
			String query = url.getQuery();
			if (query != null) {
				try {
					query = URLDecoder.decode(query, StandardCharsets.UTF_8);
				} catch (IllegalArgumentException e) {
					//ignore
				}
			}
			String ref = url.getRef();
			if (ref != null) {
				try {
					ref = URLDecoder.decode(ref, StandardCharsets.UTF_8);
				} catch (IllegalArgumentException e) {
					//ignore
				}
			}
			URI uri = new URI("file", null, path, query, ref); //$NON-NLS-1$
			configurationArea = new File(uri);
		} catch (Exception e) {
			MarketplaceClientUi.error(e);
			return null;
		}
		return configurationArea;
	}

	protected Location getConfigurationLocation() {
		Location configurationLocation = Platform.getConfigurationLocation();
		return configurationLocation;
	}

	/**
	 * This method is only protected for testing purposes. Do not override or call directly.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected File computeBundleRegistryFile() {
		File dataFile = Platform.getBundle(MarketplaceClientUi.BUNDLE_ID)
				.getBundleContext()
				.getDataFile(PERSISTENT_FILE);
		return dataFile;
	}

	/**
	 * This method is only protected for testing purposes. Do not override or call directly.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected File computeUserHomeRegistryFile(File userHome) {
		File eclipseConfigLocation = new File(userHome, ".eclipse/mpc"); //$NON-NLS-1$
		File configFile = computeConfigFile(eclipseConfigLocation);
		return configFile;
	}

	/**
	 * This method is only protected for testing purposes. Do not override or call directly.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected File computeLegacyUserHomeRegistryFile(File userHome) {
		File legacyConfigLocation = new File(userHome, ".eclipse_mpc"); //$NON-NLS-1$
		File legacyConfigFile = computeConfigFile(legacyConfigLocation);
		return legacyConfigFile;
	}

	private static File computeConfigFile(File mpcConfigLocation) {
		return new File(mpcConfigLocation, PERSISTENT_FILE);
	}

	/**
	 * This is only non-private for testing purposes
	 *
	 * @noreference This class is not intended to be referenced by clients.
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	protected static class RegistryFile {
		private final File[] locations;

		public RegistryFile(File... locations) {
			this.locations = locations;
		}

		public RegistryFile(RegistryFile registryFile) {
			this(registryFile.getLocations());
		}

		protected File[] getLocations() {
			return locations;
		}

		public File load() {
			for (File file : locations) {
				if (isFile(file) && canRead(file)) {
					return file;
				}
			}
			if (locations.length > 0) {
				return locations[0];
			}
			return null;
		}

		public File save() {
			for (int i = 0; i < locations.length; i++) {
				File file = locations[i];
				try {
					if ((isDirectory(file.getParentFile()) || mkdirs(file.getParentFile()))
							&& ((isFile(file) && canWrite(file)) || (!exists(file) && createNewFile(file)))) {
						for (int j = i + 1; j < locations.length; j++) {
							File parentFile = locations[j].getParentFile();
							if (exists(parentFile)) {
								if (locations[j].exists() && !locations[j].delete()) {
									locations[j].deleteOnExit();
								}
								if (!parentFile.delete()) {
									parentFile.deleteOnExit();
								}
							}
						}
						return file;
					}
				} catch (IOException ex) {
					//ignore
				}
			}
			if (locations.length > 0) {
				return locations[0];
			}
			return null;
		}

		protected boolean mkdirs(File file) {
			return file.mkdirs();
		}

		protected boolean isDirectory(File file) {
			return file.isDirectory();
		}

		protected boolean createNewFile(File file) throws IOException {
			return file.createNewFile();
		}

		protected boolean exists(File file) {
			return file.exists();
		}

		protected boolean canWrite(File file) {
			return file.canWrite();
		}

		protected boolean canRead(File file) {
			return file.canRead();
		}

		protected boolean isFile(File file) {
			return file.isFile();
		}
	}
}
