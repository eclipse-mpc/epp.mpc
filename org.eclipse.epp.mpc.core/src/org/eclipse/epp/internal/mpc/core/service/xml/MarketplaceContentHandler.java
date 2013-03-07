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
import org.eclipse.epp.internal.mpc.core.service.News;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceContentHandler extends UnmarshalContentHandler {

	private Marketplace model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("marketplace")) { //$NON-NLS-1$
			model = new Marketplace();

		} else if (localName.equals("market")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.MarketContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.MarketContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("catalogs")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.CatalogsContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.CatalogsContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("category")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.CategoryContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.CategoryContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("node")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.NodeContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("featured")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.FeaturedContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.FeaturedContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("search")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.SearchContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.SearchContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("favorites")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.FavoritesContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.FavoritesContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("popular")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.PopularContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.PopularContentHandler();
			childHandler.setParentModel(model);
			childHandler.setParentHandler(this);
			childHandler.setUnmarshaller(getUnmarshaller());
			getUnmarshaller().setCurrentHandler(childHandler);
			childHandler.startElement(uri, localName, attributes);
		} else if (localName.equals("recent")) { //$NON-NLS-1$
			org.eclipse.epp.internal.mpc.core.service.xml.RecentContentHandler childHandler = new org.eclipse.epp.internal.mpc.core.service.xml.RecentContentHandler();
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
		if (localName.equals("marketplace")) { //$NON-NLS-1$

			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("market")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("category")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("node")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("featured")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("search")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("favorites")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("popular")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("recent")) { //$NON-NLS-1$
			// nothing to do
		} else if (localName.equals("news")) { //$NON-NLS-1$
			News news = (News) getUnmarshaller().getModel();
			getUnmarshaller().setModel(null);
			model.setNews(news);
		}
		return false;
	}

}
