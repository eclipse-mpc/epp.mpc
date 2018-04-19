/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.IIus;


/**
 * @author David Green
 */
public class Ius implements IIus {

	protected List<IIu> iuElements = new ArrayList<>();

	public Ius() {
	}

	@Override
	@Deprecated
	public List<String> getIu() {
		List<String> ius = new ArrayList<>();
		for (IIu iu : iuElements) {
			ius.add(iu.getId());
		}
		return ius;
	}

	public void setIuElements(List<IIu> iuElements) {
		this.iuElements = iuElements;
	}

	@Override
	public List<IIu> getIuElements() {
		return iuElements;
	}

}
