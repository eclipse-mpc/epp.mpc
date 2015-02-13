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
package org.eclipse.epp.internal.mpc.core.service.xml;


import org.eclipse.epp.internal.mpc.core.service.Marketplace;
import org.eclipse.epp.internal.mpc.core.service.Popular;


/**
 * @author David Green
 */
public class PopularContentHandler extends NodeListingContentHandler<Popular> {

	public PopularContentHandler() {
		super("popular"); //$NON-NLS-1$
	}

	@Override
	protected Popular createModel() {
		return new Popular();
	}

	@Override
	protected void setMarketplaceResult(Marketplace marketplace, Popular model) {
		marketplace.setPopular(model);
	}
}
