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
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory.Contents;
import org.eclipse.epp.internal.mpc.ui.util.ConcurrentTaskManager;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
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

	private final Map<String, Version> repositoryIuVersionById = new HashMap<>();

	private INews news;

	private List<MarketplaceNodeCatalogItem> availableUpdates = new ArrayList<>();

	private interface DiscoveryOperation {
		public void run(MarketplaceDiscoveryStrategy strategy, IProgressMonitor monitor) throws CoreException;
	}

	public IStatus performQuery(final IMarket market, final ICategory category, final String queryText,
			IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.performQuery(market, category, queryText, monitor1),
				false, monitor);
	}

	public IStatus tagged(final String tag, IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.tagged(tag, monitor1), false, monitor);
	}

	public IStatus related(IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.related(monitor1), false, monitor);
	}

	public IStatus recent(IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.recent(monitor1), false, monitor);
	}

	public IStatus popular(IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.popular(monitor1), false, monitor);
	}

	public IStatus featured(IProgressMonitor monitor, final IMarket market, final ICategory category) {
		return performDiscovery((strategy, monitor1) -> strategy.featured(monitor1, market, category), false, monitor);
	}

	public IStatus installed(IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.installed(monitor1), false, monitor);
	}

	public IStatus userFavorites(final boolean login, IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.userFavorites(login, monitor1), false, monitor);
	}

	public IStatus refreshUserFavorites(IProgressMonitor monitor) {
		return performDiscovery((strategy, monitor1) -> strategy.refreshUserFavorites(monitor1), true, monitor);
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
		return performDiscovery((strategy, monitor1) -> strategy.performQuery(monitor1, nodeIds), false, monitor);
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
		return performDiscovery((strategy, monitor1) -> strategy.performNodeQuery(monitor1, nodes), false, monitor);
	}

	public IStatus checkForUpdates(final IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceCatalog_checkingForUpdates, 10000000);
		try {
			Map<String, IInstallableUnit> installedIUs = calculateInstalledIUs(progress.newChild(100000));
			List<MarketplaceNodeCatalogItem> updateCheckNeeded = new ArrayList<>();
			List<CatalogItem> updateCheckItems = getUpdateCheckItems(progress.newChild(100000));
			List<MarketplaceNodeCatalogItem> updateableItems = new ArrayList<MarketplaceNodeCatalogItem>();
			for (CatalogItem item : updateCheckItems) {
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
					} else if (Boolean.TRUE.equals(catalogItem.getUpdateAvailable())) {
						updateableItems.add(catalogItem);
					}
				}
			}
			if (!updateCheckNeeded.isEmpty()) {
				checkForUpdates(updateCheckNeeded, installedIUs, progress.newChild(10000000 - 200000));
				for (MarketplaceNodeCatalogItem catalogItem : updateCheckNeeded) {
					if (Boolean.TRUE.equals(catalogItem.getUpdateAvailable())) {
						updateableItems.add(catalogItem);
					}
				}
			}

			availableUpdates = updateableItems;

			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}

	private List<CatalogItem> getUpdateCheckItems(IProgressMonitor monitor) {
		List<CatalogItem> updateCheckItems = getItems();

		//add all locally installed items as well
		boolean includeInstalled = false;
		for (CatalogCategory category : getCategories()) {
			if (category instanceof MarketplaceCategory) {
				MarketplaceCategory marketplaceCategory = (MarketplaceCategory) category;
				if (marketplaceCategory.getContents() == Contents.FEATURED) {
					includeInstalled = true;
					break;
				}
			}
		}
		if (!includeInstalled) {
			return updateCheckItems;
		}

		updateCheckItems = new ArrayList<>(updateCheckItems);
		Map<String, CatalogItem> itemsById = new HashMap<>();
		for (CatalogItem catalogItem : updateCheckItems) {
			itemsById.put(catalogItem.getId(), catalogItem);
		}
		List<AbstractDiscoveryStrategy> discoveryStrategies = getDiscoveryStrategies();
		SubMonitor progress = SubMonitor.convert(monitor, discoveryStrategies.size() * 1000);
		for (AbstractDiscoveryStrategy discoveryStrategy : discoveryStrategies) {
			if (monitor.isCanceled()) {
				break;
			}
			SubMonitor discoveryStrategyProgress = progress.newChild(1000);
			if (discoveryStrategy instanceof MarketplaceDiscoveryStrategy) {
				MarketplaceDiscoveryStrategy marketplaceDiscoveryStrategy = (MarketplaceDiscoveryStrategy) discoveryStrategy;
				try {
					performUpdateCheckDiscovery(marketplaceDiscoveryStrategy, updateCheckItems, itemsById,
							discoveryStrategyProgress);
				} catch (CoreException e) {
					// TODO Auto-generated catch block WIP
					e.printStackTrace();
				}
			}
			discoveryStrategyProgress.setWorkRemaining(0);
		}
		return updateCheckItems;
	}

	private void performUpdateCheckDiscovery(MarketplaceDiscoveryStrategy marketplaceDiscoveryStrategy,
			List<CatalogItem> updateCheckItems, Map<String, CatalogItem> itemsById,
			SubMonitor discoveryStrategyProgress) throws CoreException {
		MarketplaceCategory catalogCategory = marketplaceDiscoveryStrategy
				.findMarketplaceCategory(discoveryStrategyProgress.newChild(1));
		final Contents realContents = catalogCategory.getContents();
		try {
			catalogCategory.setContents(Contents.INSTALLED);
			ISearchResult installed = marketplaceDiscoveryStrategy
					.computeInstalled(discoveryStrategyProgress.newChild(499));
			List<? extends INode> installedNodes = installed.getNodes();
			if (installedNodes == null || installedNodes.isEmpty()) {
				return;
			}
			SubMonitor itemProgress = discoveryStrategyProgress.newChild(500);
			itemProgress.setWorkRemaining(installedNodes.size() * 100);
			for (INode node : installedNodes) {
				CatalogItem item = marketplaceDiscoveryStrategy.createCatalogItem(node, catalogCategory.getId(), false,
						itemProgress.newChild(100));
				if (!itemsById.containsKey(item.getId())) {
					itemsById.put(item.getId(), item);
					updateCheckItems.add(item);
				}
			}
			itemProgress.done();
		} finally {
			catalogCategory.setContents(realContents);
		}
	}

	private Map<String, IInstallableUnit> calculateInstalledIUs(IProgressMonitor monitor) {
		Map<String, IInstallableUnit> installedIUs = new HashMap<>();
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
		Map<URI, List<MarketplaceNodeCatalogItem>> installedCatalogItemsByUpdateUri = new HashMap<>();

		for (MarketplaceNodeCatalogItem catalogItem : updateCheckNeeded) {
			INode node = catalogItem.getData();
			String updateurl = node.getUpdateurl();
			try {
				if (updateurl == null) {
					catalogItem.setAvailable(false);
					continue;
				}
				URI uri = new URI(updateurl);
				List<MarketplaceNodeCatalogItem> catalogItemsThisSite = installedCatalogItemsByUpdateUri.get(uri);
				if (catalogItemsThisSite == null) {
					catalogItemsThisSite = new ArrayList<>();
					installedCatalogItemsByUpdateUri.put(uri, catalogItemsThisSite);
				}
				catalogItemsThisSite.add(catalogItem);
			} catch (URISyntaxException e) {
				MarketplaceClientUi.log(IStatus.WARNING,
						Messages.MarketplaceCatalog_InvalidRepositoryUrl, node.getName(), updateurl);
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
				executor.submit(() -> {
					ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
					IMetadataRepositoryManager manager = (IMetadataRepositoryManager) session.getProvisioningAgent()
							.getService(IMetadataRepositoryManager.SERVICE_NAME);
					try {
						for (MarketplaceNodeCatalogItem item1 : catalogItemsThisSite) {
							if (Boolean.TRUE.equals(item1.getAvailable())) {
								item1.setAvailable(null);
							}
						}
						IMetadataRepository repository = manager.loadRepository(uri, pm);
						IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery( //
								"id ~= /*.feature.group/ && " + //$NON-NLS-1$
								"properties['org.eclipse.equinox.p2.type.group'] == true ");//$NON-NLS-1$
						IQueryResult<IInstallableUnit> result = repository.query(query, pm);

						// compute highest version for all available IUs.
						Map<String, Version> repositoryIuVersionById = new HashMap<>();
						for (IInstallableUnit iu : result) {
							String key1 = createRepositoryIuKey(uri.toString(), iu.getId());
							Version version = iu.getVersion();
							Version priorVersion = repositoryIuVersionById.put(key1, version);
							if (priorVersion != null && priorVersion.compareTo(version) > 0) {
								repositoryIuVersionById.put(key1, priorVersion);
							}
						}

						for (MarketplaceNodeCatalogItem item2 : catalogItemsThisSite) {
							List<MarketplaceNodeInstallableUnitItem> installableUnitItems = item2
									.getInstallableUnitItems();
							for (MarketplaceNodeInstallableUnitItem iuItem : installableUnitItems) {
								String key2 = createRepositoryIuKey(uri.toString(), iuItem.getId());
								Version availableVersion = repositoryIuVersionById.get(key2);
								MarketplaceCatalog.this.repositoryIuVersionById.put(key2, availableVersion);
								if (availableVersion != null) {
									item2.setAvailable(true);
								}
							}
						}
						for (MarketplaceNodeCatalogItem item3 : catalogItemsThisSite) {
							setUpdatesAvailable(installedIUs, item3);
						}

					} catch (ProvisionException e1) {
						MultiStatus errorStatus = new MultiStatus(MarketplaceClientUi.BUNDLE_ID, IStatus.WARNING,
								NLS.bind(Messages.MarketplaceCatalog_ErrorReadingRepository, uri), e1);
						for (MarketplaceNodeCatalogItem item4 : catalogItemsThisSite) {
							item4.setAvailable(false);
							errorStatus.add(MarketplaceClientUi.newStatus(IStatus.INFO, item4.getName()));
						}
						MarketplaceClientUi.getLog().log(errorStatus);
					} catch (OperationCanceledException e2) {
						// nothing to do
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
				IStatus newStatus = MarketplaceClientCore.computeWellknownProblemStatus(exception);
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
		List<CatalogItem> items = new ArrayList<>();
		List<CatalogCategory> categories = new ArrayList<>(getCategories());
		List<Certification> certifications = new ArrayList<>(getCertifications());
		List<Tag> tags = new ArrayList<>(getTags());
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
					List<CatalogCategory> oldCategories = discoveryStrategy.getCategories();
					List<CatalogItem> oldItems = discoveryStrategy.getItems();
					List<Certification> oldCertifications = discoveryStrategy.getCertifications();
					List<Tag> oldTags = discoveryStrategy.getTags();

					discoveryStrategy.setCategories(categories);
					discoveryStrategy.setItems(items);
					discoveryStrategy.setCertifications(certifications);
					discoveryStrategy.setTags(tags);
					try {
						MarketplaceDiscoveryStrategy marketplaceStrategy = (MarketplaceDiscoveryStrategy) discoveryStrategy;
						operation.run(marketplaceStrategy, new SubProgressMonitor(monitor, strategyTicks));

					} catch (CoreException e) {
						IStatus error = MarketplaceClientCore.computeWellknownProblemStatus(e);
						if (error == null) {
							error = new Status(e.getStatus().getSeverity(), DiscoveryCore.ID_PLUGIN, NLS.bind(
									Messages.MarketplaceCatalog_failedWithError, discoveryStrategy.getClass()
									.getSimpleName()), e);
						}
						status.add(error);
					} finally {
						// remove everything from strategy again, so it can't accidentally mess with the results later
						discoveryStrategy.setCategories(oldCategories);
						discoveryStrategy.setItems(oldItems);
						discoveryStrategy.setCertifications(oldCertifications);
						discoveryStrategy.setTags(oldTags);

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
		List<IMarket> markets = new ArrayList<>();
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

	public List<MarketplaceNodeCatalogItem> getAvailableUpdates() {
		return availableUpdates;
	}

	public void removeItem(CatalogItem item) {
		getItems().remove(item);
		getFilteredItems().remove(item);
		getAvailableUpdates().remove(item);
		for (CatalogCategory category : getCategories()) {
			category.getItems().remove(item);
		}
	}
}
