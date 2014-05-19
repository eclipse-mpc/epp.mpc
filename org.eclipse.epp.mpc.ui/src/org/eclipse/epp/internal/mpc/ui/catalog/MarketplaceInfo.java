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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
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
	 * Calculate the known catalog nodes that are installed
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
				if (computeInstalled(installedIus, ius)) {
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
		if (node.getIus() != null && !node.getIus().getIu().isEmpty()) {
			List<String> ius = new ArrayList<String>(new HashSet<String>(node.getIus().getIu()));
			return computeInstalled(installedFeatures, ius);
		}
		return false;
	}

	private boolean computeInstalled(Set<String> installedIus, List<String> ius) {
		int installCount = 0;
		for (String iu : ius) {
			if (installedIus.contains(iu) || installedIus.contains(iu + P2_FEATURE_GROUP_SUFFIX)) {
				++installCount;
			}
		}
		// FIXME: review do we need to have _all_ installed, or just a minimum of one?  relates to bug 305441
		return installCount > 0;
	}

	public synchronized void map(URL marketUrl, INode node) {
		String itemKey = computeItemKey(marketUrl, node);
		if (node.getIus() != null && !node.getIus().getIu().isEmpty()) {
			List<String> ius = new ArrayList<String>(new HashSet<String>(node.getIus().getIu()));
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

		File registryFile = computeRegistryFile();
		if (registryFile != null && registryFile.exists()) {
			synchronized (MarketplaceInfo.class) {
				try {
					final InputStream in = new BufferedInputStream(new FileInputStream(registryFile));
					try {
						XMLDecoder decoder = new XMLDecoder(in);
						Object object = decoder.readObject();
						decoder.close();
						if (object instanceof MarketplaceInfo) {
							return (MarketplaceInfo) object;
						}
					} finally {
						in.close();
					}
				} catch (Throwable t) {
					// ignore, fallback
					MarketplaceClientUi.error(t);
				}
			}
		}
		return new MarketplaceInfo();
	}

	public void save() {
		File registryFile = computeRegistryFile();
		if (registryFile != null) {
			synchronized (MarketplaceInfo.class) {
				try {
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
		}
	}

	/**
	 * compute the registry file
	 *
	 * @return the registry file, or null if there's no persistent registry.
	 */
	private static final File computeRegistryFile() {
		// compute the file we'll use for registry persistence, starting with the platform configuration location
		File dataFile = Platform.getBundle(MarketplaceClientUi.BUNDLE_ID)
				.getBundleContext()
				.getDataFile(PERSISTENT_FILE);
		if (dataFile != null) {
			return dataFile;
		}

		// platform config location no good, so let's try the user's home directory
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		File userHomeFile = new File(userHome);
		if (userHomeFile.exists()) {
			File mpcConfigLocation = new File(userHomeFile, ".eclipse_mpc"); //$NON-NLS-1$
			if (!mpcConfigLocation.exists()) {
				if (!mpcConfigLocation.mkdir()) {
					return null;
				}
			}
			return computeConfigFile(mpcConfigLocation);
		}
		return null;
	}

	private static File computeConfigFile(File mpcConfigLocation) {
		return new File(mpcConfigLocation, PERSISTENT_FILE);
	}

}
