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


import org.eclipse.epp.internal.mpc.core.service.Popular;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * @author David Green
 */
public class PopularContentHandler extends UnmarshalContentHandler {
	
	private static final String NS_URI = ""; //$NON-NLS-1$
	
	private Popular model;
	
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("popular")) { //$NON-NLS-1$
			model = new Popular();
			
			model.setCount(toInteger(attributes.getValue(NS_URI,"count"))); //$NON-NLS-1$
		} else if (localName.equals("node")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri,localName,attributes);
		}
	}
	
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("popular")) { //$NON-NLS-1$
			if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Marketplace) {
				((org.eclipse.epp.internal.mpc.core.service.Marketplace)parentModel).setPopular(model);
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
	
}
