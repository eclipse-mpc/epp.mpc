/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ide.IUnassociatedEditorStrategy;
import org.eclipse.ui.internal.ide.registry.SystemEditorOrTextEditorStrategy;

/**
 * For a given file, search entries on marketplace that would match the search "fileExtension_${extension}". MarketPlace
 * entry can declare support for some extension by adding these terms as tags.
 *
 * @author mistria
 */
public class AskMarketplaceForFileSupportStrategy implements IUnassociatedEditorStrategy {


	public AskMarketplaceForFileSupportStrategy() {
	}

	@Override
	public IEditorDescriptor getEditorDescriptor(final String fileName, final IEditorRegistry editorRegistry)
			throws CoreException, OperationCanceledException {
		final IEditorDescriptor defaultDescriptor = createDefaultDescriptor(fileName, editorRegistry);

		Job mpcJob = new DiscoverFileSupportJob(editorRegistry, defaultDescriptor, fileName);
		mpcJob.setPriority(Job.INTERACTIVE);
		mpcJob.setUser(false);
		mpcJob.schedule();

		return defaultDescriptor;
	}

	@SuppressWarnings("restriction")
	private IEditorDescriptor createDefaultDescriptor(final String fileName, final IEditorRegistry editorRegistry) {
		SystemEditorOrTextEditorStrategy editorStrategy = new SystemEditorOrTextEditorStrategy();
		return editorStrategy.getEditorDescriptor(fileName,
				editorRegistry);
	}

}
