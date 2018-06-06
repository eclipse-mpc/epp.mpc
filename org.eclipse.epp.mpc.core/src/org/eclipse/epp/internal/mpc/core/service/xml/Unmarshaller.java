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
 *      The Eclipse Foundation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;



import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML unmarshaller
 *
 * @author David Green
 * @author Benjamin Muskalla
 */
public class Unmarshaller extends DefaultHandler {

	private static SAXParserFactory parserFactory;

	private static EntityResolver emptyResolver;

	static {
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
		parserFactory.setValidating(false);
		setFeature(parserFactory, "http://xml.org/sax/features/namespaces", true); //$NON-NLS-1$
		setFeature(parserFactory, "http://xml.org/sax/features/validation", false); //$NON-NLS-1$
		setFeature(parserFactory, "http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); //$NON-NLS-1$
		setFeature(parserFactory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$

		emptyResolver = (publicId, systemId) -> {
			InputSource emptyInputSource = new InputSource(new StringReader("")); //$NON-NLS-1$
			emptyInputSource.setPublicId(publicId);
			emptyInputSource.setSystemId(systemId);
			return emptyInputSource;
		};
	}

	public static XMLReader createXMLReader(final Unmarshaller unmarshaller) {

		try {
			final XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
			xmlReader.setEntityResolver(emptyResolver);
			xmlReader.setContentHandler(unmarshaller);
			return xmlReader;
		} catch (Exception e1) {
			throw new IllegalStateException(e1);
		}
	}

	private static void setFeature(SAXParserFactory parserFactory, String feature, boolean enablement) {
		try {
			parserFactory.setFeature(feature, enablement);
		} catch (SAXNotRecognizedException e) {
			//ignore
		} catch (SAXNotSupportedException e) {
			//ignore
		} catch (ParserConfigurationException e) {
			//ignore
		}
	}

	/**
	 * Unmarshal an object from the given input source
	 */
	public static Object parse(InputSource input) throws IOException, SAXException {
		Unmarshaller unmarshaller = new Unmarshaller();
		XMLReader xmlReader = createXMLReader(unmarshaller);
		xmlReader.parse(input);
		return unmarshaller.getModel();
	}


	private final Map<String, UnmarshalContentHandler> elementNameToUnmarshalContentHandler = new HashMap<>();
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
		elementNameToUnmarshalContentHandler.put("related", new RelatedContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("favorites", new FavoritesContentHandler()); //$NON-NLS-1$
		elementNameToUnmarshalContentHandler.put("news", new NewsContentHandler()); //$NON-NLS-1$
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
			currentHandler = getHandler(localName);
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

	public UnmarshalContentHandler getHandler(String localName) {
		return elementNameToUnmarshalContentHandler.get(localName);
	}
}
