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

import org.eclipse.epp.mpc.rest.api.impl.ImmutablePagingInfo;
import org.immutables.value.Value.Immutable;

@Immutable
public interface PagingInfo {

	int page();

	int limit();

	PagingInfo withPage(int page);

	PagingInfo withLimit(int limit);

	default PagingInfo nextPage() {
		return withPage(page() + 1);
	}

	default PagingInfo previousPage() {
		return page() == 0 ? null : withPage(page() - 1);
	}

	default Builder toBuilder() {
		return Builder.from(this);
	}

	static Builder builder() {
		return Builder.create();
	}

	static interface Builder {

		static Builder create() {
			return ImmutablePagingInfo.builder();
		}

		static Builder from(PagingInfo instance) {
			return ImmutablePagingInfo.builder().from(instance);
		}

		Builder page(int page);

		Builder limit(int limit);

		PagingInfo build();
	}
}
