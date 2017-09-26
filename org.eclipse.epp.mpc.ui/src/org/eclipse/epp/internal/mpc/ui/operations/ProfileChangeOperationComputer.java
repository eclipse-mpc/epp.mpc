/*******************************************************************************
 * Copyright (c) 2009, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.operations;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.FeatureEntry;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 * A job that configures a p2 provisioning operation for installing/updating/removing one or more {@link CatalogItem
 * connectors}. The bulk of the installation work is done by p2; this class just sets up the p2 repository meta-data and
 * selects the appropriate features to install.
 *
 * @author David Green
 * @author Steffen Pingel
 */
public class ProfileChangeOperationComputer extends AbstractProvisioningOperation {

	public enum ResolutionStrategy {
		ALL_REPOSITORIES, SELECTED_REPOSITORIES, FALLBACK_STRATEGY
	}

	public enum OperationType {
		/**
		 * Install and update features
		 */
		INSTALL,
		/**
		 * update only, use {@link #INSTALL} if features are to be installed and updated in a single operation
		 */
		UPDATE,
		/**
		 * uninstall features
		 */
		UNINSTALL,
		/**
		 * Change an installation - this might involve any of the above operations
		 */
		CHANGE;

		public static OperationType map(org.eclipse.epp.mpc.ui.Operation operation) {
			if (operation == null) {
				return null;
			}
			switch (operation) {
			case INSTALL:
				return INSTALL;
			case UNINSTALL:
				return UNINSTALL;
			case UPDATE:
				return UPDATE;
			case CHANGE:
				return CHANGE;
			case NONE:
				return null;
			default:
				throw new IllegalArgumentException(NLS.bind(Messages.ProfileChangeOperationComputer_unknownOperation,
						operation));
			}
		}
	}

	private final OperationType operationType;

	private final List<FeatureEntry> featureEntries;

	private ProfileChangeOperation operation;

	private IInstallableUnit[] ius;

	private final ResolutionStrategy resolutionStrategy;

	private final URI dependenciesRepository;

	private final boolean withRemediation;

	private String errorMessage;

