/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Yatta Solutions  - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;

import org.eclipse.epp.internal.mpc.core.service.Iu;
import org.eclipse.epp.internal.mpc.core.service.Ius;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Carsten Reckord
 */
public class IuContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = ""; //$NON-NLS-1$

	private Iu model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("iu")) { //$NON-NLS-1$
			model = new Iu();
			//FIXME at some point we sent optional, at another required, so we handle both for now...
			Boolean optional = null;
			String optionalValue = attributes.getValue(NS_URI, "optional"); //$NON-NLS-1$
			if (optionalValue != null) {
				optional = Boolean.valueOf(optionalValue);
			}
			String requiredValue = attributes.getValue(NS_URI, "required"); //$NON-NLS-1$
			if (requiredValue != null) {
				Boolean required = Boolean.valueOf(requiredValue);
				optional = optional == null ? !required : optional && !required;
			}
			if (optional != null) {
				model.setOptional(optional);
			}

			String selectedValue = attributes.getValue(NS_URI, "selected"); //$NON-NLS-1$
			if (selectedValue != null) {
				model.setSelected(Boolean.valueOf(selectedValue));
			}
			capturingContent = true;
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("iu")) { //$NON-NLS-1$
			if (content != null) {
				model.setId(content.toString());
				if (parentModel instanceof Ius) {
					((Ius) parentModel).getIuElements().add(model);
				}
				content = null;
				getUnmarshaller().setModel(model);
			}
			capturingContent = false;
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
