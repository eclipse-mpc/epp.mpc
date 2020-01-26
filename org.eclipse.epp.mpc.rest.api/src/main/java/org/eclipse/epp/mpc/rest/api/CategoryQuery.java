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

import org.eclipse.epp.mpc.rest.api.impl.ImmutableCategoryQuery;
import org.immutables.value.Value.Immutable;

@Immutable
public interface CategoryQuery {
	Optional<Integer> marketId();

	Optional<Integer> categoryId();

	CategoryQuery withMarketId(int id);

	CategoryQuery withCategoryId(int id);

	default Builder toBuilder() {
		return Builder.from(this);
	}

	static Builder builder() {
		return Builder.create();
	}

	static interface Builder {

		static Builder create() {
			return ImmutableCategoryQuery.builder();
		}

		static Builder from(CategoryQuery instance) {
			return ImmutableCategoryQuery.builder().from(instance);
		}

		Builder marketId(Optional<Integer> id);

		Builder categoryId(Optional<Integer> id);

		Builder marketId(int id);

		Builder categoryId(int id);

		CategoryQuery build();
	}

}
