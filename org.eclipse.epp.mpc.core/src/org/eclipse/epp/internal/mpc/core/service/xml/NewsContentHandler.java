/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;

import org.eclipse.epp.internal.mpc.core.model.News;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Carsten Reckord
 */
public class NewsContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = ""; //$NON-NLS-1$

	private News model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("news")) { //$NON-NLS-1$
			model = new News();
			model.setShortTitle(attributes.getValue(NS_URI, "shorttitle")); //$NON-NLS-1$
			model.setTimestamp(toLong(attributes.getValue(NS_URI, "timestamp"))); //$NON-NLS-1$

			capturingContent = true;
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("news")) { //$NON-NLS-1$
			if (content != null) {
				final String url = toUrlString(content.toString());
				model.setUrl(url);
				content = null;
			}
			capturingContent = false;

			getUnmarshaller().setModel(model.getUrl() == null ? null : model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
		}
		return false;
	}

}
