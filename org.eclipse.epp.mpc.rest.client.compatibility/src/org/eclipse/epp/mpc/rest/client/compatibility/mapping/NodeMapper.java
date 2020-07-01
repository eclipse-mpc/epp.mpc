/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution
and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.rest.client.compatibility.mapping;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.internal.mpc.core.model.SearchResult;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.rest.client.compatibility.util.ListingVersionUtil;
import org.eclipse.epp.mpc.rest.model.Account;
import org.eclipse.epp.mpc.rest.model.Listing;
import org.eclipse.epp.mpc.rest.model.ListingVersion;
import org.eclipse.epp.mpc.rest.model.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@SuppressWarnings("restriction")
@Mapper(uses = { IuMapper.class, PlatformMapper.class, TagMapper.class, CategoryMapper.class,
		SimpleTypesMapper.class })
public abstract class NodeMapper extends AbstractMapper {

	public INode toNode(Listing listing) {
		return toNodeInternal(listing);
	}

	public ISearchResult toSearchResult(List<Listing> listings) {
		return toSearchResultInternal(listings);
	}

	@Mappings({ @Mapping(source = "title", target = "name"),
		@Mapping(ignore = true, target = "url"),
		@Mapping(source = "listing", target = "companyname", qualifiedByName = "NodeCompanyName"),
		@Mapping(source = "versions", target = "eclipseversion", qualifiedByName = "NodeEclipseVersions"),
		@Mapping(source = "favoriteCount", target = "favorited"),
		@Mapping(source = "foundationMember", target = "foundationmember"),
		@Mapping(source = "homepageUrl", target = "homepageurl"),
		@Mapping(source = "logo", target = "image"),
		@Mapping(source = "installCountRecent", target = "installsRecent"),
		@Mapping(source = "installCount", target = "installsTotal"),
		@Mapping(source = "versions", target = "ius"),
		@Mapping(source = "listing", target = "owner", qualifiedByName = "NodeOwner"),
		@Mapping(source = "versions", target = "platforms"),
		@Mapping(ignore = true, target = "screenshot"),
		@Mapping(source = "teaser", target = "shortdescription"),
		@Mapping(source = "supportUrl", target = "supporturl"),
		@Mapping(constant = "resource", target = "type"),
		@Mapping(source = "versions", target = "updateurl", qualifiedByName = "NodeUpdateUrl"),
		@Mapping(ignore = true, target = "userFavorite"),
		@Mapping(source = "versions", target = "version", qualifiedByName = "NodeVersion"),
		@Mapping(source = "licenseType", target = "license") })
	abstract Node toNodeInternal(Listing listing);

	SearchResult toSearchResultInternal(List<Listing> listings) {
		SearchResult result = new SearchResult();
		result.setNodes(mapAll(listings, this::toNodeInternal));
		result.setMatchCount(listings.size());//FIXME how to get total result size in face of paging !?
		return result;
	}

	@Named("NodeOwner")
	String authorsAndOrganizationsToOwner(Listing listing) {
		return Stream
				.concat(listing.getAuthors().stream().map(Account::getFullName),
						Stream.ofNullable(listing.getOrganization()).map(Organization::getName))
				.findFirst()
				.orElse(null);
	}

	@Named("NodeCompanyName")
	String organizationsToCompany(Listing listing) {
		return Optional.ofNullable(listing.getOrganization()).map(Organization::getName).orElse(null);
	}

	@Named("NodeVersion")
	String latestListingVersion(List<ListingVersion> versions) {
		return ListingVersionUtil.newestApplicableVersion(versions).map(ListingVersion::getVersion).orElse(null);
	}

	@Named("NodeEclipseVersions")
	String latestListingVersionEclipseVersions(List<ListingVersion> versions) {
		return ListingVersionUtil.newestApplicableVersion(versions)
				.map(sv -> sv.getEclipseVersions().stream().collect(Collectors.joining(","))) //$NON-NLS-1$
				.orElse(null);
	}

	@Named("NodeUpdateUrl")
	String latestListingVersionUpdateUrl(List<ListingVersion> versions) {
		return ListingVersionUtil.newestApplicableVersion(versions)
				.map(ListingVersion::getUpdateSiteUrl)
				.orElse(null);
	}

}
