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

import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.rest.model.Market;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@SuppressWarnings("restriction")
@Mapper(uses = CategoryMapper.class)
public abstract class MarketMapper extends AbstractMapper {

	public IMarket toMarket(Market market) {
		return toMarketInternal(market);
	}

	@Mappings({ @Mapping(source = "title", target = "name"), @Mapping(source = "categories", target = "category") })
	abstract org.eclipse.epp.internal.mpc.core.model.Market toMarketInternal(Market market);
}