	/**
	 * @param operationType
	 *            the type of operation to perform
	 * @param items
	 *            the items for which features are being installed
	 * @param featureEntries
	 *            the features to install/update/uninstall, which must correspond to features provided by the given
	 *            items
	 * @param dependenciesRepository
	 *            an optional URI to a repository from which dependencies may be installed, may be null
	 */
	public ProfileChangeOperationComputer(OperationType operationType, Collection<CatalogItem> items, Collection<FeatureEntry> featureEntries,
			URI dependenciesRepository, ResolutionStrategy resolutionStrategy, boolean withRemediation) {
		super(items);
		if (featureEntries == null || featureEntries.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (operationType == null) {
			throw new IllegalArgumentException();
		}
		if (resolutionStrategy == null) {
			throw new IllegalArgumentException();
		}
		this.featureEntries = new ArrayList<FeatureEntry>(featureEntries);
		this.operationType = operationType;
		this.resolutionStrategy = resolutionStrategy;
		this.dependenciesRepository = dependenciesRepository;
		this.withRemediation = withRemediation;
	}

	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
		try {
			boolean hasInstall = hasInstall();
			boolean hasUninstall = hasUninstall();
			SubMonitor monitor = SubMonitor.convert(progressMonitor,
					Messages.ProvisioningOperation_configuringProvisioningOperation, 1000 + (hasInstall ? 500 : 0)
					+ (hasUninstall ? 100 : 0));
			try {
				IInstallableUnit[] uninstallIUs = null;
				if (hasInstall) {
					ius = computeInstallableUnits(monitor.newChild(500));
				} else {
					ius = new IInstallableUnit[0];
				}
				if (hasUninstall) {
					uninstallIUs = computeUninstallUnits(monitor.newChild(100));
				}
				checkCancelled(monitor);

				URI[] repositories = repositoryLocations == null ? new URI[0] : repositoryLocations.toArray(new URI[0]);
				switch (operationType) {
				case INSTALL:
					operation = resolveInstall(monitor.newChild(500), ius, repositories);
					break;
				case UPDATE:
					operation = resolveUpdate(monitor.newChild(500), computeInstalledIus(ius), repositories);
					break;
				case UNINSTALL:
					operation = resolveUninstall(monitor.newChild(500), uninstallIUs, repositories);
					break;
				case CHANGE:
					operation = resolveChange(monitor.newChild(500), ius, uninstallIUs, repositories);
					break;
				default:
					throw new UnsupportedOperationException(operationType.name());
				}
				if (withRemediation && operation != null && operation.getResolutionResult().getSeverity() == IStatus.ERROR) {
					RemediationOperation remediationOperation = resolveRemediation(monitor.newChild(500), operation,
							repositories);
					if (remediationOperation.getResolutionResult() == Status.OK_STATUS) {
						errorMessage = operation.getResolutionDetails();
						operation = remediationOperation;
					}
				}
				checkCancelled(monitor);
			} finally {
				monitor.done();
			}
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	private boolean hasInstall() {
		switch (operationType) {
		case INSTALL:
		case UPDATE:
			return true;
		case UNINSTALL:
			return false;
		case CHANGE:
			for (FeatureEntry entry : featureEntries) {
				Operation operation = entry.computeChangeOperation();
				if (operation == Operation.INSTALL || operation == Operation.UPDATE) {
					return true;
				}
			}
			//$fall-through$
		default:
			return false;
		}
	}

	private boolean hasUninstall() {
		switch (operationType) {
		case INSTALL:
		case UPDATE:
			return false;
		case UNINSTALL:
			return true;
		case CHANGE:
			for (FeatureEntry entry : featureEntries) {
				if (entry.computeChangeOperation() == Operation.UNINSTALL) {
					return true;
				}
			}
			//$fall-through$
		default:
			return false;
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	private IInstallableUnit[] computeInstalledIus(IInstallableUnit[] ius) {
		List<IInstallableUnit> installedIus = new ArrayList<IInstallableUnit>(ius.length);
		Map<String, IInstallableUnit> iUsById = MarketplaceClientUi.computeInstalledIUsById(new NullProgressMonitor());

		for (IInstallableUnit iu : ius) {
			IInstallableUnit installedIu = iUsById.get(iu.getId());
			installedIus.add(installedIu);
		}
		return installedIus.toArray(new IInstallableUnit[installedIus.size()]);
	}

	public ProfileChangeOperation getOperation() {
		return operation;
	}

	public IInstallableUnit[] getIus() {
		return ius;
	}

	private interface ProfileChangeOperationFactory {
		ProfileChangeOperation create(List<IInstallableUnit> ius) throws CoreException;
	}

	private ProfileChangeOperation resolveInstall(IProgressMonitor monitor, final IInstallableUnit[] ius,
			URI[] repositories) throws CoreException {
		return resolve(monitor, new ProfileChangeOperationFactory() {
			public ProfileChangeOperation create(List<IInstallableUnit> ius) throws CoreException {
				return provisioningUI.getInstallOperation(ius, null);
			}
		}, this.resolutionStrategy, ius, repositories);
	}

	private ProfileChangeOperation resolveUninstall(IProgressMonitor monitor, final IInstallableUnit[] ius,
			URI[] repositories) throws CoreException {
		return resolve(monitor, new ProfileChangeOperationFactory() {
			public ProfileChangeOperation create(List<IInstallableUnit> ius) throws CoreException {
				return provisioningUI.getUninstallOperation(ius, null);
			}
		}, this.resolutionStrategy, ius, repositories);
	}

	private ProfileChangeOperation resolveUpdate(IProgressMonitor monitor, final IInstallableUnit[] ius,
			URI[] repositories) throws CoreException {
		return resolve(monitor, new ProfileChangeOperationFactory() {
			public ProfileChangeOperation create(List<IInstallableUnit> ius) throws CoreException {
				return provisioningUI.getUpdateOperation(ius, null);
			}
		}, this.resolutionStrategy, ius, repositories);
	}

	private ProfileChangeOperation resolveChange(IProgressMonitor monitor, IInstallableUnit[] ius,
			final IInstallableUnit[] uninstallIUs, URI[] repositories) throws CoreException {
		return resolve(monitor, new ProfileChangeOperationFactory() {
			public ProfileChangeOperation create(List<IInstallableUnit> ius) throws CoreException {
				InstallOperation installOperation = provisioningUI.getInstallOperation(ius, null);
				UninstallOperation uninstallOperation = provisioningUI.getUninstallOperation(
						Arrays.asList(uninstallIUs), null);
				CompositeProfileChangeOperation operation = new CompositeProfileChangeOperation(
						provisioningUI.getSession());
				operation.setProfileId(provisioningUI.getProfileId());

				ProvisioningContext provisioningContext = installOperation.getProvisioningContext();
				operation.setProvisioningContext(provisioningContext);
				uninstallOperation.setProvisioningContext(provisioningContext);

				operation.add(uninstallOperation).add(installOperation);
				return operation;
			}
		}, this.resolutionStrategy, ius, repositories);
	}

	private RemediationOperation resolveRemediation(IProgressMonitor monitor, final ProfileChangeOperation operation,
			URI[] repositories) throws CoreException {
		return (RemediationOperation) resolve(monitor, new ProfileChangeOperationFactory() {
			public RemediationOperation create(List<IInstallableUnit> ius) throws CoreException {
				return new RemediationOperation(ProvisioningUI.getDefaultUI().getSession(),
						operation.getProfileChangeRequest());
			}
		}, ResolutionStrategy.ALL_REPOSITORIES, new IInstallableUnit[0], repositories);
	}

	private ProfileChangeOperation resolve(IProgressMonitor monitor, ProfileChangeOperationFactory operationFactory,
			ResolutionStrategy resolutionStrategy, IInstallableUnit[] ius, URI[] repositories) throws CoreException {
		List<IInstallableUnit> installableUnits = Arrays.asList(ius);
		List<ResolutionStrategy> strategies = new ArrayList<ProfileChangeOperationComputer.ResolutionStrategy>(2);
		switch (resolutionStrategy) {
		case FALLBACK_STRATEGY:
			strategies.add(ResolutionStrategy.SELECTED_REPOSITORIES);
			strategies.add(ResolutionStrategy.ALL_REPOSITORIES);
			break;
		default:
			strategies.add(resolutionStrategy);
		}

		ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
		RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();

		URI[] knownRepositories = repositoryTracker.getKnownRepositories(session);

		ProfileChangeOperation operation = null;
		final int workPerStrategy = 1000;
		SubMonitor subMonitor = SubMonitor.convert(monitor, strategies.size() * workPerStrategy);
		Set<URI> previousRepositoryLocations = null;
		for (ResolutionStrategy strategy : strategies) {
			Set<URI> repositoryLocations = new HashSet<URI>(Arrays.asList(repositories));
			repositoryLocations.addAll(Arrays.asList(repositories));
			if (dependenciesRepository != null) {
				repositoryLocations.add(dependenciesRepository);
			}
			if (strategy == ResolutionStrategy.ALL_REPOSITORIES) {
				repositoryLocations.addAll(Arrays.asList(knownRepositories));
			}
			if (repositoryLocations.equals(previousRepositoryLocations)) {
				continue;
			}
			operation = operationFactory.create(installableUnits);
			if (!repositoryLocations.isEmpty()) {
				URI[] locations = repositoryLocations.toArray(new URI[repositoryLocations.size()]);
				operation.getProvisioningContext().setMetadataRepositories(locations);
				operation.getProvisioningContext().setArtifactRepositories(locations);
			}
			resolveModal(subMonitor.newChild(workPerStrategy), operation);
			if (operation.getResolutionResult() != null
					&& operation.getResolutionResult().getSeverity() != IStatus.ERROR) {
				break;
			}
			previousRepositoryLocations = repositoryLocations;
		}
		return operation;
	}

	public void resolveModal(IProgressMonitor monitor, ProfileChangeOperation operation) throws CoreException {
		operation.resolveModal(new SubProgressMonitor(monitor, items.size()));
	}

	public IInstallableUnit[] computeInstallableUnits(IProgressMonitor monitor) throws CoreException {
		try {
			SubMonitor progress = SubMonitor.convert(monitor, 100);
			// add repository urls and load meta data
			List<IMetadataRepository> repositories = addRepositories(progress.newChild(50));
			List<IInstallableUnit> installableUnits = queryInstallableUnits(progress.newChild(50), repositories);
			checkForUnavailable(installableUnits);
			pruneNonInstall(installableUnits);

			// bug 306446 we never want to downgrade the installed version
			pruneOlderVersions(installableUnits);

			return installableUnits.toArray(new IInstallableUnit[installableUnits.size()]);

		} catch (URISyntaxException e) {
			// should never happen, since we already validated URLs.
			throw new CoreException(new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
					Messages.ProvisioningOperation_unexpectedErrorUrl, e));
		} finally {
			monitor.done();
		}
	}

	public IInstallableUnit[] computeUninstallUnits(IProgressMonitor monitor) throws CoreException {
		try {
			// calculate installed ius
			Map<String, IInstallableUnit> installedIUs = MarketplaceClientUi.computeInstalledIUsById(monitor);
			List<IInstallableUnit> installableUnits = new ArrayList<IInstallableUnit>(installedIUs.values());

			pruneNonUninstall(installableUnits);

			return installableUnits.toArray(new IInstallableUnit[installableUnits.size()]);

		} finally {
			monitor.done();
		}
	}

	/**
	 * Remove ius from the given list where the current profile already contains a newer version of that iu.
	 *
	 * @param installableUnits
	 * @throws CoreException
	 */
	private void pruneOlderVersions(List<IInstallableUnit> installableUnits) throws CoreException {
		if (!installableUnits.isEmpty()) {
			Map<String, IInstallableUnit> iUsById = MarketplaceClientUi.computeInstalledIUsById(new NullProgressMonitor());
			Iterator<IInstallableUnit> it = installableUnits.iterator();
			while (it.hasNext()) {
				IInstallableUnit iu = it.next();
				IInstallableUnit installedIu = iUsById.get(iu.getId());
				if (installedIu != null) {
					Version installedVersion = installedIu.getVersion();
					Version installableVersion = iu.getVersion();
					if (installedVersion.compareTo(installableVersion) >= 0) {
						it.remove();
					}
				}
			}
			if (installableUnits.isEmpty()) {
				throw new CoreException(new Status(IStatus.INFO, MarketplaceClientUi.BUNDLE_ID,
						Messages.ProvisioningOperation_nothingToUpdate));
			}
		}
	}

	private void pruneNonInstall(List<IInstallableUnit> installableUnits) {
		Set<String> installableFeatureIds = new HashSet<String>();
		for (FeatureEntry featureEntry : featureEntries) {
			Operation operation = featureEntry.computeChangeOperation();
			if (operation == Operation.INSTALL || operation == Operation.UPDATE) {
				installableFeatureIds.add(featureEntry.getFeatureDescriptor().getId());
			}
		}
		Iterator<IInstallableUnit> it = installableUnits.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = it.next();
			if (!installableFeatureIds.contains(iu.getId())) {
				it.remove();
			}
		}
	}

	private void pruneNonUninstall(List<IInstallableUnit> installableUnits) {
		Set<String> installableFeatureIds = new HashSet<String>();
		for (FeatureEntry featureEntry : featureEntries) {
			if (featureEntry.computeChangeOperation() == Operation.UNINSTALL) {
				installableFeatureIds.add(featureEntry.getFeatureDescriptor().getId());
			}
		}
		Iterator<IInstallableUnit> it = installableUnits.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = it.next();
			if (!installableFeatureIds.contains(iu.getId())) {
				it.remove();
			}
		}
	}

	/**
	 * Verifies that we found what we were looking for: it's possible that we have connector descriptors that are no
	 * longer available on their respective sites. In that case we must inform the user. Unfortunately this is the
	 * earliest point at which we can know.
	 */
	private void checkForUnavailable(final List<IInstallableUnit> installableUnits) throws CoreException {
		// at least one selected connector could not be found in a repository
		Set<String> foundIds = new HashSet<String>();
		for (IInstallableUnit unit : installableUnits) {
			foundIds.add(unit.getId());
		}

		Set<String> installFeatureIds = new HashSet<String>();
		for (FeatureEntry entry : featureEntries) {
			Operation operation = entry.computeChangeOperation();
			if (operation == Operation.INSTALL || operation == Operation.UPDATE) {
				installFeatureIds.add(entry.getFeatureDescriptor().getId());
			}
		}

		String message = ""; //$NON-NLS-1$
		String detailedMessage = ""; //$NON-NLS-1$
		for (CatalogItem descriptor : items) {
			StringBuilder unavailableIds = null;
			for (String id : getFeatureIds(descriptor)) {
				if (!foundIds.contains(id) && installFeatureIds.contains(id)) {
					if (unavailableIds == null) {
						unavailableIds = new StringBuilder();
					} else {
						unavailableIds.append(Messages.ProvisioningOperation_commaSeparator);
					}
					unavailableIds.append(id);
				}
			}
			if (unavailableIds != null) {
				if (message.length() > 0) {
					message += Messages.ProvisioningOperation_commaSeparator;
				}
				message += descriptor.getName();

				if (detailedMessage.length() > 0) {
					detailedMessage += Messages.ProvisioningOperation_commaSeparator;
				}
				detailedMessage += NLS.bind(Messages.ProvisioningOperation_unavailableFeatures, new Object[] {
						descriptor.getName(), unavailableIds.toString(), descriptor.getSiteUrl() });
			}
		}

		if (message.length() > 0) {
			// instead of aborting here we ask the user if they wish to proceed anyways
			final boolean[] okayToProceed = new boolean[1];
			final String finalMessage = message;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					okayToProceed[0] = MessageDialog.openQuestion(WorkbenchUtil.getShell(),
							Messages.ProvisioningOperation_proceedQuestion, NLS.bind(
									Messages.ProvisioningOperation_unavailableSolutions_proceedQuestion,
									new Object[] { finalMessage }));
				}
			});
			if (!okayToProceed[0]) {
				throw new CoreException(new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, NLS.bind(
						Messages.ProvisioningOperation_unavailableSolutions, detailedMessage), null));
			}
		}
	}

}
