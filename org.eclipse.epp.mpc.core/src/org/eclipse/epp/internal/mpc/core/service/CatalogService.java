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
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.model.Catalog;
import org.eclipse.epp.mpc.core.service.ICatalogService;

/**
 * @deprecated Use {@link org.eclipse.epp.mpc.core.service.ICatalogService} instead.
 */
@Deprecated
public interface CatalogService extends ICatalogService {

	@Override
	public List<Catalog> listCatalogs(IProgressMonitor monitor) throws CoreException;

}
