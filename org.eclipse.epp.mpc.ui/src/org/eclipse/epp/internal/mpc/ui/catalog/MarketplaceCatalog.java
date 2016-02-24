/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - error handling (bug 374105), news (bug 401721), public API (bug 432803)
 * 	                  featured market (bug 461603)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.util.ConcurrentTaskManager;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Certification;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceCatalog extends Catalog {

	private final Map<String, Version> repositoryIuVersionById = new HashMap<String, Version>();

	private INews news;

	private interface DiscoveryOperation {
		public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException;
	}

	public IStatus performQuery(final IMarket market, final ICategory category, final String queryText,
			IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.performQuery(market, category, queryText, monitor);
			}
		}, false, monitor);
	}

	public IStatus related(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.related(monitor);
			}
		}, false, monitor);
	}

	public IStatus recent(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.recent(monitor);
			}
		}, false, monitor);
	}

	public IStatus popular(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.popular(monitor);
			}
		}, false, monitor);
	}

	public IStatus featured(IProgressMonitor monitor, final IMarket market, final ICategory category) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.featured(monitor, market, category);
			}
		}, false, monitor);
	}

	public IStatus installed(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.installed(monitor);
			}
		}, false, monitor);
	}

	public IStatus userFavorites(final boolean login, IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.userFavorites(login, monitor);
			}
		}, false, monitor);
	}

	public IStatus refreshUserFavorites(IProgressMonitor monitor) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.refreshUserFavorites(monitor);
			}
		}, true, monitor);
	}

	/**
	 * Query for a set of node ids. Use this method infrequently since it may have to make multiple server round-trips.
	 *
	 * @param monitor
	 *            the progress monitor
	 * @param nodeIds
	 *            the nodes to retrieve
	 * @return
	 */
	public IStatus performQuery(IProgressMonitor monitor, final Set<String> nodeIds) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.performQuery(monitor, nodeIds);
			}
		}, false, monitor);
	}

	/**
	 * Query for a set of nodes. Use this method infrequently since it may have to make multiple server round-trips.
	 *
	 * @param monitor
	 *            the progress monitor
	 * @param nodeIds
	 *            the nodes to retrieve
	 * @return
	 */
	public IStatus performNodeQuery(IProgressMonitor monitor, final Set<? extends INode> nodes) {
		return performDiscovery(new DiscoveryOperation() {
			public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException {
				strategy.performNodeQuery(monitor, nodes);
			}
		}, false, monitor);
	}

	public IStatus checkForUpdates(final IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceCatalog_checkingForUpdates, 10000000);
		try {
			Map<String, IInstallableUnit> installedIUs = calculateInstalledIUs(progress.newChild(100000));
			List<MarketplaceNodeCatalogItem> updateCheckNeeded = new ArrayList<MarketplaceNodeCatalogItem>();
			for (CatalogItem item : getItems()) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				if (!(item instanceof MarketplaceNodeCatalogItem)) {
					continue;
				}
				MarketplaceNodeCatalogItem catalogItem = (MarketplaceNodeCatalogItem) item;
				if (catalogItem.isInstalled()) {
					if (setUpdatesAvailable(installedIUs, catalogItem)) {
						updateCheckNeeded.add(catalogItem);
					}
				}
			}
			if (!updateCheckNeeded.isEmpty()) {
				checkForUpdates(updateCheckNeeded, installedIUs, progress.newChild(10000000 - 100000));
			}

			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}

	private Map<String, IInstallableUnit> calculateInstalledIUs(IProgressMonitor monitor) {
		Map<String, IInstallableUnit> installedIUs = new HashMap<String, IInstallableUnit>();
		List<AbstractDiscoveryStrategy> discoveryStrategies = getDiscoveryStrategies();
		SubMonitor progress = SubMonitor.convert(monitor, discoveryStrategies.size() * 1000);
		for (AbstractDiscoveryStrategy discoveryStrategy : discoveryStrategies) {
			if (monitor.isCanceled()) {
				break;
			}
			SubMonitor childProgress = progress.newChild(1000);
			if (discoveryStrategy instanceof MarketplaceDiscoveryStrategy) {
				MarketplaceDiscoveryStrategy marketplaceDiscoveryStrategy = (MarketplaceDiscoveryStrategy) discoveryStrategy;
				Map<String, IInstallableUnit> ius = marketplaceDiscoveryStrategy.computeInstalledIUs(childProgress);
				installedIUs.putAll(ius);
			}
			childProgress.setWorkRemaining(0);
		}
		return installedIUs;
	}

	protected IStatus checkForUpdates(List<MarketplaceNodeCatalogItem> updateCheckNeeded,
			final Map<String, IInstallableUnit> installedIUs, final IProgressMonitor monitor) {
		Map<URI, List<MarketplaceNodeCatalogItem>> installedCatalogItemsByUpdateUri = new HashMap<URI, List<MarketplaceNodeCatalogItem>>();

		for (MarketplaceNodeCatalogItem catalogItem : updateCheckNeeded) {
			INode node = catalogItem.getData();
			try {
				String updateurl = node.getUpdateurl();
				if (updateurl == null) {
					catalogItem.setAvailable(false);
					continue;
				}
				URI uri = new URI(updateurl);
				List<MarketplaceNodeCatalogItem> catalogItemsThisSite = installedCatalogItemsByUpdateUri.get(uri);
				if (catalogItemsThisSite == null) {
					catalogItemsThisSite = new ArrayList<MarketplaceNodeCatalogItem>();
					installedCatalogItemsByUpdateUri.put(uri, catalogItemsThisSite);
				}
				catalogItemsThisSite.add(catalogItem);
			} catch (URISyntaxException e) {
				MarketplaceClientUi.error(e);
				catalogItem.setAvailable(false);
			}
		}
		if (installedCatalogItemsByUpdateUri.isEmpty()) {
			return Status.OK_STATUS;
		}

		ConcurrentTaskManager executor = new ConcurrentTaskManager(installedCatalogItemsByUpdateUri.size(),
				Messages.MarketplaceCatalog_checkingForUpdates);
		try {
			final IProgressMonitor pm = new NullProgressMonitor() {
				@Override
				public boolean isCanceled() {
					return super.isCanceled() || monitor.isCanceled();
				}
			};
			for (Map.Entry<URI, List<MarketplaceNodeCatalogItem>> entry : installedCatalogItemsByUpdateUri.entrySet()) {
				final URI uri = entry.getKey();
				final List<MarketplaceNodeCatalogItem> catalogItemsThisSite = entry.getValue();
				executor.submit(new Runnable() {
					public void run() {
						ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
						IMetadataRepositoryManager manager = (IMetadataRepositoryManager) session.getProvisioningAgent()
								.getService(IMetadataRepositoryManager.SERVICE_NAME);
						try {
							for (MarketplaceNodeCatalogItem item : catalogItemsThisSite) {
								if (Boolean.TRUE.equals(item.getAvailable())) {
									item.setAvailable(null);
								}
							}
							IMetadataRepository repository = manager.loadRepository(uri, pm);
							IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery( //
									"id ~= /*.feature.group/ && " + //$NON-NLS-1$
									"properties['org.eclipse.equinox.p2.type.group'] == true ");//$NON-NLS-1$
							IQueryResult<IInstallableUnit> result = repository.query(query, pm);

							// compute highest version for all available IUs.
							Map<String, Version> repositoryIuVersionById = new HashMap<String, Version>();
							for (IInstallableUnit iu : result) {
								String key = createRepositoryIuKey(uri.toString(), iu.getId());
								Version version = iu.getVersion();
								Version priorVersion = repositoryIuVersionById.put(key, version);
								if (priorVersion != null && priorVersion.compareTo(version) > 0) {
									repositoryIuVersionById.put(key, priorVersion);
								}
							}

							for (MarketplaceNodeCatalogItem item : catalogItemsThisSite) {
								List<MarketplaceNodeInstallableUnitItem> installableUnitItems = item.getInstallableUnitItems();
								for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
									String key = createRepositoryIuKey(uri.toString(), iuItem.getId());
									Version availableVersion = repositoryIuVersionById.get(key);
									MarketplaceCatalog.this.repositoryIuVersionById.put(key, availableVersion);
									if (availableVersion != null) {
										item.setAvailable(true);
									}
								}
							}
							for (MarketplaceNodeCatalogItem item : catalogItemsThisSite) {
								setUpdatesAvailable(installedIUs, item);
							}

						} catch (ProvisionException e) {
							MarketplaceClientUi.error(e);
							for (MarketplaceNodeCatalogItem item : catalogItemsThisSite) {
								item.setAvailable(false);
							}
						} catch (OperationCanceledException e) {
							// nothing to do
						}
					}
				});
			}
			try {
				executor.waitUntilFinished(monitor);
			} catch (CoreException e) {
				MarketplaceClientUi.error(e);
				return e.getStatus();
			}
			return Status.OK_STATUS;
		} finally {
			executor.shutdownNow();
		}
	}

	private String createRepositoryIuKey(String uri, String id) {
		return uri + "!" + id; //$NON-NLS-1$
	}

	private boolean setUpdatesAvailable(Map<String, IInstallableUnit> installedIUs, MarketplaceNodeCatalogItem item) {
		boolean needOnlineCheck = false;
		List<MarketplaceNodeInstallableUnitItem> installableUnitItems = item.getInstallableUnitItems();
		for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
			String key = createRepositoryIuKey(item.getSiteUrl(), iuItem.getId());
			Version availableVersion = repositoryIuVersionById.get(key);
			iuItem.setUpdateAvailable(false);
			iuItem.setAvailable(false);
			if (availableVersion != null) {
				iuItem.setAvailable(true);
				IInstallableUnit installedIu = installedIUs.get(iuItem.getId());
				if (installedIu != null && installedIu.getVersion().compareTo(availableVersion) < 0) {
					iuItem.setUpdateAvailable(true);
				}
			} else if (!repositoryIuVersionById.containsKey(key)) {
				needOnlineCheck = true;
			}
		}
		return needOnlineCheck;
	}

	@Override
	public IStatus performDiscovery(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 200000);
		IStatus status = super.performDiscovery(progress.newChild(100000));

		//check for updates
		if (status.getSeverity() < IStatus.ERROR) {
			IStatus updateStatus = checkForUpdates(progress.newChild(100000));
			if (!updateStatus.isOK()) {
				if (status.isOK()) {
					status = updateStatus;
				} else {
					MultiStatus multiStatus = new MultiStatus(MarketplaceClientUi.BUNDLE_ID, 0,
							Messages.MarketplaceCatalog_Discovery_Error, null);
					multiStatus.add(status);
					multiStatus.add(updateStatus);
					status = multiStatus;
				}
			}
		}
		return computeStatus(status);
	}

	private IStatus computeStatus(IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			// unwrap a multistatus with one child see bug 309612
			if (status.isMultiStatus()) {
				MultiStatus multiStatus = (MultiStatus) status;
				if (multiStatus.getChildren().length == 1) {
					status = multiStatus.getChildren()[0];
				}
			}
			if (!status.isMultiStatus()) {
				Throwable exception = status.getException();
				IStatus newStatus = MarketplaceClientUi.computeWellknownProblemStatus(exception);
				if (newStatus != null) {
					return newStatus;
				}
			}
		}
		return status;
	}

	protected IStatus performDiscovery(DiscoveryOperation operation, boolean refresh, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(MarketplaceClientUi.BUNDLE_ID, 0, Messages.MarketplaceCatalog_queryFailed,
				null);
		if (getDiscoveryStrategies().isEmpty()) {
			throw new IllegalStateException();
		}

		// reset, keeping no items but the same tags, categories and certifications
		List<CatalogItem> items = new ArrayList<CatalogItem>();
		List<CatalogCategory> categories = new ArrayList<CatalogCategory>(getCategories());
		List<Certification> certifications = new ArrayList<Certification>(getCertifications());
		List<Tag> tags = new ArrayList<Tag>(getTags());
		if (!refresh) {
			for (CatalogCategory catalogCategory : categories) {
				catalogCategory.getItems().clear();
			}
		}

		final int totalTicks = 100000;
		final int discoveryTicks = totalTicks - (totalTicks / 10);
		monitor.beginTask(Messages.MarketplaceCatalog_queryingMarketplace, totalTicks);
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
						IStatus error = MarketplaceClientUi.computeWellknownProblemStatus(e);
						if (error == null) {
							error = new Status(e.getStatus().getSeverity(), DiscoveryCore.ID_PLUGIN, NLS.bind(
									Messages.MarketplaceCatalog_failedWithError, discoveryStrategy.getClass()
									.getSimpleName()), e);
						}
						status.add(error);
					} finally {
						// remove everything from strategy again, so it can't accidentally mess with the results later
						discoveryStrategy.setCategories(null);
						discoveryStrategy.setItems(null);
						discoveryStrategy.setCertifications(null);
						discoveryStrategy.setTags(null);

						// make sure strategy didn't misbehave
						if (items.contains(null)) {
							while (items.remove(null)) {
							}
							IStatus error = new Status(IStatus.WARNING, DiscoveryCore.ID_PLUGIN,
									NLS.bind(Messages.MarketplaceCatalog_addedNullEntry, discoveryStrategy.getClass().getSimpleName()));
							status.add(error);
						}
					}
				}
			}

			update(categories, items, certifications, tags);
		} finally {
			monitor.done();
		}
		return computeStatus(status);
	}

	public IStatus performNewsDiscovery(IProgressMonitor monitor) {
		if (getDiscoveryStrategies().isEmpty()) {
			throw new IllegalStateException();
		}

		INews news = null;

		MultiStatus status = new MultiStatus(MarketplaceClientUi.BUNDLE_ID, 0, Messages.MarketplaceCatalog_queryFailed,
				null);
		final int totalTicks = 100000;
		final SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceCatalog_Checking_News, totalTicks);
		try {
			int strategyTicks = totalTicks / getDiscoveryStrategies().size();
			for (AbstractDiscoveryStrategy discoveryStrategy : getDiscoveryStrategies()) {
				if (monitor.isCanceled()) {
					status.add(Status.CANCEL_STATUS);
					break;
				}
				if (discoveryStrategy instanceof MarketplaceDiscoveryStrategy) {
					try {
						MarketplaceDiscoveryStrategy marketplaceStrategy = (MarketplaceDiscoveryStrategy) discoveryStrategy;
						news = marketplaceStrategy.performNewsDiscovery(progress.newChild(strategyTicks));
						if (news != null) {
							break;
						}
					} catch (CoreException e) {
						status.add(new Status(e.getStatus().getSeverity(), DiscoveryCore.ID_PLUGIN, NLS.bind(
								Messages.MarketplaceCatalog_failedWithError, discoveryStrategy.getClass()
								.getSimpleName()), e));
					}
				}
			}
		} finally {
			monitor.done();
		}
		if (status.isOK()) {
			setNews(news);
		}
		return status;
	}

	/**
	 * Report an error for an attempted install
	 *
	 * @param result
	 *            the result of the install operation
	 * @param items
	 *            the catalog items to be installed
	 * @param operationIUs
	 *            the computed IUs that were part of the provisioning operation
	 * @param resolutionDetails
	 *            the detailed error message, or null
	 */
	public void installErrorReport(IProgressMonitor monitor, IStatus result, Set<CatalogItem> items,
			IInstallableUnit[] operationIUs, String resolutionDetails) {
		for (AbstractDiscoveryStrategy discoveryStrategy : getDiscoveryStrategies()) {
			if (discoveryStrategy instanceof MarketplaceDiscoveryStrategy) {
				try {
					((MarketplaceDiscoveryStrategy) discoveryStrategy).installErrorReport(monitor, result, items,
							operationIUs, resolutionDetails);
				} catch (CoreException e) {
					// log only
					MarketplaceClientUi.error(e);
				}
			}
		}
	}

	public INews getNews() {
		return news;
	}

	public void setNews(INews news) {
		this.news = news;
	}

	public List<IMarket> getMarkets() {
		List<IMarket> markets = new ArrayList<IMarket>();
		for (CatalogCategory category : getCategories()) {
			if (category instanceof MarketplaceCategory) {
				MarketplaceCategory marketplaceCategory = (MarketplaceCategory) category;
				for (IMarket market : marketplaceCategory.getMarkets()) {
					if (!markets.contains(market)) {
						markets.add(market);
					}
				}
			}
		}
		return markets;
	}
}
