/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API, bug 461603: featured market
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 */
public class MarketplaceCatalogConfiguration extends CatalogConfiguration implements IMarketplaceClientConfiguration {
	private List<CatalogDescriptor> catalogDescriptors = new ArrayList<CatalogDescriptor>();

	private CatalogDescriptor catalogDescriptor;

	private String initialState;

	private Map<String, Operation> initialOperations;

	private ContentType initialContentType;

	public MarketplaceCatalogConfiguration() {
		setShowTagFilter(false);
		setShowInstalled(true);
		setShowInstalledFilter(false);
		setVerifyUpdateSiteAvailability(true);
		setShowCategories(false);
	}

	public MarketplaceCatalogConfiguration(IMarketplaceClientConfiguration configuration) {
		this();
		setCatalogDescriptors(configuration.getCatalogDescriptors());
		setCatalogDescriptor(configuration.getCatalogDescriptor());
		setInitialState(configuration.getInitialState());
		setInitialOperations(configuration.getInitialOperations());
	}

	public ContentType getInitialContentType() {
		return initialContentType;
	}

	public void setInitialContentType(ContentType initialContentType) {
		this.initialContentType = initialContentType;
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

	public void setInitialState(Object state) {
		if (state == null || state instanceof String) {
			String stateString = (String) state;
			setInitialState(stateString);
		} else {
			throw new IllegalArgumentException(NLS.bind(Messages.MarketplaceCatalogConfiguration_invalidStateObject,
					state));
		}
	}

	public void setInitialState(String initialState) {
		this.initialState = initialState;
	}

	public String getInitialState() {
		return initialState;
	}

	public Map<String, Operation> getInitialOperations() {
		return initialOperations == null ? null : Collections.unmodifiableMap(initialOperations);
	}

	public void setInitialOperations(Map<String, Operation> initialOperations) {
		this.initialOperations = new LinkedHashMap<String, Operation>(initialOperations);
	}

	/**
	 * @deprecated use {@link #getInitialOperations()} instead
	 */
	@Deprecated
	public Map<String, org.eclipse.epp.internal.mpc.ui.wizards.Operation> getInitialOperationByNodeId() {
		Map<String, org.eclipse.epp.internal.mpc.ui.wizards.Operation> map = org.eclipse.epp.internal.mpc.ui.wizards.Operation.mapAll(initialOperations);
		return map == null ? null : Collections.unmodifiableMap(map);
	}

	/**
	 * @deprecated use {@link #setInitialOperations(Map)} instead
	 */
	@Deprecated
	public void setInitialOperationByNodeId(
			Map<String, org.eclipse.epp.internal.mpc.ui.wizards.Operation> initialOperationByNodeId) {
		this.initialOperations = org.eclipse.epp.internal.mpc.ui.wizards.Operation.mapAllBack(initialOperationByNodeId);
	}
}
