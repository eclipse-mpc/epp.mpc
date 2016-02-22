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

import org.eclipse.epp.internal.mpc.core.model.Marketplace;
import org.eclipse.epp.internal.mpc.core.model.News;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceContentHandler extends UnmarshalContentHandler {

	private Marketplace model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) throws SAXException {
		if (localName.equals("marketplace")) { //$NON-NLS-1$
			model = new Marketplace();
		} else {
			Unmarshaller unmarshaller = getUnmarshaller();
			UnmarshalContentHandler childHandler = unmarshaller == null ? null : unmarshaller.getHandler(localName);
			if (unmarshaller != null && childHandler != null) {
				childHandler.setParentModel(model);
				childHandler.setParentHandler(this);
				childHandler.setUnmarshaller(unmarshaller);
				unmarshaller.setCurrentHandler(childHandler);
				childHandler.startElement(uri, localName, attributes);
			}
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("marketplace")) { //$NON-NLS-1$

			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("news")) { //$NON-NLS-1$
			News news = (News) getUnmarshaller().getModel();
			getUnmarshaller().setModel(null);
			model.setNews(news);
		}
		return false;
	}

}
