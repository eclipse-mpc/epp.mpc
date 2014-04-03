/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.model;

/**
 * Branding information for a marketplace catalog entry.
 *
 * @see ICatalog
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICatalogBranding extends IIdentifiable {

	String getWizardIcon();

	boolean hasSearchTab();

	String getSearchTabName();

	boolean hasPopularTab();

	String getPopularTabName();

	boolean hasRecentTab();

	String getRecentTabName();

	String getWizardTitle();

}