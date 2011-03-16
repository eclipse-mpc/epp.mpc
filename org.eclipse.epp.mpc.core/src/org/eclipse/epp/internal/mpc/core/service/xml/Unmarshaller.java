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



import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML unmarshaller
 * 
 * @author David Green
 * @author Benjamin Muskalla
 */
public class Unmarshaller extends DefaultHandler {

	/**
	 * Unmarshal an object from the given input source
	 */
	public static Object parse(InputSource input) throws IOException, SAXException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
		XMLReader xmlReader;
		try {
			xmlReader = parserFactory.newSAXParser().getXMLReader();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
		Unmarshaller unmarshaller = new Unmarshaller();
		xmlReader.setContentHandler(unmarshaller);
		xmlReader.parse(input);
		return unmarshaller.getModel();
	}


	private final Map<String,UnmarshalContentHandler> elementNameToUnmarshalContentHandler = new HashMap<String, UnmarshalContentHandler>();
	{
		elementNameToUnmarshalContentHandler.put("marketplace", new MarketplaceContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("market", new MarketContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("category", new CategoryContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("node", new NodeContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("categories", new CategoriesContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("catalogs", new CatalogsContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("catalog", new CatalogContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("wizard", new CatalogBrandingContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("tags", new TagsContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("tag", new TagContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("ius", new IusContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("platforms", new PlatformsContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("search", new SearchContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("recent", new RecentContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("featured", new FeaturedContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("popular", new PopularContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("favorites", new FavoritesContentHandler()); //$NON-NLS-1$
	}

	private UnmarshalContentHandler currentHandler;
	private Object model;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		computeHandler(localName);
		currentHandler.startElement(uri, localName, attributes);
	}

	private void computeHandler(String localName) {
		if (currentHandler == null) {
			currentHandler = elementNameToUnmarshalContentHandler.get(localName);
			if (currentHandler == null) {
				currentHandler = new DefaultContentHandler();
			}
			currentHandler.setUnmarshaller(this);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		currentHandler.endElement(uri, localName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (currentHandler != null) {
			currentHandler.characters(ch, start, length);
		}
	}

	public Object getModel() {
		return model;
	}

	public void setModel(Object model) {
		this.model = model;
	}
	protected UnmarshalContentHandler getCurrentHandler() {
		return currentHandler;
	}

	protected void setCurrentHandler(UnmarshalContentHandler currentHandler) {
		this.currentHandler = currentHandler;
	}
}
