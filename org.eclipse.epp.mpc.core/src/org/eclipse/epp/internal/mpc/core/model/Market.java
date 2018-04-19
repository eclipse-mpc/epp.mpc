/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import org.eclipse.epp.mpc.core.model.IMarket;


/**
 * @author David Green
 */
public class Market extends Identifiable implements IMarket {

	protected java.util.List<Category> category = new java.util.ArrayList<Category>();

	public Market() {
	}

	@Override
	public java.util.List<Category> getCategory() {
		return category;
	}

	public void setCategory(java.util.List<Category> category) {
		this.category = category;
	}

	@Override
	protected boolean equalsType(Object obj) {
		return obj instanceof IMarket;
	}

}
