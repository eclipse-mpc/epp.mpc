/*******************************************************************************
 * Copyright (c) 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;

import org.eclipse.epp.internal.mpc.core.model.Tag;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Benjamin Muskalla
 */
public class TagContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = ""; //$NON-NLS-1$

	private Tag model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("tag")) { //$NON-NLS-1$
			model = new Tag();

			model.setId(attributes.getValue(NS_URI, "id")); //$NON-NLS-1$
			model.setName(attributes.getValue(NS_URI, "name")); //$NON-NLS-1$
			model.setUrl(toUrlString(attributes.getValue(NS_URI, "url"))); //$NON-NLS-1$
		} else if (localName.equals("node")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("tag")) { //$NON-NLS-1$
			if (parentModel instanceof org.eclipse.epp.internal.mpc.core.model.Tags) {
				((org.eclipse.epp.internal.mpc.core.model.Tags) parentModel).getTags().add(model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		}
		return false;
	}

}
