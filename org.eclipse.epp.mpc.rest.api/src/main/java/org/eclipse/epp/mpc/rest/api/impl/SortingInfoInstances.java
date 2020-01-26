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
package org.eclipse.epp.mpc.rest.api.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.epp.mpc.rest.api.SortingInfo;
import org.eclipse.epp.mpc.rest.api.SortingInfo.Builder;
import org.eclipse.epp.mpc.rest.model.SortWhitelist;

public final class SortingInfoInstances {

	public static final SortingInfo FEATURED;

	public static final SortingInfo UNSORTED;

	private static final Map<SortWhitelist, SortingInfo> SORTED;

	static {
		FEATURED = SortingInfo.builder().sort(Optional.empty()).featured(true).build();
		Builder sortedBuilder = SortingInfo.builder().featured(false);
		UNSORTED = sortedBuilder.build();
		SORTED = new EnumMap<>(SortWhitelist.class);
		for (SortWhitelist sort : SortWhitelist.values()) {
			SORTED.put(sort, sortedBuilder.sort(sort).build());
		}
	}

	public static SortingInfo featured() {
		return FEATURED;
	}

	public static SortingInfo unsorted() {
		return UNSORTED;
	}

	public static SortingInfo sorted(SortWhitelist sort) {
		return SORTED.get(sort);
	}

	public static SortingInfo interned(SortingInfo build) {
		if (build.featured()) {
			if (build.sort().isPresent()) {
				return build;
			}
			return FEATURED;
		}
		return build.sort().map(SortingInfoInstances::sorted).orElse(UNSORTED);
	}

}
