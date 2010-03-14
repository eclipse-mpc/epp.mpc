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

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Resolve the user-visible names of features from their symbolic names.
 * 
 * @author David Green
 */
public class ResolveFeatureNamesOperation extends AbstractProvisioningOperation {

	private final Set<FeatureDescriptor> featureDescriptors = new HashSet<FeatureDescriptor>();

	private final Set<FeatureDescriptor> unresolvedFeatureDescriptors = new HashSet<FeatureDescriptor>();

	public ResolveFeatureNamesOperation(List<CatalogItem> installableConnectors) {
		super(installableConnectors);
	}

	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
		try {
			SubMonitor monitor = SubMonitor.convert(progressMonitor, "Resolving features", 100);
			try {
				List<IMetadataRepository> repositories = addRepositories(monitor.newChild(50));
				List<IInstallableUnit> installableUnits = queryInstallableUnits(monitor.newChild(50), repositories);
				Set<String> resolvedFeatureIds = new HashSet<String>();
				for (IInstallableUnit iu : installableUnits) {
					FeatureDescriptor descriptor = new FeatureDescriptor(iu);
					resolvedFeatureIds.add(descriptor.getId());
					resolvedFeatureIds.add(descriptor.getSimpleId());
					featureDescriptors.add(descriptor);
				}
				for (CatalogItem catalogItem : installableConnectors) {
					for (String iu : catalogItem.getInstallableUnits()) {
						if (!resolvedFeatureIds.contains(iu)) {
							FeatureDescriptor descriptor = new FeatureDescriptor(iu);
							unresolvedFeatureDescriptors.add(descriptor);
						}
					}
				}

			} finally {
				monitor.done();
			}
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	public String getProperty(IInstallableUnit candidate, String key) {
		String value = TranslationSupport.getInstance().getIUProperty(candidate, key);
		return (value != null) ? value : "";
	}

	public Set<FeatureDescriptor> getFeatureDescriptors() {
		return featureDescriptors;
	}

	public Set<FeatureDescriptor> getUnresolvedFeatureDescriptors() {
		return unresolvedFeatureDescriptors;
	}
}
