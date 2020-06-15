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

import java.util.Optional;

import org.eclipse.epp.mpc.rest.api.impl.ImmutableInstallsQuery;
import org.immutables.value.Value.Immutable;

@Immutable
public interface InstallsQuery {

	String listingId();

	Optional<String> country();

	Optional<String> version();

	Optional<PlatformInfo> platform();

	InstallsQuery withListingId(String id);

	InstallsQuery withCountry(String country);

	InstallsQuery withVersion(String versionId);

	InstallsQuery withPlatform(PlatformInfo platform);


	default Builder toBuilder() {
		return Builder.from(this);
	}

	static Builder builder() {
		return Builder.create();
	}

	static interface Builder {

		static Builder create() {
			return ImmutableInstallsQuery.builder();
		}

		static Builder from(InstallsQuery instance) {
			return ImmutableInstallsQuery.builder().from(instance);
		}

		Builder listingId(String id);

		Builder country(Optional<String> country);

		Builder version(Optional<String> versionId);

		Builder platform(Optional<? extends PlatformInfo> platform);

		Builder country(String country);

		Builder version(String version);

		Builder platform(PlatformInfo platform);

		InstallsQuery build();
	}
}
