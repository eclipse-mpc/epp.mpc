/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Yatta Solutions GmbH  - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;


import org.eclipse.epp.internal.mpc.core.service.Marketplace;
import org.eclipse.epp.internal.mpc.core.service.NodeListing;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * @author Carsten Reckord
 */
public abstract class NodeListingContentHandler<T extends NodeListing> extends UnmarshalContentHandler {

	protected static final String NS_URI = ""; //$NON-NLS-1$

	private T model;

	private final String rootElementName;

	public NodeListingContentHandler(String rootElementName) {
		this.rootElementName = rootElementName;
	}

	public String getRootElementName() {
		return rootElementName;
	}

	public T getModel() {
		return model;
	}

	protected abstract T createModel();

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals(getRootElementName())) {
			model = createModel();
			configureModel(model, attributes);
		} else if (localName.equals("node")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri,localName,attributes);
		}
	}

	protected void configureModel(T model, Attributes attributes) {
		model.setCount(toInteger(attributes.getValue(NS_URI,"count"))); //$NON-NLS-1$
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals(getRootElementName())) {
			if (parentModel instanceof Marketplace) {
				Marketplace marketplace = (Marketplace)parentModel;
				setMarketplaceResult(marketplace, model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri,localName);
			}
			return true;
		} else if (localName.equals("node")) { //$NON-NLS-1$
			// nothing to do
		}
		return false;
	}

	protected abstract void setMarketplaceResult(Marketplace marketplace, T model);

}
