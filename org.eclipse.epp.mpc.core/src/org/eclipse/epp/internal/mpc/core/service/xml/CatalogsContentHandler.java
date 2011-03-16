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

import org.eclipse.epp.internal.mpc.core.service.Catalogs;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Benjamin Muskalla
 */
public class CatalogsContentHandler extends UnmarshalContentHandler {

	private Catalogs model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("catalogs")) { //$NON-NLS-1$
			model = new Catalogs();

		} else if (localName.equals("catalog")) { //$NON-NLS-1$
			CatalogContentHandler childHandler = new CatalogContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("catalogs")) { //$NON-NLS-1$
			getUnmarshaller().setModel(model);
			model = null;
			return true;
		} else if (localName.equals("catalog")) { //$NON-NLS-1$
			// nothing to do
		}
		return false;
	}

}
