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

import org.eclipse.epp.mpc.rest.api.impl.ImmutablePlatformInfo;
import org.immutables.value.Value.Immutable;

@Immutable
public interface PlatformInfo {

	PlatformInfo ALL = builder().build();

	Optional<String> product();

	Optional<Float> platformVersion();

	Optional<String> javaVersion();

	Optional<String> os();

	PlatformInfo withProduct(String product);

	PlatformInfo withPlatformVersion(float version);

	PlatformInfo withJavaVersion(String version);

	PlatformInfo withOs(String os);

	default Builder toBuilder() {
		return Builder.from(this);
	}

	static Builder builder() {
		return Builder.create();
	}

	static interface Builder {

		static Builder create() {
			return ImmutablePlatformInfo.builder();
		}

		static Builder from(PlatformInfo instance) {
			return ImmutablePlatformInfo.builder().from(instance);
		}

		Builder product(Optional<String> product);

		Builder platformVersion(Optional<Float> version);

		Builder javaVersion(Optional<String> version);

		Builder os(Optional<String> os);

		Builder product(String product);

		Builder platformVersion(float version);

		Builder javaVersion(String version);

		Builder os(String os);

		PlatformInfo build();
	}
}
