/*******************************************************************************
 * Copyright (c) 2011, 2018 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import org.eclipse.epp.mpc.core.model.ITags;

/**
 * @author Benjamin Muskalla
 */
public class Tags implements ITags {

	protected java.util.List<Tag> tags = new java.util.ArrayList<>();

	public Tags() {
	}

	@Override
	public java.util.List<Tag> getTags() {
		return tags;
	}

}
