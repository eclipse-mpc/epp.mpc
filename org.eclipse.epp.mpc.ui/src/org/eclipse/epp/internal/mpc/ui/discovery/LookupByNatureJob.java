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
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.Collection;
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
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

final class LookupByNatureJob extends Job {
	private final String natureId;

	private List<? extends INode> nodes;

	LookupByNatureJob(String natureId) {
		super(NLS.bind(Messages.MissingNatureDetector_jobName, natureId));
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
			IStatus status = new Status(IStatus.ERROR,
					MarketplaceClientUiPlugin.getInstance().getBundle().getSymbolicName(), ex.getMessage(), ex);
			MarketplaceClientUiPlugin.getInstance().getLog().log(status);
			// Do not return this status as it would show an error
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	public Collection<INode> getCandidates() {
		return (List<INode>) this.nodes;
	}

	public String getNatureId() {
		return natureId;
	}
}