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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.mapstruct.Mapper;

@Mapper
public abstract class SimpleTypesMapper extends AbstractMapper {

	public Date toDate(Long epoch) {
		return epoch == null ? null
				: new Calendar.Builder().setTimeZone(TimeZone.getDefault()).setInstant(epoch).build().getTime();
	}
}
