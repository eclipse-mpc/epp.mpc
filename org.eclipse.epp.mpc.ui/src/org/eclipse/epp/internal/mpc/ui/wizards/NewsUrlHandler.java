/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epp.internal.mpc.core.service.Category;
import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;

/**
 * @author Carsten Reckord
 */
public class NewsUrlHandler extends MarketplaceUrlHandler implements LocationListener {

	private final Set<String> documentLinks = new HashSet<String>();

	private final NewsViewer viewer;

	public NewsUrlHandler(NewsViewer viewer) {
		this.viewer = viewer;
	}

	public void changed(LocationEvent event) {
		// Links should open in external browser.
		// Since explicit HREF targets interfere with that,
		// we'll just remove them.
		Object[] links = (Object[]) viewer.getBrowser().evaluate( //
				"var links = document.links;" + //$NON-NLS-1$
				"var hrefs = Array();" + //$NON-NLS-1$
				"for (var i=0; i<links.length; i++) {" + //$NON-NLS-1$
				"   links[i].target='_self';" + //$NON-NLS-1$
				"   hrefs[i]=links[i].href;" + //$NON-NLS-1$
				"};" + //$NON-NLS-1$
				"return hrefs"); //$NON-NLS-1$

		// Remember document links for navigation handling since we
		// don't want to deal with URLs from dynamic loading events
		documentLinks.clear();
		for (Object link : links) {
			documentLinks.add(link.toString());
		}
	}

	public void changing(LocationEvent event) {
		if (!event.doit) {
			return;
		}
		String newLocation = event.location;
		boolean handled = handleUri(newLocation);
		if (handled) {
			event.doit = false;
		} else {
			String currentLocation = viewer.getBrowser().getUrl();
			if (isNavigation(currentLocation, newLocation)) {
				event.doit = false;
				viewer.getWizard().openUrl(newLocation);
			}
		}
	}

	private boolean isNavigation(String currentLocation, String newLocation) {
		if (eq(currentLocation, newLocation) || newLocation.startsWith("javascript:") || //$NON-NLS-1$
				"about:blank".equals(newLocation) || "about:blank".equals(currentLocation)) { //$NON-NLS-1$//$NON-NLS-2$
			return false;
		}
		if (!documentLinks.contains(newLocation)) {
			return false;
		}
		return !isSameLocation(currentLocation, newLocation);
	}

	static boolean isSameLocation(String currentLocation, String newLocation) {
		try {
			URI currentUri = new URI(currentLocation);
			URI newUri = new URI(newLocation);
			return equalsIgnoreFragment(currentUri, newUri);
		} catch (URISyntaxException e) {
			return false;
		}
	}

	static boolean equalsIgnoreFragment(URI currentLocation, URI newLocation) {
		return eq(currentLocation.getHost(), newLocation.getHost())
				&& eq(currentLocation.getPath(), newLocation.getPath())
				&& currentLocation.getPort() == newLocation.getPort()
				&& eq(currentLocation.getAuthority(), newLocation.getAuthority())
				&& eq(currentLocation.getScheme(), newLocation.getScheme())
				&& eq(currentLocation.getQuery(), newLocation.getQuery());
	}

	static boolean eq(String s1, String s2) {
		return s1 == s2 || (s1 != null && s1.equals(s2));
	}

	@Override
	protected boolean handleSearch(CatalogDescriptor descriptor, String url, String searchString,
			Map<String, String> params) {
		MarketplaceWizard marketplaceWizard = viewer.getWizard();

		String filterParam = params.get("filter"); //$NON-NLS-1$
		String[] filters = filterParam.split(" "); //$NON-NLS-1$
		Category searchCategory = null;
		Market searchMarket = null;
		for (String filter : filters) {
			if (filter.startsWith("tid:")) { //$NON-NLS-1$
				String id = filter.substring("tid:".length()); //$NON-NLS-1$
				List<CatalogCategory> catalogCategories = marketplaceWizard.getCatalog().getCategories();
				for (CatalogCategory catalogCategory : catalogCategories) {
					if (catalogCategory instanceof MarketplaceCategory) {
						MarketplaceCategory marketplaceCategory = (MarketplaceCategory) catalogCategory;
						List<Market> markets = marketplaceCategory.getMarkets();
						for (Market market : markets) {
							if (id.equals(market.getId())) {
								searchMarket = market;
							} else {
								final List<Category> categories = market.getCategory();
								for (Category category : categories) {
									if (id.equals(category.getId())) {
										searchCategory = category;
									}
								}
							}
						}
					}
				}
			}
		}

		marketplaceWizard.getCatalogPage().search(descriptor, searchMarket, searchCategory, searchString);
		return true;
	}

	@Override
	protected boolean handleRecent(CatalogDescriptor descriptor, String url) {
		viewer.getWizard().getCatalogPage().show(descriptor, MarketplaceViewer.ContentType.RECENT);
		return true;
	}

	@Override
	protected boolean handlePopular(CatalogDescriptor descriptor, String url) {
		viewer.getWizard().getCatalogPage().show(descriptor, MarketplaceViewer.ContentType.POPULAR);
		return true;
	}

	@Override
	protected boolean handleNode(CatalogDescriptor descriptor, String url, Node node) {
		viewer.getWizard().getCatalogPage().show(descriptor, Collections.singleton(node));
		return true;
	}

	@Override
	protected boolean handleInstallRequest(SolutionInstallationInfo installInfo, String url) {
		//TODO no new wizard
		triggerInstall(installInfo);
		return true;
	}
}