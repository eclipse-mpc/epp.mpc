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
package org.eclipse.epp.mpc.rest.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.eclipse.epp.mpc.rest.api.impl.ImmutableRequestHelper;
import org.eclipse.epp.mpc.rest.model.Category;
import org.eclipse.epp.mpc.rest.model.Installs;
import org.eclipse.epp.mpc.rest.model.LicenseType;
import org.eclipse.epp.mpc.rest.model.Listing;
import org.eclipse.epp.mpc.rest.model.Market;
import org.eclipse.epp.mpc.rest.model.SortWhitelist;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

@Immutable(singleton = true)
@Enclosing
public abstract class RequestHelper {

	public static RequestHelper of() {
		return ImmutableRequestHelper.of();
	}

	public abstract Optional<PlatformInfo> defaultPlatform();

	public abstract Optional<PagingInfo> defaultPage();

	public abstract Optional<SortingInfo> defaultSort();

	public abstract RequestHelper withDefaultPlatform(PlatformInfo platform);

	public abstract RequestHelper withDefaultPage(PagingInfo page);

	public abstract RequestHelper withDefaultSort(SortingInfo sort);

	public RequestHelper.Async async() {
		return RequestHelper.Async.builder()
				.defaultPlatform(defaultPlatform())
				.defaultPage(defaultPage())
				.defaultSort(defaultSort())
				.build();
	}

	public Listing getListing(ListingsApi api, String listingId) {
		return getListing(api, listingId, defaultPlatform().orElse(null));
	}

	public Listing getListing(ListingsApi api, String listingId, PlatformInfo platform) {
		return api.getListing(listingId, Parameters.platformVersion(platform), Parameters.javaVersion(platform),
				Parameters.os(platform));
	}

	public List<Listing> getListings(ListingsApi api, ListingQuery query) {
		return getListings(api, query, defaultSort().orElse(null), defaultPage().orElse(null));
	}

	public List<Listing> getListings(ListingsApi api, ListingQuery query, SortingInfo sort) {
		return getListings(api, query, sort, defaultPage().orElse(null));
	}

	public List<Listing> getListings(ListingsApi api, ListingQuery query, SortingInfo sort, PagingInfo page) {
		query = applyDefaultPlatform(query);
		return api.getListings(Parameters.marketId(query), Parameters.categoryId(query), Parameters.licenseType(query),
				Parameters.sort(sort), Parameters.featured(sort), Parameters.query(query), Parameters.ids(query),
				Parameters.tags(query), Parameters.platformVersion(query), Parameters.javaVersion(query),
				Parameters.os(query), Parameters.page(page), Parameters.limit(page));
	}

	public List<Market> getMarkets(MarketsApi api) {
		return getMarkets(api, defaultPage().orElse(null));
	}

	public List<Market> getMarkets(MarketsApi api, PagingInfo page) {
		return api.getMarkets(Parameters.page(page), Parameters.limit(page));
	}

	public Installs getInstalls(InstallsApi api, InstallsQuery query, PagingInfo page) {
		return query.version().isPresent()
				? api.getInstallsForVersion(Parameters.listingId(query), Parameters.versionId(query),
						Parameters.platformVersion(query), Parameters.javaVersion(query), Parameters.os(query),
						Parameters.country(query))
						: api.getInstalls(Parameters.listingId(query), Parameters.platformVersion(query),
						        Parameters.javaVersion(query), Parameters.os(query),
								Parameters.page(page), Parameters.limit(page));
	}

	public List<Category> getCategories(CategoriesApi api) {
		return getCategories(api, defaultPage().orElse(null));
	}

	public List<Category> getCategories(CategoriesApi api, PagingInfo page) {
		return api.getCategories(Parameters.page(page), Parameters.limit(page));
	}

	private ListingQuery applyDefaultPlatform(ListingQuery query) {
		return Parameters.applyDefaultPlatform(query, defaultPlatform());
	}

	public Builder toBuilder() {
		return Builder.from(this);
	}

	public static Builder builder() {
		return Builder.create();
	}

	public static interface Builder {

