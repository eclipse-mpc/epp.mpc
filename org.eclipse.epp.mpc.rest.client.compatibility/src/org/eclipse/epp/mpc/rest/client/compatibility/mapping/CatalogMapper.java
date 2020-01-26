/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.rest.client.compatibility.mapping;

import org.eclipse.epp.internal.mpc.core.model.CatalogBranding;
import org.eclipse.epp.internal.mpc.core.model.News;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.model.ICatalogBranding;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.rest.model.Catalog;
import org.eclipse.epp.mpc.rest.model.CatalogTab.TypeEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper
public abstract class CatalogMapper extends AbstractMapper {

	public ICatalog toCatalog(Catalog catalog) {
		return toCatalogInternal(catalog);
	}

	public ICatalogBranding toCatalogBranding(Catalog catalog) {
		return toCatalogBrandingInternal(catalog);
	}

	public INews toNews(Catalog catalog) {
		return toNewsInternal(catalog);
	}

	@Mappings({
		@Mapping(source = "title", target = "name"),
		@Mapping(source = "catalog", target = "branding"),
		@Mapping(source = "dependenciesRepository", target = "dependencyRepository"),
		@Mapping(source = "icon", target = "imageUrl"),
		@Mapping(ignore = true, target = "news")
	})
	abstract org.eclipse.epp.internal.mpc.core.model.Catalog toCatalogInternal(Catalog catalog);

	News toNewsInternal(Catalog catalog) {
		return catalog.getTabs().stream().filter(tab -> tab.getType() == TypeEnum.EMBEDDED).limit(1).map(tab -> {
			News news = new News();
			news.setShortTitle(tab.getTitle());
			news.setUrl(tab.getUrl());
			return news;
		}).findFirst().orElse(null);
	}

	CatalogBranding toCatalogBrandingInternal(Catalog catalog) {
		CatalogBranding branding = new CatalogBranding();
		branding.setUrl(catalog.getUrl());
		branding.setId(String.valueOf(catalog.getId()));
		branding.setName(catalog.getTitle());
		branding.setWizardIcon(catalog.getIcon());
		branding.setWizardTitle(catalog.getTitle());

		branding.setFavoritesTabName("Favorites");
		branding.setHasFavoritesTab(true);
		branding.setFeaturedMarketTabName("Featured");
		branding.setHasFeaturedMarketTab(true);
		branding.setPopularTabName("Popular");
		branding.setHasPopularTab(true);
		branding.setRecentTabName("Recent");
		branding.setHasRecentTab(true);
		branding.setSearchTabName("Search");
		branding.setHasSearchTab(true);
		branding.setHasRelatedTab(false);
		return branding;
	}
}
