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
package org.eclipse.epp.internal.mpc.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.model.ITag;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ide.IUnassociatedEditorStrategy;
import org.eclipse.ui.internal.ide.SystemEditorOrTextEditorStrategy;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * For a given file, search entries on marketplace that would match the search "fileExtension_${extension}". MarketPlace
 * entry can declare support for some extension by adding these terms as tags.
 *
 * @author mistria
 */
public class AskMarketPlaceForFileSupportStrategy implements IUnassociatedEditorStrategy {


	public AskMarketPlaceForFileSupportStrategy() {
	}

	public IEditorDescriptor getEditorDescriptor(final String fileName, final IEditorRegistry editorRegistry)
			throws CoreException, OperationCanceledException {
		final IEditorDescriptor res = new SystemEditorOrTextEditorStrategy().getEditorDescriptor(fileName, editorRegistry);
		final Display display = Display.getCurrent();
		Job mpcJob = new Job(Messages.AskMarketPlaceForFileSupportStrategy_jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				BundleContext bundleContext = MarketplaceClientUiPlugin.getBundleContext();
				ServiceReference<IMarketplaceServiceLocator> locatorReference = bundleContext
						.getServiceReference(IMarketplaceServiceLocator.class);
				IMarketplaceServiceLocator locator = bundleContext.getService(locatorReference);
				IMarketplaceService marketplaceService = locator.getDefaultMarketplaceService();
				String[] split = fileName.split("\\."); //$NON-NLS-1$
				final String fileExtension = split[split.length - 1];
				String query = "fileExtension_" + fileExtension; //$NON-NLS-1$]
				ISearchResult searchResult = null;
				try {
					searchResult = marketplaceService.search(null, null, query, monitor);
				} catch (CoreException ex) {
					return new Status(IStatus.ERROR,
							MarketplaceClientUiPlugin.getInstance().getBundle().getSymbolicName(), ex.getMessage(), ex);
				}
				// this list requires some filtering: bug 492513
				final List<INode> nodes = new ArrayList<INode>();
				for (INode node : searchResult.getNodes()) {
					if (node.getTags() != null) {
						boolean hasTag = false;
						for (ITag tag : node.getTags().getTags()) {
							if (query.equalsIgnoreCase(tag.getName())) {
								hasTag = true;
								break;
							}
						}
						if (hasTag) {
							nodes.add(node);
						}
					}
				}
				if (nodes.isEmpty()) {
					return Status.OK_STATUS;
				}
				UIJob openDialog = new UIJob(Messages.AskMerketplaceForFileSupportStrategy_dialogJobName) {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						final Shell shell = WorkbenchUtil.getShell();
						final MarketplaceOrAssociateDialog dialog = new MarketplaceOrAssociateDialog(shell,
								fileExtension, res);
						if (dialog.open() == IDialogConstants.OK_ID) {
							if (dialog.isShowProposals()) {
								IMarketplaceClientService marketplaceClientService = MarketplaceClient
										.getMarketplaceClientService();
								IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
								marketplaceClientService.open(config, new LinkedHashSet<INode>(nodes));
							} else if (dialog.isAssociateToExtension()) {
								List<String> extensions = new ArrayList<String>(1);
								extensions.add(fileExtension);
								// need internal API:
								// * https://bugs.eclipse.org/bugs/show_bug.cgi?id=110602
								// * https://www.eclipse.org/forums/index.php/t/98199/
								FileEditorMapping[] mappings = new FileEditorMapping[editorRegistry
								                                                     .getFileEditorMappings().length + 1];
								System.arraycopy(editorRegistry.getFileEditorMappings(), 0, mappings, 0,
										mappings.length - 1);
								FileEditorMapping newMapping = null;
								if (fileName.equals(fileExtension)) {
									newMapping = new FileEditorMapping(fileName, null);
								} else {
									newMapping = new FileEditorMapping(fileExtension);
								}
								newMapping.setDefaultEditor(res);
								mappings[mappings.length - 1] = newMapping;
								((EditorRegistry) editorRegistry).setFileEditorMappings(mappings);
							}
							return Status.OK_STATUS;
						} else {
							return Status.CANCEL_STATUS;
						}
					}

					@Override
					public Display getDisplay() {
						if (display != null && !display.isDisposed()) {
							return display;
						}
						return super.getDisplay();
					}
				};
				openDialog.setPriority(Job.INTERACTIVE);
				openDialog.setSystem(true);
				openDialog.schedule();
				return Status.OK_STATUS;
			}
		};
		mpcJob.setPriority(Job.INTERACTIVE);
		mpcJob.setUser(false);
		mpcJob.schedule();

		return res;
	}

}
