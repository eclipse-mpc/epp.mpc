/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 314936, bug 398200, bug 432803: public API, bug 413871: performance
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.model.Identifiable;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.model.SearchResult;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.internal.mpc.core.service.StorageConfigurer;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory.Contents;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem.UserAction;
import org.eclipse.epp.mpc.core.model.ICategories;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IIdentifiable;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.IIus;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.IMarketplaceService;
import org.eclipse.epp.mpc.core.service.IMarketplaceServiceLocator;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService.LoginListener;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.osgi.framework.BundleContext;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private static final Pattern BREAK_PATTERN = Pattern.compile("<!--\\s*break\\s*-->"); //$NON-NLS-1$

	protected final CatalogDescriptor catalogDescriptor;

	protected final IMarketplaceService marketplaceService;

	private MarketplaceCatalogSource source;

	private MarketplaceInfo marketplaceInfo;

	private Map<String, IInstallableUnit> featureIUById;

	private List<LoginListener> loginListeners;

	private IShellProvider shellProvider;

	public MarketplaceDiscoveryStrategy(CatalogDescriptor catalogDescriptor) {
		if (catalogDescriptor == null) {
			throw new IllegalArgumentException();
		}
		this.catalogDescriptor = catalogDescriptor;
		marketplaceService = createMarketplaceService();//use deprecated method in case someone has overridden it

		source = new MarketplaceCatalogSource(marketplaceService);
		marketplaceInfo = MarketplaceInfo.getInstance();
	}

	/**
	 * @deprecated get a marketplace service from the registered {@link IMarketplaceServiceLocator} OSGi service instead
	 */
	@Deprecated
	public IMarketplaceService createMarketplaceService() {
		return acquireMarketplaceService();
	}

	protected IMarketplaceService acquireMarketplaceService() {
		String baseUrl = this.catalogDescriptor.getUrl().toExternalForm();
		return ServiceLocator.getCompatibilityLocator().getMarketplaceService(baseUrl);
	}

	/**
	 * @deprecated moved to {@link ServiceLocator#computeDefaultRequestMetaParameters()}
	 */
	@Deprecated
	public static Map<String, String> computeDefaultRequestMetaParameters() {
		return ServiceLocator.computeDefaultRequestMetaParameters();
	}

	@Override
	public void dispose() {
		List<LoginListener> loginListeners = this.loginListeners;
		this.loginListeners = null;
		if (loginListeners != null) {
			IUserFavoritesService favoritesService = marketplaceService.getUserFavoritesService();
			if (favoritesService != null) {
				IMarketplaceStorageService storageService = favoritesService.getStorageService();
				for (LoginListener loginListener : loginListeners) {
					storageService.removeLoginListener(loginListener);
				}
			}
		}
		if (source != null) {
			source.dispose();
			source = null;
		}
		if (marketplaceInfo != null) {
			final MarketplaceInfo fMarketplaceInfo = marketplaceInfo;
			new Job(Messages.MarketplaceDiscoveryStrategy_saveMarketplaceInfoJobName) {

				{
					setSystem(true);
					setPriority(SHORT);
					setUser(false);
				}
				@Override
				public boolean belongsTo(Object family) {
					BundleContext bundleContext = MarketplaceClientUiPlugin.getBundleContext();
					MarketplaceClientUiPlugin plugin = MarketplaceClientUiPlugin.getInstance();
					return (bundleContext != null && (family == bundleContext || family == bundleContext.getBundle()))
							|| (plugin != null && family == plugin);
				}

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						fMarketplaceInfo.save();
					} catch (Exception e) {
						return MarketplaceClientCore.computeStatus(e, Messages.MarketplaceDiscoveryStrategy_failedToSaveMarketplaceInfo);
					}
					return Status.OK_STATUS;
				}
			}.schedule();
			marketplaceInfo = null;
		}
		super.dispose();
	}

	public synchronized void addLoginListener(LoginListener loginListener) {
		IUserFavoritesService favoritesService = marketplaceService.getUserFavoritesService();
		if (favoritesService != null) {
			if (loginListeners == null) {
				loginListeners = new CopyOnWriteArrayList<LoginListener>();
			}
			if (!loginListeners.contains(loginListener)) {
				loginListeners.add(loginListener);
				IMarketplaceStorageService storageService = favoritesService.getStorageService();
				storageService.addLoginListener(loginListener);
			}
		}
	}

	public synchronized void removeLoginListener(LoginListener loginListener) {
		if (loginListeners != null) {
			loginListeners.remove(loginListener);
		}
		IUserFavoritesService favoritesService = marketplaceService.getUserFavoritesService();
		if (favoritesService != null) {
			IMarketplaceStorageService storageService = favoritesService.getStorageService();
			storageService.removeLoginListener(loginListener);
		}
	}

	protected void applyShellProvider() {
		IUserFavoritesService userFavoritesService = marketplaceService.getUserFavoritesService();
		if (userFavoritesService == null) {
			return;
		}
		IMarketplaceStorageService storageService = userFavoritesService.getStorageService();
		if (storageService == null) {
			return;
		}
		IStorage storage = storageService.getStorage();
		ICredentialsProvider credentialsProvider = storage.getCredentialsProvider();
		if (credentialsProvider != null) {
			try {
				StorageConfigurer.get().setShellProvider(storage, this.shellProvider);
			} catch (CoreException e) {
				//ignore
			}
		}
	}

	public boolean hasUserFavoritesService() {
		return marketplaceService.getUserFavoritesService() != null;
	}

	@Override
	public void performDiscovery(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_loadingMarketplace,
				3000);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1000));

			handleDiscoveryCategory(catalogCategory);

			ISearchResult discoveryResult = doPerformDiscovery(progress.newChild(1000));
			handleSearchResult(catalogCategory, discoveryResult, progress.newChild(1000));
			maybeAddCatalogItem(catalogCategory);
		} finally {
			monitor.done();
		}
	}

	protected ISearchResult doPerformDiscovery(IProgressMonitor monitor) throws CoreException {
		return marketplaceService.featured(monitor);
	}

	protected void handleDiscoveryCategory(MarketplaceCategory catalogCategory) {
		catalogCategory.setContents(Contents.FEATURED);
	}

	protected void handleSearchResult(MarketplaceCategory catalogCategory, ISearchResult result,
			final IProgressMonitor monitor) {
		List<CatalogItem> items = getItems();
		if (items != null && !result.getNodes().isEmpty()) {
			int nodeWork = 1000;
			int favoritesWork = catalogCategory.getContents() == Contents.USER_FAVORITES ? 0 : 1000;
			SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_loadingResources,
					result.getNodes().size() * nodeWork + favoritesWork);

			try {
				boolean userFavoritesSupported = false;
				if (catalogCategory.getContents() == Contents.USER_FAVORITES) {
					userFavoritesSupported = true;
				} else if (hasUserFavoritesService()) {
					try {
						applyShellProvider();
						marketplaceService.userFavorites(result.getNodes(), progress.newChild(favoritesWork));
						userFavoritesSupported = true;
					} catch (NotAuthorizedException e1) {
						// user is not logged in. we just ignore this.
					} catch (UnsupportedOperationException e1) {
						// ignore
					} catch (Exception e1) {
						// something went wrong. log and proceed.
						MarketplaceClientCore.error(Messages.MarketplaceDiscoveryStrategy_FavoritesRetrieveError, e1);
					}
				}
				for (final INode node : result.getNodes()) {
					String id = node.getId();
					try {
						final MarketplaceNodeCatalogItem catalogItem = new MarketplaceNodeCatalogItem();
						catalogItem.setMarketplaceUrl(catalogDescriptor.getUrl());
						catalogItem.setId(id);
						catalogItem.setName(getCatalogItemName(node));
						catalogItem.setCategoryId(catalogCategory.getId());
						ICategories categories = node.getCategories();
						if (categories != null) {
							for (ICategory category : categories.getCategory()) {
								catalogItem.addTag(new Tag(ICategory.class, category.getId(), category.getName()));
							}
						}
						catalogItem.setData(node);
						catalogItem.setSource(source);
						catalogItem.setLicense(node.getLicense());
						catalogItem.setUserFavorite(userFavoritesSupported ? node.getUserFavorite() : null);
						IIus ius = node.getIus();
						if (ius != null) {
							List<MarketplaceNodeInstallableUnitItem> installableUnitItems = new ArrayList<MarketplaceNodeInstallableUnitItem>();
							for (IIu iu : ius.getIuElements()) {
								MarketplaceNodeInstallableUnitItem iuItem = new MarketplaceNodeInstallableUnitItem();
								iuItem.init(iu);
								installableUnitItems.add(iuItem);
							}
							catalogItem.setInstallableUnitItems(installableUnitItems);
						}
						if (node.getShortdescription() == null && node.getBody() != null) {
							// bug 306653 <!--break--> marks the end of the short description.
							String descriptionText = node.getBody();
							Matcher matcher = BREAK_PATTERN.matcher(node.getBody());
							if (matcher.find()) {
								int start = matcher.start();
								if (start > 0) {
									String shortDescriptionText = descriptionText.substring(0, start).trim();
									if (shortDescriptionText.length() > 0) {
										descriptionText = shortDescriptionText;
									}
								}
							}
							catalogItem.setDescription(descriptionText);
						} else {
							catalogItem.setDescription(node.getShortdescription());
						}
						catalogItem.setProvider(node.getCompanyname());
						String updateurl = node.getUpdateurl();
						if (updateurl != null) {
							try {
								// trim is important!
								updateurl = updateurl.trim();
								URLUtil.toURL(updateurl);
								catalogItem.setSiteUrl(updateurl);
							} catch (MalformedURLException e) {
								// don't use malformed URLs
							}
						}
						if (catalogItem.getInstallableUnits() == null || catalogItem.getInstallableUnits().isEmpty()
								|| catalogItem.getSiteUrl() == null) {
							catalogItem.setAvailable(false);
						}
						if (node.getImage() != null) {
							if (!source.getResourceProvider().containsResource(node.getImage())) {
								cacheResource(source.getResourceProvider(), catalogItem, node.getImage());
							}
							createIcon(catalogItem, node);
						}
						if (node.getBody() != null || node.getScreenshot() != null) {
							final Overview overview = new Overview();
							overview.setItem(catalogItem);
							overview.setSummary(node.getBody());
							overview.setUrl(node.getUrl());
							catalogItem.setOverview(overview);

							if (node.getScreenshot() != null) {
								if (!source.getResourceProvider().containsResource(node.getScreenshot())) {
									cacheResource(source.getResourceProvider(), catalogItem, node.getScreenshot());
								}
								overview.setScreenshot(node.getScreenshot());
							}
						}
						items.add(catalogItem);
						marketplaceInfo.map(catalogItem.getMarketplaceUrl(), node);
						marketplaceInfo.computeInstalled(computeInstalledFeatures(progress.newChild(nodeWork)),
								catalogItem);
					} catch (RuntimeException ex) {
						MarketplaceClientUi.error(
								NLS.bind(Messages.MarketplaceDiscoveryStrategy_ParseError,
										node == null ? "null" : id), //$NON-NLS-1$
								ex);
					}
				}
			} finally {
				progress.done();
			}
			if (result.getMatchCount() != null) {
				catalogCategory.setMatchCount(result.getMatchCount());
				if (result.getMatchCount() > result.getNodes().size()) {
					// add an item here to indicate that the search matched more items than were returned by the server
					addCatalogItem(catalogCategory);
				}
			}
		}
	}

	public static void cacheResource(ResourceProvider resourceProvider, CatalogItem catalogItem,
			String resource) {
		if (!resourceProvider.containsResource(resource)) {
			String requestSource = NLS.bind(Messages.MarketplaceDiscoveryStrategy_requestSource, catalogItem.getName(), catalogItem.getId());
			try {
				resourceProvider.retrieveResource(requestSource, resource);
			} catch (URISyntaxException e) {
				MarketplaceClientUi.log(IStatus.WARNING, Messages.MarketplaceDiscoveryStrategy_badUri,
						catalogItem.getName(),
						catalogItem.getId(), resource, e);
			} catch (IOException e) {
				MarketplaceClientUi.log(IStatus.WARNING, Messages.MarketplaceDiscoveryStrategy_downloadError,
						catalogItem.getName(),
						catalogItem.getId(), resource, e);
			}
		}
	}

	private static String getCatalogItemName(INode node) {
		String name = node.getName();
		String version = node.getVersion();
		return version == null || version.length() == 0 ? name : NLS.bind(
				Messages.MarketplaceDiscoveryStrategy_Name_and_Version, name, version);
	}

	public void maybeAddCatalogItem(MarketplaceCategory catalogCategory) {
		List<CatalogItem> items = getItems();
		if (items != null && !items.isEmpty()) {
			CatalogItem catalogItem = items.get(items.size() - 1);
			if (catalogItem.getData() != catalogDescriptor) {
				addCatalogItem(catalogCategory);
			}
		}
	}

	public void addCatalogItem(MarketplaceCategory catalogCategory) {
		CatalogItem catalogItem = createCategoryItem(catalogCategory);
		items.add(catalogItem);
	}

	private CatalogItem createCategoryItem(MarketplaceCategory catalogCategory) {
		CatalogItem catalogItem = new CatalogItem();
		catalogItem.setSource(source);
		catalogItem.setData(catalogDescriptor);
		catalogItem.setId(catalogDescriptor.getUrl().toString());
		catalogItem.setCategoryId(catalogCategory.getId());
		return catalogItem;
	}

	public UserActionCatalogItem addUserActionItem(MarketplaceCategory catalogCategory, UserAction userAction) {
		return addUserActionItem(catalogCategory, userAction, catalogDescriptor);
	}

	public UserActionCatalogItem addUserActionItem(MarketplaceCategory catalogCategory, UserAction userAction,
			Object data) {
		for (ListIterator<CatalogItem> i = items.listIterator(items.size()); i.hasPrevious();) {
			CatalogItem item = i.previous();
			if (item.getSource() == source && (item.getCategory() == catalogCategory || catalogCategory.getId().equals(item.getCategoryId()))
					&& item instanceof UserActionCatalogItem) {
				UserActionCatalogItem actionItem = (UserActionCatalogItem) item;
				if (actionItem.getUserAction() == userAction) {
					return actionItem;
				}
			}
		}
		UserActionCatalogItem catalogItem = new UserActionCatalogItem();
		catalogItem.setUserAction(userAction);
		catalogItem.setSource(source);
		catalogItem.setData(data);
		catalogItem.setId(catalogDescriptor.getUrl().toString() + "#" + userAction.name()); //$NON-NLS-1$
		catalogItem.setCategoryId(catalogCategory.getId());
		items.add(0, catalogItem);
		return catalogItem;
	}

	private static void createIcon(CatalogItem catalogItem, final INode node) {
		Icon icon = new Icon();
		// don't know the size
		icon.setImage32(node.getImage());
		icon.setImage48(node.getImage());
		icon.setImage64(node.getImage());
		catalogItem.setIcon(icon);
	}

	public void tagged(String tag, IProgressMonitor monitor) throws CoreException {
		final int totalWork = 1000;
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				totalWork);
		try {
			ISearchResult result;
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.QUERY);

			//resolve market and category if necessary
			result = marketplaceService.tagged(tag, progress.newChild(500));

			handleSearchResult(catalogCategory, result, progress.newChild(500));
			if (result.getNodes().isEmpty()) {
				catalogCategory.setMatchCount(0);
				addCatalogItem(catalogCategory);
			}
		} finally {
			progress.done();
		}
	}

	public void performQuery(IMarket market, ICategory category, String queryText, IProgressMonitor monitor)
			throws CoreException {
		final int totalWork = 1001;
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				totalWork);
		try {
			ISearchResult result;
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.QUERY);

			SubMonitor nodeQueryProgress = progress.newChild(500);
			try {
				//check if the query matches a node url and just retrieve that node
				result = performNodeQuery(queryText, nodeQueryProgress);
			} catch (CoreException ex) {
				// node not found, continue with regular query
				result = null;
				//no work was done
				nodeQueryProgress.setWorkRemaining(0);
			}

			if (result == null) {
				//regular query

				//resolve market and category if necessary
				IMarket resolvedMarket;
				ICategory resolvedCategory;
				try {
					resolvedMarket = resolve(market, catalogCategory.getMarkets());
					resolvedCategory = resolveCategory(category, catalogCategory.getMarkets());
				} catch (IllegalArgumentException ex) {
					throw new CoreException(MarketplaceClientCore.computeStatus(ex, Messages.MarketplaceDiscoveryStrategy_invalidFilter));
				} catch (NoSuchElementException ex) {
					throw new CoreException(MarketplaceClientCore.computeStatus(ex, Messages.MarketplaceDiscoveryStrategy_unknownFilter));
				}
				progress.setWorkRemaining(totalWork - 1);
				result = marketplaceService.search(resolvedMarket, resolvedCategory, queryText, progress.newChild(500));
			}

			handleSearchResult(catalogCategory, result, progress.newChild(500));
			if (result.getNodes().isEmpty()) {
				catalogCategory.setMatchCount(0);
				addCatalogItem(catalogCategory);
			}
		} finally {
			progress.done();
		}
	}

	private static ICategory resolveCategory(ICategory category, List<? extends IMarket> markets)
			throws IllegalArgumentException, NoSuchElementException {
		if (category != null && category.getId() == null) {
			//need to resolve
			if (category.getUrl() == null && category.getName() == null) {
				throw new IllegalArgumentException(NLS.bind(Messages.MarketplaceDiscoveryStrategy_unidentifiableItem,
						category));
			}
			for (IMarket market : markets) {
				List<? extends ICategory> categories = market.getCategory();
				ICategory resolved = resolve(category, categories);
				if (resolved != null) {
					return resolved;
				}
			}
			if (category.getUrl() != null) {
				throw new NoSuchElementException(NLS.bind(Messages.MarketplaceDiscoveryStrategy_noUrlMatch,
						category.getUrl()));
			} else {
				throw new NoSuchElementException(NLS.bind(Messages.MarketplaceDiscoveryStrategy_noNameMatch,
						category.getName()));
			}
		}
		return category;
	}

	private static <T extends IIdentifiable> T resolve(T id, List<? extends T> candidates)
			throws IllegalArgumentException,
			NoSuchElementException {
		if (id != null && id.getId() == null) {
			//need to resolve
			if (id.getUrl() == null && id.getName() == null) {
				throw new IllegalArgumentException(NLS.bind(
						Messages.MarketplaceDiscoveryStrategy_unidentifiableItem, id));
			}
			for (T candidate : candidates) {
				if (Identifiable.matches(candidate, id)) {
					return candidate;
				}
			}
			if (id.getUrl() != null) {
				throw new NoSuchElementException(NLS.bind(Messages.MarketplaceDiscoveryStrategy_noUrlMatch, id.getUrl()));
			} else {
				throw new NoSuchElementException(NLS.bind(Messages.MarketplaceDiscoveryStrategy_noNameMatch, id.getName()));
			}
		}
		return id;
	}

	private ISearchResult performNodeQuery(String nodeUrl, IProgressMonitor progress) throws CoreException {
		final INode[] queryNode = new INode[1];
		MarketplaceUrlHandler urlHandler = new MarketplaceUrlHandler() {
			@Override
			protected boolean handleNode(CatalogDescriptor descriptor, String url, INode node) {
				queryNode[0] = node;
				return true;
			}
		};
		if (urlHandler.handleUri(nodeUrl) && queryNode[0] != null) {
			INode node = marketplaceService.getNode(queryNode[0], progress);
			SearchResult result = new SearchResult();
			result.setMatchCount(1);
			result.setNodes(Collections.singletonList((Node) node));
			return result;
		}
		return null;
	}

	public void recent(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				1001);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.RECENT);
			ISearchResult result = marketplaceService.recent(progress.newChild(500));
			handleSearchResult(catalogCategory, result, progress.newChild(500));
			maybeAddCatalogItem(catalogCategory);
		} finally {
			monitor.done();
		}
	}

	public void related(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				801);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.RELATED);
			SearchResult installed = computeInstalled(progress.newChild(200));
			if (!monitor.isCanceled()) {
				ISearchResult result = marketplaceService.related(installed.getNodes(),
						progress.newChild(300));
				handleSearchResult(catalogCategory, result, progress.newChild(300));
				maybeAddCatalogItem(catalogCategory);
			}
		} finally {
			monitor.done();
		}
	}

	public void featured(IProgressMonitor monitor, final IMarket market, final ICategory category) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				1001);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.FEATURED);
			ISearchResult result = marketplaceService.featured(market, category, progress.newChild(500));
			handleSearchResult(catalogCategory, result, progress.newChild(500));
			maybeAddCatalogItem(catalogCategory);
		} finally {
			monitor.done();
		}
	}

	public void popular(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				1001);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.POPULAR);
			ISearchResult result = marketplaceService.popular(progress.newChild(500));
			handleSearchResult(catalogCategory, result, progress.newChild(500));
			maybeAddCatalogItem(catalogCategory);
		} finally {
			monitor.done();
		}
	}

	public void userFavorites(boolean promptLogin, IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_FavoritesRetrieve, 1001);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.USER_FAVORITES);
			IUserFavoritesService userFavoritesService = marketplaceService.getUserFavoritesService();
			if (userFavoritesService != null) {
				try {
					applyShellProvider();
					ISearchResult result;
					if (promptLogin) {
						IMarketplaceStorageService storageService = userFavoritesService.getStorageService();
						result = storageService.runWithLogin(new Callable<ISearchResult>() {
							public ISearchResult call() throws Exception {
								return marketplaceService.userFavorites(progress.newChild(500));
							}
						});
					} else {
						result = marketplaceService.userFavorites(progress.newChild(500));
					}
					if (result.getNodes().isEmpty()) {
						catalogCategory = addPopularItems(progress.newChild(500));
						addNoFavoritesItem(catalogCategory);
					} else {
						handleSearchResult(catalogCategory, result, progress.newChild(500));
					}
				} catch (NotAuthorizedException e) {
					catalogCategory = addPopularItems(progress.newChild(500));
					addUserStorageLoginItem(catalogCategory, e.getLocalizedMessage());
				} catch (UnsupportedOperationException ex) {
					catalogCategory = addPopularItems(progress.newChild(500));
					addFavoritesNotSupportedItem(catalogCategory);
				} catch (Exception ex) {
					//FIXME we should use the wizard page's status line to show errors, but that's unreachable from here...
					MarketplaceClientCore.error(Messages.MarketplaceDiscoveryStrategy_FavoritesRetrieveError, ex);
					addRetryErrorItem(catalogCategory, ex);
				}
			} else {
				catalogCategory = addPopularItems(progress.newChild(1000));
				addFavoritesNotSupportedItem(catalogCategory);
			}
		} finally {
			monitor.done();
		}
	}

	private MarketplaceCategory addPopularItems(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		MarketplaceCategory catalogCategory;
		popular(progress.newChild(99));
		catalogCategory = findMarketplaceCategory(progress.newChild(1));
		return catalogCategory;
	}

	public void refreshUserFavorites(IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_FavoritesRefreshing, 1001);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			List<CatalogItem> items = catalogCategory.getItems();
			if (hasUserFavoritesService()) {
				Map<String, INode> nodes = new HashMap<String, INode>();
				for (CatalogItem item : items) {
					Object data = item.getData();
					if (data instanceof INode) {
						INode node = (INode) data;
						nodes.put(node.getId(), node);
					}
				}
				if (nodes.isEmpty()) {
					return;
				}
				try {
					applyShellProvider();
					marketplaceService.userFavorites(new ArrayList<INode>(nodes.values()), progress.newChild(500));
					for (CatalogItem catalogItem : items) {
						if (catalogItem instanceof MarketplaceNodeCatalogItem) {
							MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) catalogItem;
							INode node = nodes.get(nodeItem.getId());
							nodeItem.setUserFavorite(node == null ? null : node.getUserFavorite());
						}
					}
				} catch (NotAuthorizedException e) {
					catalogCategory = addPopularItems(progress.newChild(500));
					addUserStorageLoginItem(catalogCategory, e.getLocalizedMessage());
				} catch (UnsupportedOperationException ex) {
					catalogCategory = addPopularItems(progress.newChild(500));
					addFavoritesNotSupportedItem(catalogCategory);
				} catch (Exception ex) {
					//FIXME we should use the wizard page's status line to show errors, but that's unreachable from here...
					MarketplaceClientCore.error(Messages.MarketplaceDiscoveryStrategy_FavoritesRetrieveError, ex);
					addRetryErrorItem(catalogCategory, ex);
				}
			} else {
				for (CatalogItem catalogItem : items) {
					if (catalogItem instanceof MarketplaceNodeCatalogItem) {
						MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) catalogItem;
						nodeItem.setUserFavorite(null);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void addUserStorageLoginItem(MarketplaceCategory catalogCategory, String authMessage) {
		addUserActionItem(catalogCategory, UserAction.LOGIN, authMessage);
	}

	private void addNoFavoritesItem(MarketplaceCategory catalogCategory) {
		addUserActionItem(catalogCategory, UserAction.CREATE_FAVORITES);
	}

	private void addFavoritesNotSupportedItem(MarketplaceCategory catalogCategory) {
		addUserActionItem(catalogCategory, UserAction.FAVORITES_UNSUPPORTED);
	}

	private void addRetryErrorItem(MarketplaceCategory catalogCategory, Exception ex) {
		addUserActionItem(catalogCategory, UserAction.RETRY_ERROR, ex);
	}

	public void addOpenFavoritesItem(MarketplaceCategory catalogCategory) {
		addUserActionItem(catalogCategory, UserAction.OPEN_FAVORITES);
	}

	public void installed(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_findingInstalled,
				1000);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.INSTALLED);
			SearchResult result = computeInstalled(progress.newChild(500));
			if (!monitor.isCanceled()) {
				handleSearchResult(catalogCategory, result, progress.newChild(500));
			}
		} finally {
			monitor.done();
		}
	}

	protected SearchResult computeInstalled(IProgressMonitor monitor) throws CoreException {
		SearchResult result = new SearchResult();
		result.setNodes(new ArrayList<Node>());
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_ComputingInstalled, 1000);
		Map<String, IInstallableUnit> installedIUs = computeInstalledIUs(progress.newChild(500));
		if (!monitor.isCanceled()) {
			Set<INode> catalogNodes = marketplaceInfo.computeInstalledNodes(catalogDescriptor.getUrl(), installedIUs);
			if (!catalogNodes.isEmpty()) {
				List<INode> resolvedNodes = marketplaceService.getNodes(catalogNodes, progress.newChild(490));
				SubMonitor nodeProgress = SubMonitor.convert(progress.newChild(10), resolvedNodes.size());
				for (INode node : resolvedNodes) {
					//compute real installed state based on optional/required state
					if (marketplaceInfo.computeInstalled(installedIUs.keySet(), node)) {
						result.getNodes().add((Node) node);
					}
					nodeProgress.worked(1);
				}
			} else {
				monitor.worked(500);
			}
		}
		return result;
	}

	public void performQuery(IProgressMonitor monitor, Set<String> nodeIds) throws CoreException {
		Set<INode> nodes = new HashSet<INode>();
		for (String nodeId : nodeIds) {
			Node node = new Node();
			node.setId(nodeId);
			nodes.add(node);
		}
		performNodeQuery(monitor, nodes);
	}

	public void performNodeQuery(IProgressMonitor monitor, Set<? extends INode> nodes) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_searchingMarketplace,
				1001);
		try {
			MarketplaceCategory catalogCategory = findMarketplaceCategory(progress.newChild(1));
			catalogCategory.setContents(Contents.QUERY);
			SearchResult result = new SearchResult();
			result.setNodes(new ArrayList<Node>());
			if (!monitor.isCanceled()) {
				if (!nodes.isEmpty()) {
					List<INode> resolvedNodes = marketplaceService.getNodes(nodes, progress.newChild(500));
					for (INode node : resolvedNodes) {
						result.getNodes().add((Node) node);
					}
				} else {
					progress.setWorkRemaining(500);
				}
				result.setMatchCount(result.getNodes().size());
				handleSearchResult(catalogCategory, result, progress.newChild(500));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * @deprecated use {@link #computeInstalledIUs(IProgressMonitor)} instead
	 */
	@Deprecated
	protected Set<String> computeInstalledFeatures(IProgressMonitor monitor) {
		return computeInstalledIUs(monitor).keySet();
	}

	protected synchronized Map<String, IInstallableUnit> computeInstalledIUs(IProgressMonitor monitor) {
		if (featureIUById == null) {
			featureIUById = MarketplaceClientUi.computeInstalledIUsById(monitor);
		}
		return featureIUById;
	}

	protected MarketplaceCategory findMarketplaceCategory(IProgressMonitor monitor) throws CoreException {
		MarketplaceCategory catalogCategory = null;

		SubMonitor progress = SubMonitor.convert(monitor, Messages.MarketplaceDiscoveryStrategy_catalogCategory, 10000);
		try {
			for (CatalogCategory candidate : getCategories()) {
				if (candidate.getSource() == source) {
					catalogCategory = (MarketplaceCategory) candidate;
				}
			}

			if (catalogCategory == null) {
				List<? extends IMarket> markets = marketplaceService.listMarkets(progress.newChild(10000));

				// marketplace has markets and categories, however a node and/or category can appear in multiple
				// markets.  This doesn't match well with discovery's concept of a category.  Discovery requires all
				// items to be in a category, so we use a single root category and tagging.
				catalogCategory = new MarketplaceCategory();
				catalogCategory.setId("<root>"); //$NON-NLS-1$
				catalogCategory.setName("<root>"); //$NON-NLS-1$
				catalogCategory.setSource(source);

				catalogCategory.setMarkets(markets);

				categories.add(catalogCategory);
			}
		} finally {
			progress.done();
		}
		return catalogCategory;
	}

	public INews performNewsDiscovery(IProgressMonitor monitor) throws CoreException {
		return marketplaceService.news(monitor);
	}

	public void installErrorReport(IProgressMonitor monitor, IStatus result, Set<CatalogItem> items,
			IInstallableUnit[] operationIUs, String resolutionDetails) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				Messages.MarketplaceDiscoveryStrategy_sendingErrorNotification, 100);
		try {
			Set<Node> nodes = new HashSet<Node>();
			for (CatalogItem item : items) {
				Object data = item.getData();
				if (data instanceof INode) {
					nodes.add((Node) data);
				}
			}
			Set<String> iuIdsAndVersions = new HashSet<String>();
			for (IInstallableUnit iu : operationIUs) {
				String id = iu.getId();
				String version = iu.getVersion() == null ? null : iu.getVersion().toString();
				iuIdsAndVersions.add(id + "," + version); //$NON-NLS-1$
			}
			marketplaceService.reportInstallError(result, nodes, iuIdsAndVersions, resolutionDetails, progress);
		} finally {
			progress.done();
		}
	}

	public IMarketplaceService getMarketplaceService() {
		return marketplaceService;
	}

	protected MarketplaceCatalogSource getCatalogSource() {
		return source;
	}

	public void setShellProvider(IShellProvider shellProvider) {
		this.shellProvider = shellProvider;
		applyShellProvider();
	}

	public IShellProvider getShellProvider() {
		return shellProvider;
	}
}
