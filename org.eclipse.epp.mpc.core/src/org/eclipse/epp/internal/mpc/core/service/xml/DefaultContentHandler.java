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


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * Content handler for unknown XML elements
 * @author David Green
 */
public class DefaultContentHandler extends UnmarshalContentHandler {
	@Override
	public void startElement(String uri, String localName, Attributes attributes) throws SAXException {
	}
	
	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		return true;
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}
}
