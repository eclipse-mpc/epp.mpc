/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API, bug 461603: featured market
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import org.eclipse.epp.mpc.core.model.ICatalogBranding;


/**
 * @author Benjamin Muskalla
 */
public class CatalogBranding extends Identifiable implements ICatalogBranding {

	private String wizardIcon;

	private String wizardTitle;

	private boolean hasSearchTab;

	private String searchTabName;

	private boolean hasPopularTab;

	private String popularTabName;

	private boolean hasRecentTab;

	private String recentTabName;

	private boolean hasRelatedTab;

	private String relatedTabName;

	private boolean hasFeaturedMarketTab;

	private String featuredMarketTabName;

	private boolean hasFavoritesTab;

	private String favoritesTabName;

	private String favoritesServer;

	private String favoritesApiKey;

	@Override
	public String getWizardIcon() {
		return wizardIcon;
	}

	public void setWizardIcon(String wizardIcon) {
		this.wizardIcon = wizardIcon;
	}

	@Override
	public boolean hasSearchTab() {
		return hasSearchTab;
	}

	public void setHasSearchTab(boolean hasSearchTab) {
		this.hasSearchTab = hasSearchTab;
	}

	@Override
	public String getSearchTabName() {
		return searchTabName;
	}

	public void setSearchTabName(String searchTabName) {
		this.searchTabName = searchTabName;
	}

	@Override
	public boolean hasPopularTab() {
		return hasPopularTab;
	}

	public void setHasPopularTab(boolean hasPopularTab) {
		this.hasPopularTab = hasPopularTab;
	}

	@Override
	public String getPopularTabName() {
		return popularTabName;
	}

	public void setPopularTabName(String popularTabName) {
		this.popularTabName = popularTabName;
	}

	@Override
	public boolean hasRecentTab() {
		return hasRecentTab;
	}

	public void setHasRecentTab(boolean hasRecentTab) {
		this.hasRecentTab = hasRecentTab;
	}

	@Override
	public String getRecentTabName() {
		return recentTabName;
	}

	public void setRecentTabName(String recentTabName) {
		this.recentTabName = recentTabName;
	}

	@Override
	public boolean hasRelatedTab() {
		return hasRelatedTab;
	}

	public void setHasRelatedTab(boolean hasRelatedTab) {
		this.hasRelatedTab = hasRelatedTab;
	}

	@Override
	public String getRelatedTabName() {
		return relatedTabName;
	}

	public void setRelatedTabName(String relatedTabName) {
		this.relatedTabName = relatedTabName;
	}

	@Override
	public boolean hasFeaturedMarketTab() {
		return hasFeaturedMarketTab;
	}

	public void setHasFeaturedMarketTab(boolean hasFeaturedMarketTab) {
		this.hasFeaturedMarketTab = hasFeaturedMarketTab;
	}

	@Override
	public String getFeaturedMarketTabName() {
		return featuredMarketTabName;
	}

	public void setFeaturedMarketTabName(String featuredMarketTabName) {
		this.featuredMarketTabName = featuredMarketTabName;
	}

	@Override
	public boolean hasFavoritesTab() {
		return hasFavoritesTab;
	}

	public void setHasFavoritesTab(boolean hasFavoritesTab) {
		this.hasFavoritesTab = hasFavoritesTab;
	}

	@Override
	public String getFavoritesTabName() {
		return favoritesTabName;
	}

	public void setFavoritesTabName(String favoritesTabName) {
		this.favoritesTabName = favoritesTabName;
	}

	@Override
	public String getFavoritesServer() {
		return favoritesServer;
	}

	public void setFavoritesServer(String favoritesServer) {
		this.favoritesServer = favoritesServer;
	}

	@Override
	public String getFavoritesApiKey() {
		return favoritesApiKey;
	}

	public void setFavoritesApiKey(String favoritesApiKey) {
		this.favoritesApiKey = favoritesApiKey;
	}

	@Override
	public String getWizardTitle() {
		return wizardTitle;
	}

	public void setWizardTitle(String wizardTitle) {
		this.wizardTitle = wizardTitle;
	}

	@Override
	protected boolean equalsType(Object obj) {
		return obj instanceof ICatalogBranding;
	}
}