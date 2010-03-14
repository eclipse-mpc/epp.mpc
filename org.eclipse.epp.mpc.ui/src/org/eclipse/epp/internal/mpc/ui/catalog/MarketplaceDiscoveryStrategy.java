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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.epp.internal.mpc.core.service.Categories;
import org.eclipse.epp.internal.mpc.core.service.Category;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.Ius;
import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.epp.internal.mpc.core.service.MarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.core.service.SearchResult;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory.Contents;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;

/**
 * @author David Green
 */
@SuppressWarnings("restriction")
public class MarketplaceDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private final CatalogDescriptor catalogDescriptor;

	private final MarketplaceService marketplaceService;

	private MarketplaceCatalogSource source;

	private MarketplaceInfo marketplaceInfo;

	private Set<String> installedFeatures;

	public MarketplaceDiscoveryStrategy(CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		this.catalogDescriptor = catalogDescriptor;
		marketplaceService = new DefaultMarketplaceService(this.catalogDescriptor.getUrl());
		source = new MarketplaceCatalogSource(marketplaceService);
		marketplaceInfo = MarketplaceInfo.getInstance();
	}

	@Override
	public void dispose() {
		if (source != null) {
			source.dispose();
			source = null;
		}
		if (marketplaceInfo != null) {
			marketplaceInfo.save();
			marketplaceInfo = null;
		}
		super.dispose();
	}

	@Override
	public void performDiscovery(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		final int totalWork = 10000000;
		final int workSegment = totalWork / 3;
		monitor.beginTask("Loading marketplace", totalWork);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(new SubProgressMonitor(monitor, workSegment));

			catalogCategory.setContents(Contents.FEATURED);

			SearchResult featured = marketplaceService.featured(new SubProgressMonitor(monitor, workSegment));
			handleSearchResult(catalogCategory, featured, new SubProgressMonitor(monitor, workSegment));
		} finally {
			monitor.done();
		}
	}

	protected void handleSearchResult(MarketplaceCategory catalogCategory, SearchResult result, IProgressMonitor monitor) {
		if (!result.getNodes().isEmpty()) {
			int totalWork = 10000000;
			monitor.beginTask("Loading resources", totalWork);
			ExecutorService executor = Executors.newFixedThreadPool(Math.min(result.getNodes().size(), 10));
			try {
				List<Future<?>> futures = new ArrayList<Future<?>>();
				for (final Node node : result.getNodes()) {
					final MarketplaceNodeCatalogItem catalogItem = new MarketplaceNodeCatalogItem();
					catalogItem.setMarketplaceUrl(catalogDescriptor.getUrl());
					catalogItem.setId(node.getId());
					catalogItem.setName(node.getName());
					catalogItem.setCategoryId(catalogCategory.getId());
					Categories categories = node.getCategories();
					if (categories != null) {
						for (Category category : categories.getCategory()) {
							catalogItem.addTag(new Tag(Category.class, category.getId(), category.getName()));
						}
					}
					catalogItem.setData(node);
					catalogItem.setSource(source);
					catalogItem.setLicense(node.getLicense());
					Ius ius = node.getIus();
					if (ius != null) {
						catalogItem.setInstallableUnits(ius.getIu());
					}
					catalogItem.setDescription(node.getBody());
					catalogItem.setProvider(node.getCompanyname());
					catalogItem.setSiteUrl(node.getUpdateurl());
					if (node.getBody() != null || node.getScreenshot() != null) {
						final Overview overview = new Overview();
						overview.setItem(catalogItem);
						overview.setSummary(node.getBody());
						overview.setUrl(node.getHomepageurl());
						catalogItem.setOverview(overview);

						if (node.getScreenshot() != null) {
							futures.add(executor.submit(new AbstractResourceRunnable(monitor,
									source.getResourceProvider(), node.getScreenshot()) {
								@Override
								protected void resourceRetrieved() {
									overview.setScreenshot(node.getScreenshot());
								}
							}));
						}
					}
					if (node.getImage() != null) {
						// FIXME: icon sizing
						if (!source.getResourceProvider().containsResource(node.getImage())) {
							futures.add(executor.submit(new AbstractResourceRunnable(monitor,
									source.getResourceProvider(), node.getImage()) {
								@Override
								protected void resourceRetrieved() {
									createIcon(catalogItem, node);
								}

							}));
						} else {
							createIcon(catalogItem, node);
						}
					}
					items.add(catalogItem);
					marketplaceInfo.map(catalogItem.getMarketplaceUrl(), node);
					catalogItem.setInstalled(marketplaceInfo.computeInstalled(computeInstalledFeatures(monitor), node));
				}
				if (!futures.isEmpty()) {
					final int workUnit = totalWork / futures.size();
					while (!futures.isEmpty()) {
						Future<?> future = futures.remove(0);
						final int maxTimeouts = 15;
						for (int timeoutCount = 0;; ++timeoutCount) {
							try {
								future.get(1L, TimeUnit.SECONDS);
								break;
							} catch (TimeoutException e) {
								if (monitor.isCanceled()) {
									return;
								}
								if (timeoutCount > maxTimeouts) {
									future.cancel(true);
									break;
								}
							} catch (InterruptedException e) {
								break;
							} catch (ExecutionException e) {
								MarketplaceClientUi.error(e);
								break;
							}
						}
						monitor.worked(workUnit);
					}
				}
			} finally {
				executor.shutdownNow();
				monitor.done();
			}
			if (result.getMatchCount() != null) {
				catalogCategory.setMatchCount(result.getMatchCount());
				if (result.getMatchCount() > result.getNodes().size()) {
					// add an item here to indicate that the search matched more items than were returned by the server
					CatalogItem catalogItem = new CatalogItem();
					catalogItem.setSource(source);
					catalogItem.setData(catalogDescriptor);
					catalogItem.setId(catalogDescriptor.getUrl().toString());
					catalogItem.setCategoryId(catalogCategory.getId());
					items.add(catalogItem);
				}
			}
		}
	}

	private void createIcon(CatalogItem catalogItem, final Node node) {
		Icon icon = new Icon();
		// don't know the size
		icon.setImage32(node.getImage());
		icon.setImage48(node.getImage());
		icon.setImage64(node.getImage());
		catalogItem.setIcon(icon);
	}

	public void performQuery(Market market, Category category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		final int totalWork = 1000000;
		monitor.beginTask("Searching Marketplace", totalWork);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(new SubProgressMonitor(monitor, 1));
			catalogCategory.setContents(Contents.QUERY);
			SearchResult result = marketplaceService.search(market, category, queryText, new SubProgressMonitor(
					monitor, totalWork / 2));
			handleSearchResult(catalogCategory, result, new SubProgressMonitor(monitor, totalWork / 2));
		} finally {
			monitor.done();
		}
	}

	public void recent(IProgressMonitor monitor) throws CoreException {
		final int totalWork = 1000000;
		monitor.beginTask("Searching Marketplace", totalWork);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(new SubProgressMonitor(monitor, 1));
			catalogCategory.setContents(Contents.RECENT);
			SearchResult result = marketplaceService.recent(new SubProgressMonitor(monitor, totalWork / 2));
			handleSearchResult(catalogCategory, result, new SubProgressMonitor(monitor, totalWork / 2));
		} finally {
			monitor.done();
		}
	}

	public void featured(IProgressMonitor monitor) throws CoreException {
		final int totalWork = 1000000;
		monitor.beginTask("Searching Marketplace", totalWork);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(new SubProgressMonitor(monitor, 1));
			catalogCategory.setContents(Contents.FEATURED);
			SearchResult result = marketplaceService.featured(new SubProgressMonitor(monitor, totalWork / 2));
			handleSearchResult(catalogCategory, result, new SubProgressMonitor(monitor, totalWork / 2));

		} finally {
			monitor.done();
		}
	}

	public void popular(IProgressMonitor monitor) throws CoreException {
		// FIXME: do we want popular or favorites?
		final int totalWork = 1000000;
		monitor.beginTask("Searching Marketplace", totalWork);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(new SubProgressMonitor(monitor, 1));
			catalogCategory.setContents(Contents.POPULAR);
			SearchResult result = marketplaceService.favorites(new SubProgressMonitor(monitor, totalWork / 2));
			handleSearchResult(catalogCategory, result, new SubProgressMonitor(monitor, totalWork / 2));
		} finally {
			monitor.done();
		}
	}

	public void installed(IProgressMonitor monitor) throws CoreException {
		final int totalWork = 1000000;
		monitor.beginTask("Finding Installed", totalWork);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(new SubProgressMonitor(monitor, 1));
			catalogCategory.setContents(Contents.INSTALLED);
			SearchResult result = new SearchResult();
			result.setNodes(new ArrayList<Node>());
			Set<String> installedFeatures = computeInstalledFeatures(monitor);
			if (!monitor.isCanceled()) {
				Set<Node> catalogNodes = marketplaceInfo.computeInstalledNodes(catalogDescriptor.getUrl(),
						installedFeatures);
				if (!catalogNodes.isEmpty()) {
					int unitWork = totalWork / (2 * catalogNodes.size());
					for (Node node : catalogNodes) {
						node = marketplaceService.getNode(node, monitor);
						result.getNodes().add(node);
						monitor.worked(unitWork);
					}
				} else {
					monitor.worked(totalWork / 2);
				}
				handleSearchResult(catalogCategory, result, new SubProgressMonitor(monitor, totalWork / 2));
			}
		} finally {
			monitor.done();
		}
	}

	protected Set<String> computeInstalledFeatures(IProgressMonitor monitor) {
		if (installedFeatures == null) {
			Set<String> features = new HashSet<String>();
			IBundleGroupProvider[] bundleGroupProviders = Platform.getBundleGroupProviders();
			for (IBundleGroupProvider provider : bundleGroupProviders) {
				if (monitor.isCanceled()) {
					break;
				}
				IBundleGroup[] bundleGroups = provider.getBundleGroups();
				for (IBundleGroup group : bundleGroups) {
					features.add(group.getIdentifier());
				}
			}
			installedFeatures = features;
		}
		return installedFeatures;
	}

	protected MarketplaceCategory findMarketplaceCategory(IProgressMonitor monitor) throws CoreException {
		MarketplaceCategory catalogCategory = null;

		monitor.beginTask("Catalog category", 10000);
		try {
			for (CatalogCategory candidate : getCategories()) {
				if (candidate.getSource() == source) {
					catalogCategory = (MarketplaceCategory) candidate;
				}
			}

			if (catalogCategory == null) {
				List<Market> markets = marketplaceService.listMarkets(new SubProgressMonitor(monitor, 10000));

				// marketplace has markets and categories, however a node and/or category can appear in multiple
				// markets.  This doesn't match well with discovery's concept of a category.  Discovery requires all
				// items to be in a category, so we use a single root category and tagging.
				catalogCategory = new MarketplaceCategory();
				catalogCategory.setId("<root>");
				catalogCategory.setName("<root>");
				catalogCategory.setSource(source);

				catalogCategory.setMarkets(markets);

				categories.add(catalogCategory);
			}
		} finally {
			monitor.done();
		}
		return catalogCategory;
	}

}
