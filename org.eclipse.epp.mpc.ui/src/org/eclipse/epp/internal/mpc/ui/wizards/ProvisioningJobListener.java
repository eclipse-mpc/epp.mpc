/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.net.URL;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.util.ConcurrentTaskManager;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * A job listener that produces notifications of a successful install.
 *
 * @author David Green
 */
class ProvisioningJobListener extends JobChangeAdapter {
	private final Set<CatalogItem> installItems;

	public ProvisioningJobListener(Set<CatalogItem> installItems) {
		this.installItems = installItems;
	}

	@Override
	public void done(IJobChangeEvent event) {
		if (event.getResult().isOK()) {
			Job job = new Job(Messages.ProvisioningJobListener_notificationTaskName) {
				{
					setPriority(Job.LONG);
					setSystem(true);
					setUser(true);
				}

				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					ConcurrentTaskManager taskManager = new ConcurrentTaskManager(installItems.size(),
							Messages.ProvisioningJobListener_notificationTaskName);
					for (CatalogItem item : installItems) {
						if (item instanceof MarketplaceNodeCatalogItem) {
							final MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) item;

							taskManager.submit(new Runnable() {
								public void run() {
									INode node = nodeItem.getData();
									URL marketplaceUrl = nodeItem.getMarketplaceUrl();
									IMarketplaceService marketplaceService = ServiceHelper.getMarketplaceServiceLocator().getMarketplaceService(marketplaceUrl.toString());
									marketplaceService.reportInstallSuccess(node, new NullProgressMonitor() {
										@Override
										public boolean isCanceled() {
											return monitor.isCanceled();
										}
									});
								}
							});
						}
					}
					try {
						taskManager.waitUntilFinished(monitor);
					} catch (CoreException e) {
						return e.getStatus();
					}
					return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
				}

			};
			job.schedule();
		}
	}
}