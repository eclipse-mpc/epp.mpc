/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;

/**
 * @author David Green
 */
public class MarketplaceCatalogConfiguration extends CatalogConfiguration {
	private List<CatalogDescriptor> catalogDescriptors = new ArrayList<CatalogDescriptor>();

	private CatalogDescriptor catalogDescriptor;

	private String initialState;

	private Map<String, Operation> initialOperationByNodeId;

	public MarketplaceCatalogConfiguration() {
		setShowTagFilter(false);
		setShowInstalled(true);
		setShowInstalledFilter(false);
		setVerifyUpdateSiteAvailability(true);
		setShowCategories(false);
	}

	public List<CatalogDescriptor> getCatalogDescriptors() {
		return catalogDescriptors;
	}

	public void setCatalogDescriptors(List<CatalogDescriptor> catalogDescriptors) {
		this.catalogDescriptors = catalogDescriptors;
	}

	public CatalogDescriptor getCatalogDescriptor() {
		return catalogDescriptor;
	}

	public void setCatalogDescriptor(CatalogDescriptor catalogDescriptor) {
		this.catalogDescriptor = catalogDescriptor;
	}

	public void setInitialState(String initialState) {
		this.initialState = initialState;
	}

	public String getInitialState() {
		return initialState;
	}

	public Map<String, Operation> getInitialOperationByNodeId() {
		return initialOperationByNodeId;
	}

	public void setInitialOperationByNodeId(Map<String, Operation> initialOperationByNodeId) {
		this.initialOperationByNodeId = initialOperationByNodeId;
	}

}
