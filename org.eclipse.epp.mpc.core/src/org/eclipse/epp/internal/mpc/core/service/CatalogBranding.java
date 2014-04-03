/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import org.eclipse.epp.mpc.core.model.ICatalogBranding;


/**
 * @author Benjamin Muskalla
 */
public class CatalogBranding extends Identifiable implements ICatalogBranding {

	private String wizardIcon;

	private String wizardTitle;

	private boolean hasSearchTab;

	private String searchTabName;

	private boolean hasPopularTab;

	private String popularTabName;

	private boolean hasRecentTab;

	private String recentTabName;

	public String getWizardIcon() {
		return wizardIcon;
	}

	public void setWizardIcon(String wizardIcon) {
		this.wizardIcon = wizardIcon;
	}

	public boolean hasSearchTab() {
		return hasSearchTab;
	}

	public void setHasSearchTab(boolean hasSearchTab) {
		this.hasSearchTab = hasSearchTab;
	}

	public String getSearchTabName() {
		return searchTabName;
	}

	public void setSearchTabName(String searchTabName) {
		this.searchTabName = searchTabName;
	}

	public boolean hasPopularTab() {
		return hasPopularTab;
	}

	public void setHasPopularTab(boolean hasPopularTab) {
		this.hasPopularTab = hasPopularTab;
	}

	public String getPopularTabName() {
		return popularTabName;
	}

	public void setPopularTabName(String popularTabName) {
		this.popularTabName = popularTabName;
	}

	public boolean hasRecentTab() {
		return hasRecentTab;
	}

	public void setHasRecentTab(boolean hasRecentTab) {
		this.hasRecentTab = hasRecentTab;
	}

	public String getRecentTabName() {
		return recentTabName;
	}

	public void setRecentTabName(String recentTabName) {
		this.recentTabName = recentTabName;
	}

	public String getWizardTitle() {
		return wizardTitle;
	}

	public void setWizardTitle(String wizardTitle) {
		this.wizardTitle = wizardTitle;
	}

	@Override
	protected boolean equalsType(Object obj) {
		return obj instanceof ICatalogBranding;
	}
}