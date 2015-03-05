/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;

import org.eclipse.epp.internal.mpc.core.service.CatalogBranding;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Benjamin Muskalla
 */
public class CatalogBrandingContentHandler extends UnmarshalContentHandler {

	private static final String NS_URI = ""; //$NON-NLS-1$

	private CatalogBranding model;

	@Override
	public void startElement(String uri, String localName, Attributes attributes) {
		if (localName.equals("wizard")) { //$NON-NLS-1$
			model = new CatalogBranding();

			model.setWizardTitle(attributes.getValue(NS_URI, "title")); //$NON-NLS-1$
		} else if (localName.equals("icon")) { //$NON-NLS-1$
			capturingContent = true;
		} else if (localName.equals("searchtab")) { //$NON-NLS-1$
			model.setHasSearchTab("1".equals(attributes.getValue(NS_URI, "enabled"))); //$NON-NLS-1$//$NON-NLS-2$
			capturingContent = true;
		} else if (localName.equals("populartab")) { //$NON-NLS-1$
			model.setHasPopularTab("1".equals(attributes.getValue(NS_URI, "enabled"))); //$NON-NLS-1$//$NON-NLS-2$
			capturingContent = true;
		} else if (localName.equals("recenttab")) { //$NON-NLS-1$
			model.setHasRecentTab("1".equals(attributes.getValue(NS_URI, "enabled"))); //$NON-NLS-1$//$NON-NLS-2$
			capturingContent = true;
		} else if (localName.equals("relatedtab") || localName.equals("recommendationtab")) { //$NON-NLS-1$
			model.setHasRelatedTab("1".equals(attributes.getValue(NS_URI, "enabled"))); //$NON-NLS-1$//$NON-NLS-2$
			capturingContent = true;
		}
	}

	@Override
	public boolean endElement(String uri, String localName) throws SAXException {
		if (localName.equals("wizard")) { //$NON-NLS-1$
			if (parentModel instanceof org.eclipse.epp.internal.mpc.core.service.Catalog) {
				((org.eclipse.epp.internal.mpc.core.service.Catalog) parentModel).setBranding(model);
			}
			getUnmarshaller().setModel(model);
			model = null;
			getUnmarshaller().setCurrentHandler(parentHandler);
			if (parentHandler != null) {
				parentHandler.endElement(uri, localName);
			}
			return true;
		} else if (localName.equals("icon")) { //$NON-NLS-1$
			if (content != null) {
				model.setWizardIcon(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("searchtab")) { //$NON-NLS-1$
			if (content != null) {
				model.setSearchTabName(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("populartab")) { //$NON-NLS-1$
			if (content != null) {
				model.setPopularTabName(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("recenttab")) { //$NON-NLS-1$
			if (content != null) {
				model.setRecentTabName(content.toString());
				content = null;
			}
			capturingContent = false;
		} else if (localName.equals("relatedtab")) { //$NON-NLS-1$
			if (content != null) {
				model.setRelatedTabName(content.toString());
				content = null;
			}
			capturingContent = false;
		}
		return false;
	}

}
