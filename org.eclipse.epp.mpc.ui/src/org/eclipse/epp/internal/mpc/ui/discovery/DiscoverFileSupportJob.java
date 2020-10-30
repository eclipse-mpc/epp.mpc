/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	Mickael Istria (Red Hat Inc.) - initial implementation
 *  Lucas Bullen (Red Hat Inc.) -	Bug 517818: search IDE extensions by all subname's of the file
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.model.ITag;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

final class DiscoverFileSupportJob extends Job {
	private final Display display;

	private final IEditorRegistry editorRegistry;

	private final IEditorDescriptor defaultDescriptor;

	private final String fileName;

	public DiscoverFileSupportJob(IEditorRegistry editorRegistry, IEditorDescriptor defaultDescriptor,
			String fileName) {
		super(NLS.bind(Messages.AskMarketPlaceForFileSupportStrategy_jobName, getFileExtensionLabel(fileName)));
		this.display = Display.getCurrent();
		this.editorRegistry = editorRegistry;
		this.defaultDescriptor = defaultDescriptor;
		this.fileName = fileName;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		ServiceReference<IMarketplaceServiceLocator> locatorReference = bundleContext
				.getServiceReference(IMarketplaceServiceLocator.class);
		IMarketplaceServiceLocator locator = bundleContext.getService(locatorReference);
		IMarketplaceService marketplaceService = locator
				.getDefaultMarketplaceService();
		try {
			return run(marketplaceService, monitor);
		} finally {
			bundleContext.ungetService(locatorReference);
		}
	}

	private IStatus run(IMarketplaceService marketplaceService, IProgressMonitor monitor) {
		final List<String> fileExtensions = getFileExtensions(fileName);
		final List<String> fileExtensionTags = new ArrayList<>();

		for (String string : fileExtensions) {
			fileExtensionTags.add(getFileExtensionTag(string));
		}

		final List<? extends INode> nodes;
		try {
			ISearchResult searchResult = marketplaceService.tagged(fileExtensionTags, monitor);
			nodes = orderNodesByTagSubExtensionCount(searchResult.getNodes(), fileExtensionTags);
		} catch (Exception ex) {
			IStatus status = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
					NLS.bind(Messages.DiscoverFileSupportJob_discoveryFailed, getFileExtensionLabel(fileName)), ex);
			// Do not return this status as it would show an error, e.g. when the user is currently offline
			MarketplaceClientUi.getLog().log(status);
			return Status.CANCEL_STATUS;
		}
		if (nodes.isEmpty()) {
			return Status.OK_STATUS;
		}
		UIJob openDialog = new ShowFileSupportProposalsJob(fileName, nodes, editorRegistry, defaultDescriptor,
				display);
		openDialog.setPriority(Job.INTERACTIVE);
		openDialog.setSystem(true);
		openDialog.schedule();
		return Status.OK_STATUS;
	}

	private static String getFileExtensionTag(final String fileExtension) {
		return "fileExtension_" + fileExtension;//$NON-NLS-1$
	}

	/**
	 * Sorts the given list of tags by number of sub extension dividers(.) and uses the sorted list to return an ordered
	 * list of the nodes by first occurrence of highest sub extension tag in fileExtensionTags.
	 *
	 * @param nodes
	 * @param fileExtensionTags
	 * @return nodes ordered by first occurrence of highest sub extension count found in fileExtensionTags
	 */
	private static List<? extends INode> orderNodesByTagSubExtensionCount(List<? extends INode> nodes,
			List<String> fileExtensionTags) {

		Collections.sort(fileExtensionTags, (s1, s2) -> (s2.length() - s2.replace(".", "").length())
				- (s1.length() - s1.replace(".", "").length()));

		Map<String, List<INode>> nodesByTags = new HashMap<>();
		for (INode iNode : nodes) {
			if (iNode.getTags() == null || iNode.getTags().getTags() == null) {
				continue;
			}
			for (ITag nodeTag : iNode.getTags().getTags()) {
				boolean foundTag = false;
				for (String tag : fileExtensionTags) {
					if (nodeTag.getName().equals(tag)) {
						if (nodesByTags.containsKey(tag)) {
							nodesByTags.get(tag).add(iNode);
						} else {
							List<INode> newNodeList = new ArrayList<>();
							newNodeList.add(iNode);
							nodesByTags.put(tag, newNodeList);
						}
						foundTag = true;
						break;
					}
				}
				if (foundTag) {
					break;
				}
			}
		}
		List<INode> ordered = new ArrayList<>();
		for (String tag : fileExtensionTags) {
			if (nodesByTags.containsKey(tag)) {
				ordered.addAll(nodesByTags.get(tag));
			}
		}
		return nodes;
	}

	/**
	 * @param fileName
	 *            full name of the file including all extensions
	 * @return The last segment of the file's extension. eg "file.tar.gz" returns "gz"
	 */
	static String getFileExtensionLabel(String fileName) {
		return fileName.indexOf('.') == -1 ? fileName
				: '*' + fileName.substring(fileName.lastIndexOf('.'), fileName.length());
	}

	/**
	 * @param fileName
	 *            full name of the file including all extensions
	 * @return All sub-strings of the fileName in longest to shortest order split by periods moving from left to right.
	 *         eg "file.tar.gz" returns ["file.tar.gz", "tar.gz", "gz"]
	 */
	static List<String> getFileExtensions(String fileName) {
		List<String> extensions = new ArrayList<>();
		while (fileName.length() > 0) {
			extensions.add(fileName);
			if (fileName.indexOf('.') == -1) {
				break;
			}
			fileName = fileName.substring(fileName.indexOf('.') + 1, fileName.length());
		}
		return extensions;
	}
}