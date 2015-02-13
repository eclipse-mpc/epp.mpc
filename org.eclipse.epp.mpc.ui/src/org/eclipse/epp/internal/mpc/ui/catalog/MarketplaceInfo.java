/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.INode;

/**
 * A means of knowing about how nodes map to IUs and visa versa. Can handle nodes from multiple marketplaces, and does a
 * best-effort job at persisting information across sessions.
 *
 * @author David Green
 */
public class MarketplaceInfo {

	private static final String P2_FEATURE_GROUP_SUFFIX = ".feature.group"; //$NON-NLS-1$

	private static final String PERSISTENT_FILE = MarketplaceInfo.class.getSimpleName() + ".xml"; //$NON-NLS-1$

	private Map<String, List<String>> nodeKeyToIU = new HashMap<String, List<String>>();

	private Map<String, List<String>> iuToNodeKey = new HashMap<String, List<String>>();

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
	 */
	public synchronized Set<INode> computeInstalledNodes(URL repositoryUrl, Set<String> installedIus) {
		Set<INode> nodes = new HashSet<INode>();

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
			Set<String> ius = new HashSet<String>();
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
			List<String> ius = new ArrayList<String>();
			Set<String> uniqueIus = new HashSet<String>();
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
					catalogNodes = new ArrayList<String>(1);
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

	public static MarketplaceInfo getInstance() {

		MarketplaceInfo info = new MarketplaceInfo();
		MarketplaceInfo loaded = info.load();
		return loaded != null ? loaded : info;
	}

	/**
	 * This method is only public for testing purposes. Do not override or call directly.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public MarketplaceInfo load() {
		File registryFile = computeRegistryFile(false);
		synchronized (MarketplaceInfo.class) {
			if (registryFile != null && registryFile.isFile() && registryFile.length() > 0) {
				try {
					final InputStream in = new BufferedInputStream(new FileInputStream(registryFile));
					try {
						XMLDecoder decoder = new XMLDecoder(in);
						Object object = decoder.readObject();
						decoder.close();
						return (MarketplaceInfo) object;
					} finally {
						in.close();
					}
				} catch (Throwable t) {
					// ignore, fallback
					IStatus status = new Status(IStatus.WARNING, MarketplaceClientUi.BUNDLE_ID,
							Messages.MarketplaceInfo_LoadError, t);
					MarketplaceClientUi.getLog().log(status);
					//try to delete broken file
					registryFile.delete();
				}
			}
		}
		return null;
	}

	public void save() {
		File registryFile = computeRegistryFile(true);
		if (registryFile != null) {
			synchronized (MarketplaceInfo.class) {
				save(registryFile);
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
	protected File computeRegistryFile(boolean save) {
		// compute the file we'll use for registry persistence, starting with the platform configuration location
		File dataFile = computeBundleRegistryFile();
		if (dataFile != null) {
			return dataFile;
		}

		// platform config location no good, so let's try the user's home directory
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		File userHomeFile = new File(userHome);
		if (userHomeFile.exists()) {
			File configFile = computeUserHomeRegistryFile(userHomeFile);

			configFile = handleLegacyRegistryFile(userHomeFile, configFile, save);

			return configFile;
		}
		return null;
	}

	private File handleLegacyRegistryFile(File userHome, File configFile, boolean save) {
		File legacyConfigFile = computeLegacyUserHomeRegistryFile(userHome);
		if (!configFile.getParentFile().isDirectory() && !configFile.getParentFile().mkdirs()) {
			configFile = legacyConfigFile;// .eclipse dir is not writable, just use legacy if that exists
		} else if (legacyConfigFile != null) {
			if (save) {
				if (!legacyConfigFile.delete()) {
					legacyConfigFile.deleteOnExit();
				}
				if (!legacyConfigFile.getParentFile().delete()) {
					legacyConfigFile.getParentFile().deleteOnExit();
				}
			} else {
				//load from new location if it exists or fall back to old one
				if (!configFile.isFile()) {
					configFile = legacyConfigFile;
				}
			}
		}
		return configFile;
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
		eclipseConfigLocation.mkdirs();

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
		if (legacyConfigLocation.isDirectory()) {
			File legacyConfigFile = computeConfigFile(legacyConfigLocation);
			if (legacyConfigFile.isFile()) {
				return legacyConfigFile;
			}
		}
		return null;
	}

	private static File computeConfigFile(File mpcConfigLocation) {
		return new File(mpcConfigLocation, PERSISTENT_FILE);
	}

}
