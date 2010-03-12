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

import org.eclipse.epp.internal.mpc.core.service.Search;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author David Green
 */
public class SearchContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = "";

	private Search model;

	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("search")) {
			model = new Search();

			model.setCount(toInteger(attributes.getValue(NS_URI, "count")));
			model.setUrl(attributes.getValue(NS_URI, "url"));
			model.setTerm(attributes.getValue(NS_URI, "term"));
		} else if (localName.equals("node")) {
			org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("search")) {
			if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Marketplace) {
				((org.eclipse.epp.internal.mpc.core.service.Marketplace) parentModel).setSearch(model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("node")) {
			// nothing to do
		}
		return false;
	}

}
