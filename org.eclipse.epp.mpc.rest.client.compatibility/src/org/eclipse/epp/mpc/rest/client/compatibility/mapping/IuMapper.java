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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.epp.internal.mpc.core.model.Iu;
import org.eclipse.epp.internal.mpc.core.model.Ius;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.IIus;
import org.eclipse.epp.mpc.rest.client.compatibility.util.ListingVersionUtil;
import org.eclipse.epp.mpc.rest.model.Feature;
import org.eclipse.epp.mpc.rest.model.Feature.InstallStateEnum;
import org.eclipse.epp.mpc.rest.model.ListingVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@SuppressWarnings("restriction")
@Mapper
public abstract class IuMapper extends AbstractMapper {

	public IIu toIu(Feature feature) {
		return toIuInternal(feature);
	}

	public IIus toIus(List<ListingVersion> versions) {
		return toIusInternal(versions);
	}

	Ius toIusInternal(List<ListingVersion> versions) {
		List<IIu> ius = ListingVersionUtil.newestApplicableVersion(versions)
				.map(v -> v.getFeatureIds().stream().map(f -> toIu(f)).collect(Collectors.toList()))
				.orElse(Collections.emptyList());
		Ius result = new Ius();
		result.setIuElements(ius);
		return result;
	}

	@Mappings({ @Mapping(source = "featureId", target = "id"),
		@Mapping(source = "installState", target = "optional", qualifiedByName = "IuOptional"),
		@Mapping(source = "installState", target = "selected", qualifiedByName = "IuSelected") })
	abstract Iu toIuInternal(Feature feature);

	@Named("IuOptional")
	boolean installStateToOptional(InstallStateEnum installState) {
		return installState != InstallStateEnum.REQUIRED;
	}

	@Named("IuSelected")
	boolean installStateToSelected(InstallStateEnum installState) {
		return installState == InstallStateEnum.REQUIRED || installState == InstallStateEnum.OPTIONAL_SELECTED;
	}
}
