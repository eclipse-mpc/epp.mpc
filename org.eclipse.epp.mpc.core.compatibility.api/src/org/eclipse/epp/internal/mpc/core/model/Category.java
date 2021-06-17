/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import org.eclipse.epp.mpc.core.model.ICategory;


/**
 * @author David Green
 */
public class Category extends Identifiable implements ICategory {

	protected Integer count;

	protected java.util.List<Node> node = new java.util.ArrayList<>();

	public Category() {
	}

	@Override
	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	@Override
	public java.util.List<Node> getNode() {
		return node;
	}

	public void setNode(java.util.List<Node> node) {
		this.node = node;
	}

	@Override
	protected boolean equalsType(Object obj) {
		return obj instanceof Category;
	}
}
