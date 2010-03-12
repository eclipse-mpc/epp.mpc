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

import org.eclipse.epp.internal.mpc.core.service.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author David Green
 */
public class NodeContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = "";

	private Node model;

	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("node")) {
			model = new Node();

			model.setId(attributes.getValue(NS_URI, "id"));
			model.setName(attributes.getValue(NS_URI, "name"));
			model.setUrl(attributes.getValue(NS_URI, "url"));
		} else if (localName.equals("favorited")) {
			capturingContent = true;
		} else if (localName.equals("type")) {
			capturingContent = true;
		} else if (localName.equals("categories")) {
			org.eclipse.epp.internal.mpc.core.service.xml.CategoriesContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.CategoriesContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("owner")) {
			capturingContent = true;
		} else if (localName.equals("body")) {
			capturingContent = true;
		} else if (localName.equals("created")) {
			capturingContent = true;
		} else if (localName.equals("changed")) {
			capturingContent = true;
		} else if (localName.equals("foundationmember")) {
			capturingContent = true;
		} else if (localName.equals("homepageurl")) {
			capturingContent = true;
		} else if (localName.equals("image")) {
			capturingContent = true;
		} else if (localName.equals("screenshot")) {
			capturingContent = true;
		} else if (localName.equals("version")) {
			capturingContent = true;
		} else if (localName.equals("license")) {
			capturingContent = true;
		} else if (localName.equals("companyname")) {
			capturingContent = true;
		} else if (localName.equals("status")) {
			capturingContent = true;
		} else if (localName.equals("eclipseversion")) {
			capturingContent = true;
		} else if (localName.equals("supporturl")) {
			capturingContent = true;
		} else if (localName.equals("updateurl")) {
			capturingContent = true;
		} else if (localName.equals("ius")) {
			org.eclipse.epp.internal.mpc.core.service.xml.IusContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.IusContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("platforms")) {
			org.eclipse.epp.internal.mpc.core.service.xml.PlatformsContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.PlatformsContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("node")) {
			if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Marketplace) {
				((org.eclipse.epp.internal.mpc.core.service.Marketplace) parentModel).getNode().add(model);
			} else if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Category) {
				((org.eclipse.epp.internal.mpc.core.service.Category) parentModel).getNode().add(model);
			} else if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Search) {
				((org.eclipse.epp.internal.mpc.core.service.Search) parentModel).getNode().add(model);
			} else if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Recent) {
				((org.eclipse.epp.internal.mpc.core.service.Recent) parentModel).getNode().add(model);
			} else if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Featured) {
				((org.eclipse.epp.internal.mpc.core.service.Featured) parentModel).getNode().add(model);
			} else if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Active) {
				((org.eclipse.epp.internal.mpc.core.service.Active) parentModel).getNode().add(model);
			} else if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Favorites) {
				((org.eclipse.epp.internal.mpc.core.service.Favorites) parentModel).getNode().add(model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("favorited")) {
			if (content != null) {
				model.setFavorited(toInteger(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("type")) {
			if (content != null) {
				model.setType(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("categories")) {
			// nothing to do
		} else if (localName.equals("owner")) {
			if (content != null) {
				model.setOwner(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("body")) {
			if (content != null) {
				model.setBody(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("created")) {
			if (content != null) {
				model.setCreated(toDate(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("changed")) {
			if (content != null) {
				model.setChanged(toDate(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("foundationmember")) {
			if (content != null) {
				model.setFoundationmember(toBoolean(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("homepageurl")) {
			if (content != null) {
				model.setHomepageurl(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("image")) {
			if (content != null) {
				model.setImage(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("screenshot")) {
			if (content != null) {
				model.setScreenshot(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("version")) {
			if (content != null) {
				model.setVersion(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("license")) {
			if (content != null) {
				model.setLicense(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("companyname")) {
			if (content != null) {
				model.setCompanyname(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("status")) {
			if (content != null) {
				model.setStatus(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("eclipseversion")) {
			if (content != null) {
				model.setEclipseversion(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("supporturl")) {
			if (content != null) {
				model.setSupporturl(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("updateurl")) {
			if (content != null) {
				model.setUpdateurl(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("ius")) {
			// nothing to do
		} else if (localName.equals("platforms")) {
			// nothing to do
		}
		return false;
	}

}
