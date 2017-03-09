/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class MissingNatureDetector implements IStartup {

	private static final class CollectMissingNaturesVisitor implements IResourceDeltaVisitor {
		private final Set<String> missingNatures = new HashSet<String>();

		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta.getResource().getType() == IResource.PROJECT
					|| delta.getResource().getType() == IResource.ROOT) {
				return true;
			}
			if (delta.getResource().getType() == IResource.FILE
					&& IProjectDescription.DESCRIPTION_FILE_NAME
					.equals(delta.getResource().getName())) {
				if (delta.getKind() == IResourceDelta.ADDED
						|| delta.getKind() == IResourceDelta.CHANGED) {
					IProject project = delta.getResource().getProject();
					for (String natureId : project.getDescription().getNatureIds()) {
						if (project.getWorkspace().getNatureDescriptor(natureId) == null) {
							this.missingNatures.add(natureId);
						}
					}
				}
			}
			return false;
		}

		public Set<String> getMissingNatures() {
			return this.missingNatures;
		}
	}

	private static final class LookupByNatureJob extends Job {
		private final String natureId;

		private List<? extends INode> nodes;

		private LookupByNatureJob(String name, String natureId) {
			super(name);
			this.natureId = natureId;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			BundleContext bundleContext = MarketplaceClientUiPlugin.getBundleContext();
			ServiceReference<IMarketplaceServiceLocator> locatorReference = bundleContext
					.getServiceReference(IMarketplaceServiceLocator.class);
			IMarketplaceServiceLocator locator = bundleContext.getService(locatorReference);
			IMarketplaceService marketplaceService = locator.getDefaultMarketplaceService();
			String fileExtensionTag = "nature_" + natureId; //$NON-NLS-1$]
			try {
				ISearchResult searchResult = marketplaceService.tagged(fileExtensionTag, monitor);
				nodes = searchResult.getNodes();
			} catch (CoreException ex) {
				return new Status(IStatus.ERROR,
						MarketplaceClientUiPlugin.getInstance().getBundle().getSymbolicName(), ex.getMessage(), ex);
			}
			return Status.OK_STATUS;
		}

		public Collection<INode> getCandidates() {
			return (List<INode>) this.nodes;
		}
	}

	private final JobGroup allJobs = new JobGroup(Messages.MissingNatureDetector_Title, 3, 0);

	private final Map<String, LookupByNatureJob> lookupJobs = new HashMap<String, LookupByNatureJob>();

	private final UIJob showCandidatesJob = new UIJob(PlatformUI.getWorkbench().getDisplay(),
			Messages.MissingNatureDetector_Desc) {

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			TitleAreaDialog dialog = new TitleAreaDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()) {
				private Image wizban;

				@Override
				protected void configureShell(Shell newShell) {
					super.configureShell(newShell);
					newShell.setText(Messages.MissingNatureDetector_Title);
				}

				@Override
				public Control createDialogArea(Composite parent) {
					setTitle(Messages.MissingNatureDetector_Title);
					setMessage(Messages.MissingNatureDetector_Desc);
					wizban = DiscoveryImages.BANNER_DISOVERY.createImage();
					setTitleImage(wizban);
					Composite res = new Composite(parent, SWT.NONE);
					GridDataFactory.fillDefaults()
					.grab(true, true)
					.hint(SWT.DEFAULT, SWT.DEFAULT)
					.applyTo(res);
					GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).equalWidth(false).applyTo(
							res);
					Label label = new Label(res, SWT.WRAP);
					label.setText(Messages.MissingNatureDetector_Message);
					return res;
				}

				@Override
				protected void createButtonsForButtonBar(Composite parent) {
					super.createButtonsForButtonBar(parent);
					getButton(IDialogConstants.OK_ID).setText(Messages.MissingNatureDetector_ShowSolutions);
				}


				@Override
				public boolean close() {
					if (super.close()) {
						wizban.dispose();
					}
					return false;
				}
			};
			if (dialog.open() == IDialogConstants.CANCEL_ID) {
				return Status.CANCEL_STATUS;
			}
			IMarketplaceClientService marketplaceClientService = MarketplaceClient.getMarketplaceClientService();
			IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
			Set<INode> allNodes = new HashSet<INode>();
			for (LookupByNatureJob job : lookupJobs.values()) {
				allNodes.addAll(job.getCandidates());
			}
			marketplaceClientService.open(config, allNodes);
			return Status.OK_STATUS;
		}
	};

	public void earlyStartup() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getDelta() == null) {
					return;
				}
				try {
					CollectMissingNaturesVisitor visitor = new CollectMissingNaturesVisitor();
					event.getDelta().accept(visitor);
					for (String natureId : visitor.getMissingNatures()) {
						triggerNatureLookup(natureId);
					}
					if (!visitor.getMissingNatures().isEmpty()) {
						showProposalsIfReady();
					}
				} catch (CoreException e) {
					MarketplaceClientUiPlugin.getInstance()
					.getLog()
					.log(new Status(IStatus.ERROR,
							MarketplaceClientUiPlugin.getInstance().getBundle().getSymbolicName(),
							e.getLocalizedMessage(), e));
				}
			}

		});
	}

	private void triggerNatureLookup(final String natureId) {
		LookupByNatureJob mpcJob = null;
		synchronized (lookupJobs) {
			if (lookupJobs.containsKey(natureId)) {
				return;
			} else {
				mpcJob = new LookupByNatureJob(Messages.AskMarketPlaceForFileSupportStrategy_jobName, natureId);
				lookupJobs.put(natureId, mpcJob);
			}
		}
		mpcJob.setSystem(false);
		mpcJob.setUser(false);
		mpcJob.setJobGroup(allJobs);
		mpcJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				showProposalsIfReady();
			}
		});
		lookupJobs.put(natureId, mpcJob);
		mpcJob.schedule();
	}

	protected boolean hasCandidates() {
		for (LookupByNatureJob job : this.lookupJobs.values()) {
			if (!job.getCandidates().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private void showProposalsIfReady() {
		if (allJobs.getActiveJobs().isEmpty() && hasCandidates() && showCandidatesJob.getState() == Job.NONE) {
			showCandidatesJob.schedule();
		}
	}
}
