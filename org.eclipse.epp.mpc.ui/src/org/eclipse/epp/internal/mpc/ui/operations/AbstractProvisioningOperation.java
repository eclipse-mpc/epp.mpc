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
package org.eclipse.epp.internal.mpc.ui.operations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableWithProgress;

public abstract class AbstractProvisioningOperation implements IRunnableWithProgress {

	protected static final String P2_FEATURE_GROUP_SUFFIX = ".feature.group"; //$NON-NLS-1$

	protected final List<CatalogItem> items;

	protected final ProvisioningUI provisioningUI;

	protected Set<URI> repositoryLocations;

	protected Set<URI> addedRepositoryLocations;

	protected AbstractProvisioningOperation(Collection<CatalogItem> installableConnectors) {
		if (installableConnectors == null || installableConnectors.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.items = new ArrayList<CatalogItem>(installableConnectors);
		this.provisioningUI = ProvisioningUI.getDefaultUI();
	}

	protected List<IMetadataRepository> addRepositories(SubMonitor monitor) throws
			URISyntaxException, ProvisionException {
		// tell p2 that it's okay to use these repositories
		ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
		RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
		repositoryLocations = new HashSet<URI>();
		if (addedRepositoryLocations == null) {
			addedRepositoryLocations = new HashSet<URI>();
		}

		Set<URI> knownRepositories = new HashSet<URI>(Arrays.asList(repositoryTracker.getKnownRepositories(session)));

		monitor.setWorkRemaining(items.size() * 5);
		for (CatalogItem descriptor : items) {
			URI uri = URLUtil.toURI(descriptor.getSiteUrl());
			if (repositoryLocations.add(uri) && !knownRepositories.contains(uri)) {
				checkCancelled(monitor);
				repositoryTracker.addRepository(uri, null, session);
				addedRepositoryLocations.add(uri);
			}
			monitor.worked(1);
		}

		// fetch meta-data for these repositories
		ArrayList<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
		monitor.setWorkRemaining(repositories.size());
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) session.getProvisioningAgent().getService(
				IMetadataRepositoryManager.SERVICE_NAME);
		for (URI uri : repositoryLocations) {
			checkCancelled(monitor);
			IMetadataRepository repository = manager.loadRepository(uri, monitor.newChild(1));
			repositories.add(repository);
		}
		return repositories;
	}

	/**
	 * Perform a query to get the installable units. This causes p2 to determine what features are available in each
	 * repository. We select installable units by matching both the feature id and the repository; it is possible though
	 * unlikely that the same feature id is available from more than one of the selected repositories, and we must
	 * ensure that the user gets the one that they asked for.
	 */
	protected List<IInstallableUnit> queryInstallableUnits(SubMonitor monitor, List<IMetadataRepository> repositories)
			throws URISyntaxException {
		final List<IInstallableUnit> installableUnits = new ArrayList<IInstallableUnit>();

		monitor.setWorkRemaining(repositories.size());
		for (final IMetadataRepository repository : repositories) {
			checkCancelled(monitor);
			final Set<String> installableUnitIdsThisRepository = getDescriptorIds(repository);

			IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(QueryUtil.createIUGroupQuery());
			IQueryResult<IInstallableUnit> result = repository.query(query, monitor.newChild(1));

			for (IInstallableUnit iu : result) {
				String id = iu.getId();
				if (installableUnitIdsThisRepository.contains(id)) {
					installableUnits.add(iu);
				}
			}
		}
		return installableUnits;
	}

	private Set<String> getDescriptorIds(final IMetadataRepository repository) throws URISyntaxException {
		final Set<String> installableUnitIdsThisRepository = new HashSet<String>();
		// determine all installable units for this repository
		for (CatalogItem descriptor : items) {
			if (repository.getLocation().equals(URLUtil.toURI(descriptor.getSiteUrl()))) {
				installableUnitIdsThisRepository.addAll(getFeatureIds(descriptor));
			}
		}
		return installableUnitIdsThisRepository;
	}

	protected Set<String> getFeatureIds(CatalogItem descriptor) {
		Set<String> featureIds = new HashSet<String>();
		for (String id : descriptor.getInstallableUnits()) {
			if (!id.endsWith(P2_FEATURE_GROUP_SUFFIX)) {
				id += P2_FEATURE_GROUP_SUFFIX;
			}
			featureIds.add(id);
		}
		return featureIds;
	}

	protected void checkCancelled(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	public Set<URI> getAddedRepositoryLocations() {
		return addedRepositoryLocations;
	}

	protected void removeAddedRepositoryLocations() {
		if (addedRepositoryLocations != null) {
			removeRepositoryLocations(addedRepositoryLocations);
			addedRepositoryLocations = null;
		}
	}

	/**
	 * remove the given repository locations from the repository tracker.
	 * 
	 * @param repositoryLocations
	 */
	public static void removeRepositoryLocations(Set<URI> repositoryLocations) {
		if (repositoryLocations == null || repositoryLocations.isEmpty()) {
			return;
		}
		ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
		RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
		repositoryTracker.removeRepositories(repositoryLocations.toArray(new URI[repositoryLocations.size()]), session);
	}
}
