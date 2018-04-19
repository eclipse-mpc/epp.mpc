/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IStartup;

public class MissingNatureDetector implements IStartup, IPropertyChangeListener {

	public static final String ENABLEMENT_PROPERTY = "org.eclipse.epp.mpc.naturelookup"; //$NON-NLS-1$

	private JobGroup allJobs;

	private final Set<String> detectedNatures = new HashSet<>();

	private final Set<DiscoverNatureSupportJob> lookupJobs = new HashSet<>();

	private final IResourceChangeListener projectOpenListener = event -> {
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
	};

	public MissingNatureDetector() {
		super();
	}

	private void triggerNatureLookup(final String natureId) {
		synchronized (lookupJobs) {
			if (detectedNatures.contains(natureId)) {
				return;
			} else {
				DiscoverNatureSupportJob mpcJob = new DiscoverNatureSupportJob(natureId);
				mpcJob.setSystem(false);
				mpcJob.setUser(false);
				mpcJob.setJobGroup(allJobs);
				mpcJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						showProposalsIfReady();
					}
				});
				lookupJobs.add(mpcJob);
				//schedule() needs to happen inside synchronized(...).
				//Otherwise it's not guaranteed that allJobs.getActiveJobs() will consider it,
				//and we might end up with processing unfinished jobs in showProposalsIfReady()
				mpcJob.schedule();
			}
		}
	}

	private void showProposalsIfReady() {
		Map<String, Collection<INode>> candidates;
		synchronized (lookupJobs) {
			if (!allJobs.getActiveJobs().isEmpty()) {
				return;
			}
			candidates = new HashMap<>();
			for (DiscoverNatureSupportJob lookupJob : lookupJobs) {
				Collection<INode> entryCandidates = lookupJob.getCandidates();
				if (entryCandidates != null && !entryCandidates.isEmpty()) {
					candidates.put(lookupJob.getNatureId(), entryCandidates);
				}
			}
			lookupJobs.clear();
		}
		if (!candidates.isEmpty()) {
			new ShowNatureProposalsJob(candidates).schedule();
		}
	}

	@Override
	public void earlyStartup() {
		allJobs = new JobGroup(Messages.MissingNatureDetector_Title, 3, 0);
		IPreferenceStore preferenceStore = MarketplaceClientUiPlugin.getInstance().getPreferenceStore();
		preferenceStore.addPropertyChangeListener(this);
		boolean preferenceValue = preferenceStore.getBoolean(ENABLEMENT_PROPERTY);
		if (preferenceValue) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(projectOpenListener);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (ENABLEMENT_PROPERTY.equals(event.getProperty())) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			boolean enabled;
			if (event.getNewValue() instanceof String) {
				enabled = Boolean.parseBoolean((String) event.getNewValue());
			} else {
				enabled = Boolean.TRUE.equals(event.getNewValue());
			}
			if (enabled) {
				workspace.addResourceChangeListener(this.projectOpenListener);
			} else {
				workspace.removeResourceChangeListener(this.projectOpenListener);
			}
		}
	}

}
