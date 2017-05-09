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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

final class ShowNatureProposalsJob extends UIJob {
	private final Map<String, Collection<INode>> candidates;

	ShowNatureProposalsJob(Map<String, Collection<INode>> candidates) {
		super(PlatformUI.getWorkbench().getDisplay(), Messages.MissingNatureDetector_Desc);
		this.candidates = candidates;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		TitleAreaDialog dialog = new ShowNatureProposalsDialog(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), candidates);
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return Status.CANCEL_STATUS;
		}
		IMarketplaceClientService marketplaceClientService = MarketplaceClient.getMarketplaceClientService();
		IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
		Set<INode> allNodes = new HashSet<INode>();
		for (Collection<INode> candidateNodes : candidates.values()) {
			allNodes.addAll(candidateNodes);
		}
		marketplaceClientService.open(config, allNodes);
		return Status.OK_STATUS;
	}
}