		static Builder create() {
			return ImmutableRequestHelper.builder();
		}

		static Builder from(RequestHelper instance) {
			ImmutableRequestHelper.builder();
			return Builder.from(instance);
		}

		Builder defaultPlatform(Optional<? extends PlatformInfo> platform);

		Builder defaultPage(Optional<? extends PagingInfo> page);

		Builder defaultSort(Optional<? extends SortingInfo> sort);

		Builder defaultPlatform(PlatformInfo platform);

		Builder defaultPage(PagingInfo page);

		Builder defaultSort(SortingInfo sort);

		RequestHelper build();
	}

	@Immutable(singleton = true)
	public static abstract class Async {

		public static RequestHelper.Async of() {
			return ImmutableRequestHelper.Async.of();
		}

		public abstract Optional<PlatformInfo> defaultPlatform();

		public abstract Optional<PagingInfo> defaultPage();

		public abstract Optional<SortingInfo> defaultSort();

		public abstract RequestHelper.Async withDefaultPlatform(PlatformInfo platform);

		public abstract RequestHelper.Async withDefaultPage(PagingInfo page);

		public abstract RequestHelper.Async withDefaultSort(SortingInfo sort);

		public RequestHelper sync() {
			return RequestHelper.builder()
					.defaultPlatform(defaultPlatform())
					.defaultPage(defaultPage())
					.defaultSort(defaultSort())
					.build();
		}

		public CompletionStage<Listing> getListing(ListingsApi.Async api, String listingId) {
			return getListing(api, listingId, defaultPlatform().orElse(null));
		}

		public CompletionStage<Listing> getListing(ListingsApi.Async api, String listingId, PlatformInfo platform) {
			return api.getListing(listingId, Parameters.platformVersion(platform), Parameters.javaVersion(platform),
					Parameters.os(platform));
		}

		public CompletionStage<List<Listing>> getListings(ListingsApi.Async api, ListingQuery query) {
			return getListings(api, query, defaultSort().orElse(null), defaultPage().orElse(null));
		}

		public CompletionStage<List<Listing>> getListings(ListingsApi.Async api, ListingQuery query, SortingInfo sort) {
			return getListings(api, query, sort, defaultPage().orElse(null));
		}

		public CompletionStage<List<Listing>> getListings(ListingsApi.Async api, ListingQuery query, SortingInfo sort,
				PagingInfo page) {
			query = applyDefaultPlatform(query);
			return api.getListings(Parameters.marketId(query), Parameters.categoryId(query),
					Parameters.licenseType(query), Parameters.sort(sort), Parameters.featured(sort),
					Parameters.query(query), Parameters.ids(query), Parameters.tags(query),
					Parameters.platformVersion(query), Parameters.javaVersion(query), Parameters.os(query),
					Parameters.page(page), Parameters.limit(page));
		}

		public CompletionStage<List<Market>> getMarkets(MarketsApi.Async api) {
			return getMarkets(api, defaultPage().orElse(null));
		}

		public CompletionStage<List<Market>> getMarkets(MarketsApi.Async api, PagingInfo page) {
			return api.getMarkets(Parameters.page(page), Parameters.limit(page));
		}

		public CompletionStage<Installs> getInstalls(InstallsApi.Async api, InstallsQuery query, PagingInfo page) {
			return query.version().isPresent()
					? api.getInstallsForVersion(Parameters.listingId(query), Parameters.versionId(query),
							Parameters.platformVersion(query), Parameters.javaVersion(query), Parameters.os(query),
							Parameters.country(query))
							: api.getInstalls(Parameters.listingId(query), Parameters.platformVersion(query),
							        Parameters.javaVersion(query), Parameters.os(query),
									Parameters.page(page), Parameters.limit(page));
		}

		public CompletionStage<List<Category>> getCategories(CategoriesApi.Async api) {
			return getCategories(api, defaultPage().orElse(null));
		}

		public CompletionStage<List<Category>> getCategories(CategoriesApi.Async api, PagingInfo page) {
			return api.getCategories(Parameters.page(page), Parameters.limit(page));
		}

