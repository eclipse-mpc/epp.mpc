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
