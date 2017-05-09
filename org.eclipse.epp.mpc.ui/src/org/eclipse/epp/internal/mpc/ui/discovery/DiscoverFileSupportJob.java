/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

final class DiscoverFileSupportJob extends Job {
	private final Display display;

	private final IEditorRegistry editorRegistry;

	private final IEditorDescriptor defaultDescriptor;

	private final String fileName;

	public DiscoverFileSupportJob(IEditorRegistry editorRegistry, IEditorDescriptor defaultDescriptor,
			String fileName) {
		super(Messages.AskMarketPlaceForFileSupportStrategy_jobName);
		this.display = Display.getCurrent();
		this.editorRegistry = editorRegistry;
		this.defaultDescriptor = defaultDescriptor;
		this.fileName = fileName;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		BundleContext bundleContext = MarketplaceClientUiPlugin.getBundleContext();
		ServiceReference<IMarketplaceServiceLocator> locatorReference = bundleContext
				.getServiceReference(IMarketplaceServiceLocator.class);
		IMarketplaceServiceLocator locator = bundleContext.getService(locatorReference);
		IMarketplaceService marketplaceService = locator.getDefaultMarketplaceService();
		try {
			return run(marketplaceService, monitor);
		} finally {
			bundleContext.ungetService(locatorReference);
		}
	}

	private IStatus run(IMarketplaceService marketplaceService, IProgressMonitor monitor) {
		final String fileExtension = getFileExtension(fileName);
		String fileExtensionTag = getFileExtensionTag(fileExtension);
		final List<? extends INode> nodes;
		try {
			ISearchResult searchResult = marketplaceService.tagged(fileExtensionTag, monitor);
			nodes = searchResult.getNodes();
		} catch (CoreException ex) {
			return new Status(IStatus.ERROR,
					MarketplaceClientUiPlugin.getInstance().getBundle().getSymbolicName(), ex.getMessage(), ex);
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

	static String getFileExtensionLabel(String fileName) {
		String fileExtension = getFileExtension(fileName);
		return fileExtension.length() == fileName.length() ? fileName
				: "*." + fileExtension; //$NON-NLS-1$
	}

	static String getFileExtension(String fileName) {
		String[] split = fileName.split("\\."); //$NON-NLS-1$
		return split[split.length - 1];
	}
}