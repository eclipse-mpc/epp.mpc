/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

/**
 * @author David Green
 */
public abstract class NodeListing {
	
	protected Integer count;
	protected java.util.List<Node> node = new java.util.ArrayList<Node>();
	
	public NodeListing() {
	}
	
	/**
	 * The number of items that were matched for the node listing, which may be different than the number of nodes included in the response.
	 */
	public Integer getCount() {
		return count;
	}
	
	public void setCount(Integer count) {
		this.count = count;
	}
	
	public java.util.List<Node> getNode() {
		return node;
	}
	
	public void setNode(java.util.List<Node> node) {
		this.node = node;
	}
	
}
