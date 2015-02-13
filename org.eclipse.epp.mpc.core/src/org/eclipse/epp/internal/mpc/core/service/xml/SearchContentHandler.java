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
import org.eclipse.epp.internal.mpc.core.service.Search;
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
		model.setUrl(attributes.getValue(NS_URI, "url")); //$NON-NLS-1$
		model.setTerm(attributes.getValue(NS_URI, "term")); //$NON-NLS-1$
	}

	@Override
	protected void setMarketplaceResult(Marketplace marketplace, Search model) {
		marketplace.setSearch(model);
	}
}
