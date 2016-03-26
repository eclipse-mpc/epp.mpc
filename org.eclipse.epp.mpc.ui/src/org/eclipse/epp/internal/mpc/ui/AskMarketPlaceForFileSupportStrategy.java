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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.EditorSelectionDialog;
import org.eclipse.ui.ide.IUnassociatedEditorStrategy;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;

/**
 * For a given file, search entries on marketplace that would match the search "fileExtension_${extension}". MarketPlace
 * entry can declare support for some extension by adding these terms as tags.
 *
 * @author mistria
 */
public class AskMarketPlaceForFileSupportStrategy implements IUnassociatedEditorStrategy {

	private final class ProvisioningJobChangeListener implements IJobChangeListener {

		private final Set<Job> provisioningJobs = new HashSet<Job>();

		public void sleeping(IJobChangeEvent event) {
			// ignore
		}

		public void scheduled(IJobChangeEvent event) {
			if (event.getJob() instanceof ProvisioningJob) {
				provisioningJobs.add(event.getJob());
			}
		}

		public void running(IJobChangeEvent event) {
			if (event.getJob() instanceof ProvisioningJob) {
				provisioningJobs.add(event.getJob());
			}
		}

		public void done(IJobChangeEvent event) {
			if (event.getJob() instanceof ProvisioningJob) {
				provisioningJobs.remove(event.getJob());
			}
		}

		public void awake(IJobChangeEvent event) {
			// ignore
		}

		public void aboutToRun(IJobChangeEvent event) {
			if (event.getJob() instanceof ProvisioningJob) {
				provisioningJobs.add(event.getJob());
			}
		}

		public boolean hasProvisioningJobScheduledOrRunning() {
			return !this.provisioningJobs.isEmpty();
		}
	}

	public AskMarketPlaceForFileSupportStrategy() {
	}

	public IEditorDescriptor getEditorDescriptor(String fileName, IEditorRegistry editorRegistry)
			throws CoreException, OperationCanceledException {
		IMarketplaceClientService marketplaceClientService = MarketplaceClient.getMarketplaceClientService();
		IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
		String[] split = fileName.split("\\."); //$NON-NLS-1$
		String query = "fileExtension_" + split[split.length - 1]; //$NON-NLS-1$]
		ProvisioningJobChangeListener provisioningJobMonitor = new ProvisioningJobChangeListener();
		Job.getJobManager().addJobChangeListener(provisioningJobMonitor);
		marketplaceClientService.openSearch(config, null, null, query);
		// wait for installation to complete
		while (provisioningJobMonitor.hasProvisioningJobScheduledOrRunning()) {
			Display.getCurrent().readAndDispatch();
		}
		// after operation, a restart is prompted, what's following is then only relevant
		// if user has canceled the installation.

		// just after openSearch, we cannot immediately resolve to an editor. This require a restart.
		// Assuming necessary extension points are dynamic and registry is automatically updated, we could:
		// 1. wait for install operation completed
		// 2. apply change to update editor bundles and extension registry
		// 3. ask registry about best editor
		// return editorRegistry.getDefaultEditor(fileName);

		// case of "Cancel": ask user
		EditorSelectionDialog dialog = new EditorSelectionDialog(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		dialog.setFileName(fileName);
		dialog.setBlockOnOpen(true);
		if (IDialogConstants.CANCEL_ID == dialog.open()) {
			throw new OperationCanceledException(IDEWorkbenchMessages.IDE_noFileEditorSelectedUserCanceled);
		}
		return dialog.getSelectedEditor();
	}

}
