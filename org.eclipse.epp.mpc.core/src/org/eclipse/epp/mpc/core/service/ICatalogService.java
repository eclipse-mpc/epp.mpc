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
package org.eclipse.epp.mpc.core.service;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.mpc.core.model.ICatalog;

/**
 * Entry point to the marketplace API. A catalog service is used to retrieve a list of known catalogs that implement the
 * <a href="https://wiki.eclipse.org/Marketplace/REST">Marketplace REST API</a>. An instance of this class can be
 * retrieved through the {@link IMarketplaceServiceLocator} OSGi service.
 *
 * @see ICatalog
 * @see IMarketplaceService
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICatalogService {

	List<? extends ICatalog> listCatalogs(IProgressMonitor monitor) throws CoreException;

}