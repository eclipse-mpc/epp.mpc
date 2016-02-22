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


import org.eclipse.epp.internal.mpc.core.model.Featured;
import org.eclipse.epp.internal.mpc.core.model.Marketplace;


/**
 * @author David Green
 */
public class FeaturedContentHandler extends NodeListingContentHandler<Featured> {

	public FeaturedContentHandler() {
		super("featured"); //$NON-NLS-1$
	}

	@Override
	protected Featured createModel() {
		return new Featured();
	}

	@Override
	protected void setMarketplaceResult(Marketplace marketplace, Featured model) {
		marketplace.setFeatured(model);
	}
}
