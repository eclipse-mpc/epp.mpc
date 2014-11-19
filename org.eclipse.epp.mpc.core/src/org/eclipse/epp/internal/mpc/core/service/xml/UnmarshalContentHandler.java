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

import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Subclasses may participate in unmarshalling from XML
 *
 * @author David Green
 */
public abstract class UnmarshalContentHandler {

	protected Unmarshaller unmarshaller;

	protected StringBuilder content;

	protected boolean capturingContent;

	protected Object parentModel;

	protected UnmarshalContentHandler parentHandler;

	public abstract void startElement(String uri, String localName, Attributes attributes) throws SAXException;

	/**
	 * @return true if the handler has completed, otherwise false.
	 */
	public abstract boolean endElement(String uri, String localName) throws SAXException;

	public void characters(char[] ch, int start, int length) throws SAXException {
		if (capturingContent) {
			if (content == null) {
				content = new StringBuilder();
			}
			content.append(ch, start, length);
		}
	}

	protected Unmarshaller getUnmarshaller() {
		return unmarshaller;
	}

	protected void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	protected Object getParentModel() {
		return parentModel;
	}

	protected void setParentModel(Object parentModel) {
		this.parentModel = parentModel;
	}

	protected UnmarshalContentHandler getParentHandler() {
		return parentHandler;
	}

	protected void setParentHandler(UnmarshalContentHandler parentHandler) {
		this.parentHandler = parentHandler;
	}

	protected Date toDate(String string) {
		if (string == null) {
			return null;
		}
		string = string.trim();
		if (string.length() == 0) {
			return null;
		}
		try {
			return new Date(Long.parseLong(string) * 1000);
		} catch (NumberFormatException e) {
			// fail soft
			return null;
		}
	}

	protected Boolean toBoolean(String string) {
		if (string == null) {
			return null;
		}
		string = string.trim();
		if (string.length() == 0) {
			return null;
		}
		return "1".equals(string) || "true".equals(string); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected Integer toInteger(String string) {
		if (string == null) {
			return null;
		}
		string = string.trim();
		if (string.length() == 0) {
			return null;
		}
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			// fail soft
			return null;
		}
	}

	protected Integer toNatural(String string) {
		Integer intValue = toInteger(string);
		if (intValue != null && intValue.intValue() < 0) {
			intValue = null;
		}
		return intValue;
	}

	protected Long toLong(String string) {
		if (string == null) {
			return null;
		}
		string = string.trim();
		if (string.length() == 0) {
			return null;
		}
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			// fail soft
			return null;
		}
	}
}
