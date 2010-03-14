/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.epp.internal.mpc.core.service.Category;
import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Certification;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 */
public class MarketplaceCatalog extends Catalog {

	private interface DiscoveryOperation {
		public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException;
	}

	public IStatus performQuery(final Market market, final Category category, final String queryText,
			IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.performQuery(market, category, queryText, monitor);
			}
		}, monitor);
	}

	public IStatus recent(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.recent(monitor);
			}
		}, monitor);
	}

	public IStatus popular(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.popular(monitor);
			}
		}, monitor);
	}

	public IStatus featured(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.featured(monitor);
			}
		}, monitor);
	}

	public IStatus installed(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.installed(monitor);
			}
		}, monitor);
	}

	protected IStatus performDiscovery(DiscoveryOperation operation, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(MarketplaceClientUi.BUNDLE_ID, 0, "Query failed to complete", null);
		if (getDiscoveryStrategies().isEmpty()) {
			throw new IllegalStateException();
		}

		// reset, keeping no items but the same tags, categories and certifications
		List<CatalogItem> items = new ArrayList<CatalogItem>();
		List<CatalogCategory> categories = new ArrayList<CatalogCategory>(getCategories());
		List<Certification> certifications = new ArrayList<Certification>(getCertifications());
		List<Tag> tags = new ArrayList<Tag>(getTags());
		for (CatalogCategory catalogCategory : categories) {
			catalogCategory.getItems().clear();
		}

		final int totalTicks = 100000;
		final int discoveryTicks = totalTicks - (totalTicks / 10);
		monitor.beginTask("Querying marketplace", totalTicks);
		try {
			int strategyTicks = discoveryTicks / getDiscoveryStrategies().size();
			for (AbstractDiscoveryStrategy discoveryStrategy : getDiscoveryStrategies()) {
				if (monitor.isCanceled()) {
					status.add(Status.CANCEL_STATUS);
					break;
				}
				if (discoveryStrategy instanceof MarketplaceDiscoveryStrategy) {
					discoveryStrategy.setCategories(categories);
					discoveryStrategy.setItems(items);
					discoveryStrategy.setCertifications(certifications);
					discoveryStrategy.setTags(tags);
					try {
						MarketplaceDiscoveryStrategy marketplaceStrategy = (MarketplaceDiscoveryStrategy) discoveryStrategy;
						operation.run(marketplaceStrategy, new SubProgressMonitor(monitor, strategyTicks));

					} catch (CoreException e) {
						status.add(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
								"{0} failed with an error", discoveryStrategy.getClass().getSimpleName()), e));
					}
				}
			}

			update(categories, items, certifications, tags);
		} finally {
			monitor.done();
		}
		return status;
	}

}
