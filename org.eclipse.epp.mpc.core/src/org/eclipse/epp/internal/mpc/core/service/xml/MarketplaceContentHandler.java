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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author David Green
 */
public class MarketplaceContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = "";

	private Marketplace model;

	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("marketplace")) {
			model = new Marketplace();

		} else if (localName.equals("market")) {
			org.eclipse.epp.internal.mpc.core.service.xml.MarketContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.MarketContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("category")) {
			org.eclipse.epp.internal.mpc.core.service.xml.CategoryContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.CategoryContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("node")) {
			org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("featured")) {
			org.eclipse.epp.internal.mpc.core.service.xml.FeaturedContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.FeaturedContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("search")) {
			org.eclipse.epp.internal.mpc.core.service.xml.SearchContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.SearchContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("favorites")) {
			org.eclipse.epp.internal.mpc.core.service.xml.FavoritesContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.FavoritesContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("active")) {
			org.eclipse.epp.internal.mpc.core.service.xml.ActiveContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.ActiveContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("recent")) {
			org.eclipse.epp.internal.mpc.core.service.xml.RecentContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.RecentContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("marketplace")) {

			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("market")) {
			// nothing to do
		} else if (localName.equals("category")) {
			// nothing to do
		} else if (localName.equals("node")) {
			// nothing to do
		} else if (localName.equals("featured")) {
			// nothing to do
		} else if (localName.equals("search")) {
			// nothing to do
		} else if (localName.equals("favorites")) {
			// nothing to do
		} else if (localName.equals("active")) {
			// nothing to do
		} else if (localName.equals("recent")) {
			// nothing to do
		}
		return false;
	}

}
