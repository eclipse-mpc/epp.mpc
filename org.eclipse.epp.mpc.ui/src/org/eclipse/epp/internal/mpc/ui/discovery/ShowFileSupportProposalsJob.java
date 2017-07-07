/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Mickael Istria (Red Hat Inc.) - initial implementation
 *  Lucas Bullen (Red Hat Inc.) -	Bug 517818: retain same functionality post fix
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;
import org.eclipse.ui.progress.UIJob;

final class ShowFileSupportProposalsJob extends UIJob {

	private final Display display;

	private final IEditorRegistry editorRegistry;

	private final IEditorDescriptor defaultDescriptor;

	private final String fileName;

	private final List<? extends INode> nodes;

	ShowFileSupportProposalsJob(String fileName, List<? extends INode> nodes,
			IEditorRegistry editorRegistry, IEditorDescriptor defaultDescriptor, Display display) {
		super(Messages.AskMerketplaceForFileSupportStrategy_dialogJobName);
		this.fileName = fileName;
		this.nodes = nodes;
		this.editorRegistry = editorRegistry;
		this.defaultDescriptor = defaultDescriptor;
		this.display = display;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		final Shell shell = WorkbenchUtil.getShell();
		String fileExtensionLabel = DiscoverFileSupportJob.getFileExtensionLabel(fileName);
		final ShowFileSupportProposalsDialog dialog = new ShowFileSupportProposalsDialog(shell, fileExtensionLabel,
				defaultDescriptor);
		if (dialog.open() == IDialogConstants.OK_ID) {
			if (dialog.isShowProposals()) {
				IMarketplaceClientService marketplaceClientService = MarketplaceClient
						.getMarketplaceClientService();
				IMarketplaceClientConfiguration config = marketplaceClientService.newConfiguration();
				marketplaceClientService.open(config, new LinkedHashSet<INode>(nodes));
			} else if (dialog.isAssociateToExtension()) {
				List<String> fileExtensions = DiscoverFileSupportJob.getFileExtensions(fileName);
				IFileEditorMapping newMapping = createDefaultDescriptorMapping(
						fileExtensions.get(fileExtensions.size() - 1));
				addEditorMapping(newMapping);
			}
			return Status.OK_STATUS;
		} else {
			return Status.CANCEL_STATUS;
		}
	}

	// need internal API:
	// * https://bugs.eclipse.org/bugs/show_bug.cgi?id=110602
	// * https://www.eclipse.org/forums/index.php/t/98199/
	@SuppressWarnings("restriction")
	private void addEditorMapping(IFileEditorMapping newMapping) {
		FileEditorMapping[] mappings = new FileEditorMapping[editorRegistry.getFileEditorMappings().length + 1];
		System.arraycopy(editorRegistry.getFileEditorMappings(), 0, mappings, 0, mappings.length - 1);
		mappings[mappings.length - 1] = (FileEditorMapping) newMapping;
		((EditorRegistry) editorRegistry).setFileEditorMappings(mappings);
		((EditorRegistry) editorRegistry).saveAssociations();
	}

	// need internal API:
	// * https://bugs.eclipse.org/bugs/show_bug.cgi?id=110602
	// * https://www.eclipse.org/forums/index.php/t/98199/
	@SuppressWarnings("restriction")
	private IFileEditorMapping createDefaultDescriptorMapping(String fileExtension) {
		FileEditorMapping newMapping = null;
		if (fileName.equals(fileExtension)) {
			newMapping = new FileEditorMapping(fileName, null);
		} else {
			newMapping = new FileEditorMapping(fileExtension);
		}
		newMapping.setDefaultEditor(defaultDescriptor);
		return newMapping;
	}

	@Override
	public Display getDisplay() {
		if (display != null && !display.isDisposed()) {
			return display;
		}
		return super.getDisplay();
	}
}