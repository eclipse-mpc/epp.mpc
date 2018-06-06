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
package org.eclipse.epp.internal.mpc.ui.wizards.css;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.epp.internal.mpc.ui.wizards.CatalogSwitcherItem;
import org.w3c.dom.Element;

public class CatalogSwitcherItemElementProvider implements IElementProvider {

	public static final IElementProvider INSTANCE = new CatalogSwitcherItemElementProvider();

	@Override
	public Element getElement(Object element, CSSEngine engine) {
		if (element instanceof CatalogSwitcherItem) {
			CatalogSwitcherItem item = (CatalogSwitcherItem) element;
			return new CatalogSwitcherItemElement(item, engine);
		}
		return null;
	}

}
