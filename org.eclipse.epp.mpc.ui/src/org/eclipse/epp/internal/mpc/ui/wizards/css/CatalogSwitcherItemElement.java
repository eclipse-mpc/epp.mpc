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

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.epp.internal.mpc.ui.wizards.CatalogSwitcherItem;

public class CatalogSwitcherItemElement extends CompositeElement {

	public CatalogSwitcherItemElement(CatalogSwitcherItem item, CSSEngine engine) {
		super(item, engine);
	}

	protected CatalogSwitcherItem getCatalogSwitcherItem() {
		return getNativeWidget();
	}

	@Override
	protected CatalogSwitcherItem getComposite() {
		return getNativeWidget();
	}

	@Override
	protected CatalogSwitcherItem getControl() {
		return getNativeWidget();
	}

	@Override
	protected CatalogSwitcherItem getWidget() {
		return getNativeWidget();
	}

	@Override
	public CatalogSwitcherItem getNativeWidget() {
		return (CatalogSwitcherItem) super.getNativeWidget();
	}

	@Override
	public boolean isPseudoInstanceOf(String s) {
		if ("checked".equals(s) && getNativeWidget().isSelected()) { //$NON-NLS-1$
			return true;
		}
		return super.isPseudoInstanceOf(s);
	}
}
