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

import org.eclipse.epp.mpc.rest.api.impl.ImmutableListingQuery;
import org.eclipse.epp.mpc.rest.model.LicenseType;
import org.immutables.value.Value.Immutable;

@Immutable
public interface ListingQuery {
	Optional<Integer> marketId();

	Optional<Integer> categoryId();

	List<LicenseType> licenseType();

	Optional<String> query();

	List<String> tags();

	List<Integer> ids();

	Optional<PlatformInfo> platform();

	ListingQuery withMarketId(int id);

	ListingQuery withCategoryId(int id);

	ListingQuery withLicenseType(LicenseType... types);

	ListingQuery withLicenseType(Iterable<? extends LicenseType> types);

	ListingQuery withQuery(String query);

	ListingQuery withTags(String... tags);

	ListingQuery withTags(Iterable<String> tags);

	ListingQuery withIds(int... ids);

	ListingQuery withIds(Iterable<Integer> ids);

	ListingQuery withPlatform(PlatformInfo platform);

	default Builder toBuilder() {
		return Builder.from(this);
	}

	static Builder builder() {
		return Builder.create();
	}

	static interface Builder {

		static Builder create() {
			return ImmutableListingQuery.builder();
		}

		static Builder from(ListingQuery instance) {
			return ImmutableListingQuery.builder().from(instance);
		}

		Builder marketId(int id);

		Builder marketId(Optional<Integer> id);

		Builder categoryId(int id);

		Builder categoryId(Optional<Integer> id);

		Builder query(String query);

		Builder query(Optional<String> query);

		Builder licenseType(Iterable<? extends LicenseType> types);

		Builder addLicenseType(LicenseType type);

		Builder addLicenseType(LicenseType... types);

		Builder addAllLicenseType(Iterable<? extends LicenseType> types);

		Builder tags(Iterable<String> tags);

		Builder addTags(String tag);

		Builder addTags(String... tags);

		Builder addAllTags(Iterable<String> tags);

		Builder ids(Iterable<Integer> tags);

		Builder addIds(int tag);

		Builder addIds(int... tags);

		Builder addAllIds(Iterable<Integer> tags);

		Builder platform(PlatformInfo platform);

		Builder platform(Optional<? extends PlatformInfo> platform);

		ListingQuery build();
	}
}
