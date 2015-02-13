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

import org.eclipse.epp.internal.mpc.core.service.Category;
import org.eclipse.epp.internal.mpc.core.service.Marketplace;
import org.eclipse.epp.internal.mpc.core.service.Node;
import org.eclipse.epp.internal.mpc.core.service.NodeListing;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author David Green
 * @author Benjamin Muskalla
 */
public class NodeContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = ""; //$NON-NLS-1$

	private Node model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("node")) { //$NON-NLS-1$
			model = new Node();

			model.setId(attributes.getValue(NS_URI, "id")); //$NON-NLS-1$
			model.setName(attributes.getValue(NS_URI, "name")); //$NON-NLS-1$
			model.setUrl(attributes.getValue(NS_URI, "url")); //$NON-NLS-1$
		} else if (localName.equals("favorited")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("installstotal")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("installsrecent")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("type")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("categories")) { //$NON-NLS-1$
			CategoriesContentHandler childHandler = new CategoriesContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("tags")) { //$NON-NLS-1$
			TagsContentHandler childHandler = new TagsContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("owner")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("shortdescription")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("body")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("created")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("changed")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("foundationmember")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("homepageurl")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("image")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("screenshot")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("version")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("license")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("companyname")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("status")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("eclipseversion")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("supporturl")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("updateurl")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("ius")) { //$NON-NLS-1$
			IusContentHandler childHandler = new IusContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("platforms")) { //$NON-NLS-1$
			PlatformsContentHandler childHandler = new PlatformsContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("node")) { //$NON-NLS-1$
			if (parentModel instanceof Marketplace) {
				((Marketplace) parentModel).getNode().add(model);
			} else if (parentModel instanceof NodeListing) {
				((NodeListing) parentModel).getNode().add(model);
			} else if (parentModel instanceof Category) {
				((Category) parentModel).getNode().add(model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("favorited")) { //$NON-NLS-1$
			if (content != null) {
				model.setFavorited(toNatural(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("installstotal")) { //$NON-NLS-1$
			if (content != null) {
				model.setInstallsTotal(toNatural(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("installsrecent")) { //$NON-NLS-1$
			if (content != null) {
				model.setInstallsRecent(toNatural(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("type")) { //$NON-NLS-1$
			if (content != null) {
				model.setType(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("categories")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("owner")) { //$NON-NLS-1$
			if (content != null) {
				model.setOwner(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("shortdescription")) { //$NON-NLS-1$
			if (content != null) {
				model.setShortdescription(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("body")) { //$NON-NLS-1$
			if (content != null) {
				model.setBody(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("created")) { //$NON-NLS-1$
			if (content != null) {
				model.setCreated(toDate(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("changed")) { //$NON-NLS-1$
			if (content != null) {
				model.setChanged(toDate(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("foundationmember")) { //$NON-NLS-1$
			if (content != null) {
				model.setFoundationmember(toBoolean(content.toString()));
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("homepageurl")) { //$NON-NLS-1$
			if (content != null) {
				model.setHomepageurl(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("image")) { //$NON-NLS-1$
			if (content != null) {
				model.setImage(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("screenshot")) { //$NON-NLS-1$
			if (content != null) {
				model.setScreenshot(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("version")) { //$NON-NLS-1$
			if (content != null) {
				model.setVersion(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("license")) { //$NON-NLS-1$
			if (content != null) {
				model.setLicense(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("companyname")) { //$NON-NLS-1$
			if (content != null) {
				model.setCompanyname(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("status")) { //$NON-NLS-1$
			if (content != null) {
				model.setStatus(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("eclipseversion")) { //$NON-NLS-1$
			if (content != null) {
				model.setEclipseversion(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("supporturl")) { //$NON-NLS-1$
			if (content != null) {
				model.setSupporturl(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("updateurl")) { //$NON-NLS-1$
			if (content != null) {
				model.setUpdateurl(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("ius")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("platforms")) { //$NON-NLS-1$
			// nothing to do
		}
		return false;
	}

}
