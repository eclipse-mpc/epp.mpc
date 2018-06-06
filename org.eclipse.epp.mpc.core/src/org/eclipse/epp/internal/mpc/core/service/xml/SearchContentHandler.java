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


import org.eclipse.epp.internal.mpc.core.model.Marketplace;
import org.eclipse.epp.internal.mpc.core.model.Search;
import org.xml.sax.Attributes;

/**
 * @author David Green
 */
public class SearchContentHandler extends NodeListingContentHandler<Search> {

	public SearchContentHandler() {
		super("search"); //$NON-NLS-1$
	}

	@Override
	protected Search createModel() {
		return new Search();
	}

	@Override
	protected void configureModel(Search model, Attributes attributes) {
		super.configureModel(model, attributes);
		model.setUrl(toUrlString(attributes.getValue(NS_URI, "url"))); //$NON-NLS-1$
		model.setTerm(attributes.getValue(NS_URI, "term")); //$NON-NLS-1$
	}

	@Override
	protected void setMarketplaceResult(Marketplace marketplace, Search model) {
		marketplace.setSearch(model);
	}
}
