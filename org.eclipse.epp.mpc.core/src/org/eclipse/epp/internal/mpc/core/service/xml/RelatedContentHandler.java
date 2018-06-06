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
 *      Yatta Solutions GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;


import org.eclipse.epp.internal.mpc.core.model.Marketplace;
import org.eclipse.epp.internal.mpc.core.model.Related;


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
