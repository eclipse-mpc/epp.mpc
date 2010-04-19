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
package org.eclipse.epp.internal.mpc.core.service;


/**
 * @author David Green
 */
public class Marketplace {
	
	protected java.util.List<Market> market = new java.util.ArrayList<Market>();
	protected java.util.List<Category> category = new java.util.ArrayList<Category>();
	protected java.util.List<Node> node = new java.util.ArrayList<Node>();
	protected Featured featured;
	protected Search search;
	protected Favorites favorites;
	protected Popular popular;
	protected Recent recent;
	
	public Marketplace() {
	}
	
	public java.util.List<Market> getMarket() {
		return market;
	}
	
	public void setMarket(java.util.List<Market> market) {
		this.market = market;
	}
	
	public java.util.List<Category> getCategory() {
		return category;
	}
	
	public void setCategory(java.util.List<Category> category) {
		this.category = category;
	}
	
	public java.util.List<Node> getNode() {
		return node;
	}
	
	public void setNode(java.util.List<Node> node) {
		this.node = node;
	}
	
	public Featured getFeatured() {
		return featured;
	}
	
	public void setFeatured(Featured featured) {
		this.featured = featured;
	}
	
	public Search getSearch() {
		return search;
	}
	
	public void setSearch(Search search) {
		this.search = search;
	}
	
	public Favorites getFavorites() {
		return favorites;
	}
	
	public void setFavorites(Favorites favorites) {
		this.favorites = favorites;
	}
	
	public Popular getPopular() {
		return popular;
	}
	
	public void setPopular(Popular popular) {
		this.popular = popular;
	}
	
	public Recent getRecent() {
		return recent;
	}
	
	public void setRecent(Recent recent) {
		this.recent = recent;
	}
	
}
