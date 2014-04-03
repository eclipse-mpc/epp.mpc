/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.mpc.ui;

import java.util.List;
import java.util.Map;

import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.ICatalogService;

/**
 * Configuration for launching the Marketplace Wizard using {@link IMarketplaceClientService}.
 *
 * @see IMarketplaceClientService
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMarketplaceClientConfiguration {
	/**
	 * @see #getCatalogDescriptors()
	 */
	void setCatalogDescriptors(List<CatalogDescriptor> descriptors);

	/**
	 * @see #getCatalogDescriptor()
	 */
	void setCatalogDescriptor(CatalogDescriptor initial);

	/**
	 * Valid objects for the initial state are only created by the marketplace wizard internally. Clients wishing to
	 * start the wizard in a defined state should refer to {@link #setInitialOperations(Map)} instead.
	 *
	 * @see #getInitialState()
	 */
	void setInitialState(Object state);

	/**
	 * @see #getInitialOperations()
	 */
	void setInitialOperations(Map<String, Operation> selection);

	/**
	 * The initial selection state applied to marketplace entries by {@link INode#getId() node id}.
	 */
	Map<String, Operation> getInitialOperations();

	/**
	 * The initial state applied to the Wizard. This will be merged with any additional {@link #getInitialOperations()
	 * initial operations}.
	 * <p>
	 * This is used internally to suspend and resume the wizard, e.g. to switch to an embedded web browser to show
	 * marketplace pages.
	 * <p>
	 * Valid objects for the initial state are only created by the marketplace wizard internally. Clients wishing to
	 * start the wizard in a defined state should refer to {@link #setInitialOperations(Map)} instead.
	 */
	Object getInitialState();

	/**
	 * The initially active catalog. By default, either the last active catalog from a previous launch or the first
	 * {@link #setCatalogDescriptors(List) available catalog} will be selected.
	 */
	CatalogDescriptor getCatalogDescriptor();

	/**
	 * The list of available Marketplace catalogs. These will be shown in the wizard's catalog selector. If no
	 * descriptors are set explicitly, a default list will be retrieved using the {@link ICatalogService}.
	 */
	List<CatalogDescriptor> getCatalogDescriptors();
}