		private ListingQuery applyDefaultPlatform(ListingQuery query) {
			return Parameters.applyDefaultPlatform(query, defaultPlatform());
		}

		public Builder toBuilder() {
			return Builder.from(this);
		}

		public static Builder builder() {
			return Builder.create();
		}

		public static interface Builder {

			static Builder create() {
				return ImmutableRequestHelper.Async.builder();
			}

			static Builder from(RequestHelper.Async instance) {
				return ImmutableRequestHelper.Async.builder().from(instance);
			}

			Builder defaultPlatform(Optional<? extends PlatformInfo> platform);

			Builder defaultPage(Optional<? extends PagingInfo> page);

			Builder defaultSort(Optional<? extends SortingInfo> sort);

			Builder defaultPlatform(PlatformInfo platform);

			Builder defaultPage(PagingInfo page);

			Builder defaultSort(SortingInfo sort);

			RequestHelper.Async build();
		}
	}

	private static class Parameters {
		static ListingQuery applyDefaultPlatform(ListingQuery query, Optional<PlatformInfo> platform) {
			if (!query.platform().isPresent() && platform.isPresent()) {
				return query.withPlatform(platform.get());
			}
			return query;
		}

		static String os(PlatformInfo platform) {
			return platform == null ? null : nullIfEmpty(platform.os());
		}

		static String javaVersion(PlatformInfo platform) {
			return platform == null ? null : nullIfEmpty(platform.javaVersion());
		}

		static Float platformVersion(PlatformInfo platform) {
			return platform == null ? null : platform.platformVersion().orElse(null);
		}

		static String os(ListingQuery query) {
			return query == null ? null : query.platform().map(Parameters::os).orElse(null);
		}

		static String javaVersion(ListingQuery query) {
			return query == null ? null : query.platform().map(Parameters::javaVersion).orElse(null);
		}

		static Float platformVersion(ListingQuery query) {
			return query == null ? null : query.platform().map(Parameters::platformVersion).orElse(null);
		}

		static List<String> tags(ListingQuery query) {
			return query == null ? null : nullIfEmpty(query.tags());
		}

		static List<String> ids(ListingQuery query) {
			return query == null ? null : nullIfEmpty(query.ids());
		}

		static String query(ListingQuery query) {
			return query == null ? null : nullIfEmpty(query.query());
		}

		static Boolean featured(SortingInfo sort) {
			return sort == null ? null : sort.featured() ? Boolean.TRUE : null;
		}

		static SortWhitelist sort(SortingInfo sort) {
			return sort == null || sort.featured() ? null : sort.sort().orElse(null);
		}

		static List<LicenseType> licenseType(ListingQuery query) {
			return query == null ? null : nullIfEmpty(query.licenseType());
		}

		static String categoryId(ListingQuery query) {
			return query == null ? null : query.categoryId().orElse(null);
		}

		static String marketId(ListingQuery query) {
			return query == null ? null : query.marketId().orElse(null);
		}

		static String versionId(InstallsQuery query) {
			return query == null ? null : query.version().orElse(null);
		}

		static String os(InstallsQuery query) {
			return query == null ? null : query.platform().map(Parameters::os).orElse(null);
		}

		static String javaVersion(InstallsQuery query) {
			return query == null ? null : query.platform().map(Parameters::javaVersion).orElse(null);
		}

		static Float platformVersion(InstallsQuery query) {
			return query == null ? null : query.platform().map(Parameters::platformVersion).orElse(null);
		}

		static String listingId(InstallsQuery query) {
			return query == null ? null : query.listingId();
		}

		static String country(InstallsQuery query) {
			return query == null ? null : query.country().orElse(null);
		}

		static Integer page(PagingInfo page) {
			return page == null ? null : page.page();
		}

		static Integer limit(PagingInfo page) {
			return page == null ? null : page.limit();
		}

		private static <T> List<T> nullIfEmpty(List<T> list) {
			return list == null || list.isEmpty() ? null : list;
		}

		private static String nullIfEmpty(Optional<String> optional) {
			return optional.filter(String::isEmpty).orElse(null);
		}
	}
}
