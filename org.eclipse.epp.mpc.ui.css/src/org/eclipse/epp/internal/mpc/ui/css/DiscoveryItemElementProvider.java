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

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.equinox.internal.p2.ui.discovery.util.GradientCanvas;
import org.w3c.dom.Element;

public class DiscoveryItemElementProvider implements IElementProvider {

	public static final IElementProvider INSTANCE = new DiscoveryItemElementProvider();

	@Override
	public Element getElement(Object element, CSSEngine engine) {
		if (element instanceof GradientCanvas) {
			return new GradientCanvasElement((GradientCanvas) element, engine);
		}

		return null;
	}
}