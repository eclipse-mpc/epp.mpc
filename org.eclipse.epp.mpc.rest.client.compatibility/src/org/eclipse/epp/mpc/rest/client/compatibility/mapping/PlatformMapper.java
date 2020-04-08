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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.epp.internal.mpc.core.model.Platforms;
import org.eclipse.epp.mpc.core.model.IPlatforms;
import org.eclipse.epp.mpc.rest.model.ListingVersion;
import org.mapstruct.Mapper;

@SuppressWarnings("restriction")
@Mapper
public class PlatformMapper extends AbstractMapper {

	public IPlatforms toPlatforms(List<ListingVersion> versions) {
		return toPlatformsInternal(versions);
	}

	Platforms toPlatformsInternal(List<ListingVersion> versions) {
		List<String> allPlatforms = versions.stream()
				.flatMap(v -> v.getPlatforms().stream())
				.distinct()
				.collect(Collectors.toList());
		Platforms platforms = new Platforms();
		platforms.setPlatform(allPlatforms);
		return platforms;
	}
}
