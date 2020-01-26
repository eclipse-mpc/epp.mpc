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

import org.eclipse.epp.mpc.rest.api.impl.ImmutableSortingInfo;
import org.eclipse.epp.mpc.rest.api.impl.SortingInfoInstances;
import org.eclipse.epp.mpc.rest.model.SortWhitelist;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;

@Immutable
public interface SortingInfo {

	Optional<SortWhitelist> sort();

	boolean featured();

	SortingInfo withSort(SortWhitelist value);

	SortingInfo withSort(Optional<? extends SortWhitelist> optional);

	default SortingInfo withoutSort() {
		return withSort(Optional.empty());
	}

	SortingInfo withFeatured(boolean value);

	default SortingInfo withFeatured() {
		return withFeatured(true);
	}

	default SortingInfo withoutFeatured() {
		return withFeatured(false);
	}

	@Check
	default SortingInfo normalize() {
		return SortingInfoInstances.interned(this);
	}

	public static SortingInfo unsortedInstance() {
		return SortingInfoInstances.unsorted();
	}

	public static SortingInfo sortedInstance(SortWhitelist sort) {
		return SortingInfoInstances.sorted(sort);
	}

	public static SortingInfo featuredInstance() {
		return SortingInfoInstances.FEATURED;
	}

	default Builder toBuilder() {
		return Builder.from(this);
	}

	static Builder builder() {
		return Builder.create();
	}

	static interface Builder {

		static Builder create() {
			return ImmutableSortingInfo.builder();
		}

		static Builder from(SortingInfo instance) {
			return ImmutableSortingInfo.builder().from(instance);
		}

		default Builder notSorted() {
			return sort(Optional.empty());
		}

		default Builder notFeatured() {
			return featured(false);
		}

		default Builder featured() {
			return featured(true);
		}

		Builder sort(Optional<? extends SortWhitelist> sort);

		Builder sort(SortWhitelist sort);

		Builder featured(boolean featured);

		SortingInfo build();
	}

}
