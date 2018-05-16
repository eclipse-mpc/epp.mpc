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
package org.eclipse.epp.internal.mpc.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.swt.widgets.Widget;
import org.w3c.dom.Element;

@SuppressWarnings("restriction")
public class StyleHelper {

	private Widget widget;

	public StyleHelper on(Widget widget) {
		this.widget = widget;
		return this;
	}

	public StyleHelper setClasses(String... cssClasses) {
		return setClass(String.join(" ", cssClasses)); //$NON-NLS-1$
	}

	public StyleHelper setClass(String cssClass) {
		WidgetElement.setCSSClass(widget, cssClass);
		return this;
	}

	public StyleHelper addClass(String cssClass) {
		String classes = getWidgetClasses();
		setClass(classes == null ? cssClass : classes + " " + cssClass); //$NON-NLS-1$
		return this;
	}

	public StyleHelper addClasses(String... cssClasses) {
		return addClass(String.join(" ", cssClasses)); //$NON-NLS-1$
	}

	private String getWidgetClasses() {
		return WidgetElement.getCSSClass(widget);
	}

	public StyleHelper setId(String id) {
		WidgetElement.setID(widget, id);
		return this;
	}

	private CSSEngine getCSSEngine() {
		return WidgetElement.getEngine(widget);
	}

	public Element getElement() {
		CSSEngine cssEngine = getCSSEngine();
		return cssEngine == null ? null : cssEngine.getElement(widget);
	}

	public StyleHelper applyStyles(boolean children) {
		getCSSEngine().applyStyles(widget, children);
		return this;
	}
}
