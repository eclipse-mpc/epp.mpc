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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

@SuppressWarnings("restriction")
class CompositeProfileChangeOperation extends InstallOperation {

	CompositeProfileChangeOperation(ProvisioningSession session) {
		super(session, null);
		this.session = session;
	}

	private final ProvisioningSession session;

	private final List<ProfileChangeOperation> operations = new ArrayList<ProfileChangeOperation>();

	public CompositeProfileChangeOperation add(ProfileChangeOperation operation) {
		operations.add(operation);
		return this;
	}

	public List<ProfileChangeOperation> getOperations() {
		return operations;
	}

	@Override
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(),
				getProfileId());
		SubMonitor progress = SubMonitor.convert(monitor, 1000 * operations.size());
		for (ProfileChangeOperation operation : operations) {
			updateRequest(request, operation, status, progress.newChild(1000));
		}
		try {
			Field requestField = ProfileChangeOperation.class.getDeclaredField("request"); //$NON-NLS-1$
			boolean accessible = requestField.isAccessible();
			try {
				requestField.setAccessible(true);
				requestField.set(this, request);
			} finally {
				requestField.setAccessible(accessible);
			}
		} catch (Exception e) {
			status.add(new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
					Messages.CompositeProfileChangeOperation_ChangeRequestError, e));
		}
	}

	private void updateRequest(ProfileChangeRequest request, ProfileChangeOperation operation, MultiStatus status, IProgressMonitor monitor) {
		IStatus result = operation.resolveModal(monitor);//TODO we do too much here - this already does the plan resolution, which is expensive...
		status.merge(result);
		if (status.getSeverity() != IStatus.ERROR) {
			IProfileChangeRequest operationChangeRequest = operation.getProfileChangeRequest();
			Collection<IInstallableUnit> additions = operationChangeRequest.getAdditions();
			Collection<IInstallableUnit> removals = operationChangeRequest.getRemovals();
			Collection<IRequirement> extraRequirements = operationChangeRequest.getExtraRequirements();
			request.removeAll(removals);
			request.addAll(additions);
			if (extraRequirements != null) {
				request.addExtraRequirements(extraRequirements);
			}
			if (operationChangeRequest instanceof ProfileChangeRequest) {
				ProfileChangeRequest internalRequest = (ProfileChangeRequest) operationChangeRequest;
				Map<IInstallableUnit, List<String>> installableUnitProfilePropertiesToRemove = internalRequest.getInstallableUnitProfilePropertiesToRemove();
				for (Entry<IInstallableUnit, List<String>> entry : installableUnitProfilePropertiesToRemove.entrySet()) {
					List<String> properties = entry.getValue();
					if (properties != null && !properties.isEmpty()) {
						IInstallableUnit iu = entry.getKey();
						for (String property : properties) {
							request.removeInstallableUnitProfileProperty(iu, property);
						}
					}
				}
				Map<IInstallableUnit, Map<String, String>> installableUnitProfilePropertiesToAdd = internalRequest.getInstallableUnitProfilePropertiesToAdd();
				for (Entry<IInstallableUnit, Map<String, String>> entry : installableUnitProfilePropertiesToAdd.entrySet()) {
					Map<String, String> properties = entry.getValue();
					if (properties != null && !properties.isEmpty()) {
						IInstallableUnit iu = entry.getKey();
						for (Entry<String, String> property : properties.entrySet()) {
							request.setInstallableUnitProfileProperty(iu, property.getKey(), property.getValue());
						}
					}
				}
				String[] propertiesToRemove = internalRequest.getPropertiesToRemove();
				for (String property : propertiesToRemove) {
					request.removeProfileProperty(property);
				}
				Map<String, String> propertiesToAdd = internalRequest.getPropertiesToAdd();
				for (Entry<String, String> property : propertiesToAdd.entrySet()) {
					request.setProfileProperty(property.getKey(), property.getValue());
				}
			}
		}
	}
}
