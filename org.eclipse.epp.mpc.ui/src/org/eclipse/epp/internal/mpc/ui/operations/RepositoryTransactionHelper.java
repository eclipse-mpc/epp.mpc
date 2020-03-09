/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.operations;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

public class RepositoryTransactionHelper implements AutoCloseable {

	private final RepositoryTracker metadataRepositoryTracker;

	private final RepositoryTracker artifactRepositoryTracker;

	public RepositoryTransactionHelper(IMetadataRepositoryManager metadataRepositoryManager,
			IArtifactRepositoryManager artifactRepositoryManager) {
		metadataRepositoryTracker = metadataRepositoryManager == null ? null
				: new RepositoryTracker(metadataRepositoryManager);
		artifactRepositoryTracker = artifactRepositoryManager == null ? null
				: new RepositoryTracker(artifactRepositoryManager);
	}

	public void init() {
		if (metadataRepositoryTracker != null) {
			metadataRepositoryTracker.init();
		}
		if (artifactRepositoryTracker != null) {
			artifactRepositoryTracker.init();
		}
	}

	public void addRepository(URI uri) {
		//add artifact repo first, because if anything goes wrong it's better
		//to have artifact without meta than the other way around
		if (artifactRepositoryTracker != null) {
			artifactRepositoryTracker.addRepository(uri);
		}
		if (metadataRepositoryTracker != null) {
			metadataRepositoryTracker.addRepository(uri);
		}
	}

	public void resetRepository(URI uri) {
		if (metadataRepositoryTracker != null) {
			metadataRepositoryTracker.resetRepository(uri);
		}
		if (artifactRepositoryTracker != null) {
			artifactRepositoryTracker.resetRepository(uri);
		}
	}

	public void resetAll() {
		if (metadataRepositoryTracker != null) {
			metadataRepositoryTracker.resetAll();
		}
		if (artifactRepositoryTracker != null) {
			artifactRepositoryTracker.resetAll();
		}
	}

	@Override
	public void close() {
		resetAll();
	}

	private enum CleanupAction {
		REMOVE, DISABLE;
	}

	private static class RepositoryTracker implements AutoCloseable {
		private static final int ENABLED_STATE_FLAGS = IRepositoryManager.REPOSITORIES_NON_LOCAL
				| IRepositoryManager.REPOSITORIES_NON_SYSTEM;

		private static final int DISABLED_STATE_FLAGS = IRepositoryManager.REPOSITORIES_DISABLED | ENABLED_STATE_FLAGS;

		final IRepositoryManager<?> manager;

		final Map<URI, CleanupAction> cleanupActions;

		final Map<URI, Integer> initialState;

		RepositoryTracker(IRepositoryManager<?> manager) {
			this.manager = manager;
			this.initialState = new HashMap<>();
			this.cleanupActions = new LinkedHashMap<>();
			init();
		}

		public void init() {
			if (!cleanupActions.isEmpty()) {
				throw new IllegalStateException();
			}
			initialState.clear();

			/* TODO
			 *
			 * A lot of side effects are happening when repos are loaded - especially composites:
			 * - child repos get added, sometimes as system=true/enabled=false, or sometimes as system=false/enabled=false
			 * - repos might get enabled
			 * - repos might get converted from system=true to system=false, also changing the type from
			 *   LocalMetadataRepository/simpleRepository to something else
			 *
			 * All of this makes it rather hopeless to try and revert the repo state completely...
			 * (For example, at the time of this writing, loading http://download.eclipse.org/mylyn/releases/latest
			 * also creates http://download.eclipse.org/mylyn/releases/3.25 and leaves it behind as system=false/enabled=false,
			 * adding it as a disabled repo to the visible repo list. It is hard to gauge if having those around in meta but not in
			 * artifact would be safe - see bug 560062 - so it's probably best to always fully load both repos)
			 *
			 * That's why we only track some of the known repos and only revert what we explicitly did.
			 *
			 * We could possibly add a listener for RepositoryEvent.ADDED and relatives, but we might get false positives if
			 * user does other repo things in parallel...
			 */
			URI[] enabledRemoteRepos = manager.getKnownRepositories(ENABLED_STATE_FLAGS);
			URI[] disabledRemoteRepos = manager.getKnownRepositories(DISABLED_STATE_FLAGS);

			for (URI uri : enabledRemoteRepos) {
				initialState.put(uri, ENABLED_STATE_FLAGS);
			}
			for (URI uri : disabledRemoteRepos) {
				initialState.put(uri, DISABLED_STATE_FLAGS);
			}
		}

		public void addRepository(URI uri) {
			manager.addRepository(uri);
			mergeAction(uri, getCleanupAction(uri));
		}

		private void mergeAction(URI uri, CleanupAction cleanupAction) {
			if (cleanupAction != null) {
				cleanupActions.put(uri, cleanupAction);
			}
		}

		private CleanupAction getCleanupAction(URI uri) {
			Integer state = initialState.get(uri);
			if (state == null) {
				//did not exist initially
				return CleanupAction.REMOVE;
			}
			if ((state & IRepositoryManager.REPOSITORIES_DISABLED) != 0) {
				//initially disabled
				return CleanupAction.DISABLE;
			}
			return null;
		}

		public void resetRepository(URI uri) {
			CleanupAction action = cleanupActions.remove(uri);
			doResetRepository(uri, action);
		}

		private void doResetRepository(URI uri, CleanupAction action) {
			if (action == null) {
				return;
			}
			switch (action) {
			case REMOVE:
				manager.removeRepository(uri);
				break;
			case DISABLE:
				manager.setEnabled(uri, false);
				break;
			default:
				throw new UnsupportedOperationException(action.name());
			}
		}

		public void resetAll() {
			for (Entry<URI, CleanupAction> entry : cleanupActions.entrySet()) {
				doResetRepository(entry.getKey(), entry.getValue());
			}
			cleanupActions.clear();
		}

		@Override
		public void close() {
			resetAll();
		}
	}
}
