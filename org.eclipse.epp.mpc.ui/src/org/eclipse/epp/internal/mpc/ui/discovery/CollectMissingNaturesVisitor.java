/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

final class CollectMissingNaturesVisitor implements IResourceDeltaVisitor {
	private final Set<String> missingNatures = new HashSet<>();

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta.getResource().getType() == IResource.ROOT || delta.getResource().getType() == IResource.PROJECT) {
			return delta.getResource().isAccessible();
		}
		if (delta.getResource().getType() == IResource.FILE
				&& IProjectDescription.DESCRIPTION_FILE_NAME
				.equals(delta.getResource().getName())) {
			if (delta.getKind() == IResourceDelta.ADDED
					|| delta.getKind() == IResourceDelta.CHANGED) {
				IProject project = delta.getResource().getProject();
				if (!project.isAccessible()) {
					return false;
				}
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