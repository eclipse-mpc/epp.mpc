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

import org.eclipse.epp.internal.mpc.core.service.Catalog;
import org.eclipse.epp.internal.mpc.core.service.News;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public class CatalogContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = ""; //$NON-NLS-1$

	private Catalog model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("catalog")) { //$NON-NLS-1$
			model = new Catalog();

			model.setId(attributes.getValue(NS_URI, "id")); //$NON-NLS-1$
			model.setName(attributes.getValue(NS_URI, "title")); //$NON-NLS-1$
			model.setUrl(attributes.getValue(NS_URI, "url")); //$NON-NLS-1$
			model.setSelfContained("1".equals(attributes.getValue(NS_URI, "selfContained"))); //$NON-NLS-1$ //$NON-NLS-2$
			model.setImageUrl(attributes.getValue(NS_URI, "icon")); //$NON-NLS-1$
		} else if (localName.equals("dependenciesRepository")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("description")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("wizard")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.CatalogBrandingContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.CatalogBrandingContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("news")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.NewsContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NewsContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("catalog")) { //$NON-NLS-1$
			if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Catalogs) {
				((org.eclipse.epp.internal.mpc.core.service.Catalogs) parentModel).getCatalogs().add(model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("dependenciesRepository")) { //$NON-NLS-1$
			if (content != null) {
				model.setDependencyRepository(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("description")) { //$NON-NLS-1$
			if (content != null) {
				model.setDescription(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("news")) { //$NON-NLS-1$
			News news = (News) getUnmarshaller().getModel();
			getUnmarshaller().setModel(null);
			model.setNews(news);
		}
		return false;
	}

}
