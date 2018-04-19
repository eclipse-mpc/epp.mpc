/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.INode;

public class FavoriteList extends Identifiable implements IFavoriteList {

	private List<INode> nodes = new ArrayList<>();

	private String owner;

	private String ownerProfileUrl;

	private String icon;

	@Override
	public String getOwner() {
		return owner == null ? getId() : owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public String getOwnerProfileUrl() {
		return ownerProfileUrl;
	}

	public void setOwnerProfileUrl(String ownerProfileUrl) {
		this.ownerProfileUrl = ownerProfileUrl;
	}

	@Override
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Override
	public List<INode> getNodes() {
		return nodes;
	}

	public void setNodes(List<INode> nodes) {
		this.nodes = nodes;
	}
}
