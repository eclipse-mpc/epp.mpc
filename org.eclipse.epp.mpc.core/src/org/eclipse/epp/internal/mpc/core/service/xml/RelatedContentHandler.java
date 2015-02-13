/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Yatta Solutions GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;


import org.eclipse.epp.internal.mpc.core.service.Marketplace;
import org.eclipse.epp.internal.mpc.core.service.Related;


/**
 * @author Carsten Reckord
 */
public class RelatedContentHandler extends NodeListingContentHandler<Related> {

	public RelatedContentHandler() {
		super("related"); //$NON-NLS-1$
	}

	@Override
	protected Related createModel() {
		return new Related();
	}

	@Override
	protected void setMarketplaceResult(Marketplace marketplace, Related model) {
		marketplace.setRelated(model);
	}
}
