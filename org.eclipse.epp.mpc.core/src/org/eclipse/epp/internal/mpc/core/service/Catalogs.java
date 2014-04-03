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

import org.eclipse.epp.mpc.core.model.ICatalogs;

/**
 * @author Benjamin Muskalla
 */
public class Catalogs implements ICatalogs {

	protected java.util.List<Catalog> catalogs = new java.util.ArrayList<Catalog>();

	public Catalogs() {
	}

	public java.util.List<Catalog> getCatalogs() {
		return catalogs;
	}

	public void setCatalogs(java.util.List<Catalog> catalogs) {
		this.catalogs = catalogs;
	}

}
