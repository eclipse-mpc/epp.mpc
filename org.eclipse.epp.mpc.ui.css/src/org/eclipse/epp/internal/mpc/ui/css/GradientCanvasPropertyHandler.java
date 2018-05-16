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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.css.core.dom.properties.Gradient;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.properties.AbstractCSSPropertySWTHandler;
import org.eclipse.equinox.internal.p2.ui.discovery.util.GradientCanvas;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;

public class GradientCanvasPropertyHandler  extends AbstractCSSPropertySWTHandler {

	public static final String BACKGROUND_COLOR = "background-color"; //$NON-NLS-1$
	public static final String BACKGROUND_BASE_COLOR = "background-base-color"; //$NON-NLS-1$
	public static final String H_BOTTOM_KEYLINE_2 = "h-bottom-keyline-2-color"; //$NON-NLS-1$
	public static final String H_BOTTOM_KEYLINE_1 = "h-bottom-keyline-1-color"; //$NON-NLS-1$

	@Override
	protected void applyCSSProperty(Control control, String property, CSSValue value, String pseudo, CSSEngine engine)
			throws Exception {
		if (!(control instanceof GradientCanvas)) {
			return;
		}
		GradientCanvas canvas = (GradientCanvas) control;
		if (BACKGROUND_COLOR.equals(property)) {
			if (value.getCssValueType() == CSSValue.CSS_VALUE_LIST) {
				Gradient grad = (Gradient) engine.convert(value, Gradient.class, canvas.getDisplay());
				if (grad == null) {
					canvas.setBackgroundGradient(null, null, false);
					return;
				}
				List<CSSPrimitiveValue> values = grad.getValues();
				List<Color> colors = new ArrayList<>(values.size());
				for (CSSPrimitiveValue cssValue : values) {
					if (cssValue != null) {
						if (cssValue.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
							Color color = (Color) engine.convert(cssValue, Color.class, canvas.getDisplay());
							colors.add(color);
						}
					}
				}

				if (colors.size() > 0) {
					List<Integer> list = grad.getPercents();
					int[] percents = new int[list.size()];
					for (int i = 0; i < percents.length; i++) {
						percents[i] = list.get(i).intValue();
					}
					canvas.setBackgroundGradient(colors.toArray(new Color[0]), percents,
							grad.getVerticalGradient());
				} else {
					canvas.setBackgroundGradient(null, null, false);
				}
			} else if (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				Color color = (Color) engine.convert(value, Color.class, canvas.getDisplay());
				canvas.setBackgroundGradient(null, null, false);
				canvas.setBackground(color);
			}

		} else if ((H_BOTTOM_KEYLINE_1.equals(property) || H_BOTTOM_KEYLINE_2.equals(property) || BACKGROUND_BASE_COLOR.equals(property)) && value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			Color color = (Color) engine.convert(value, Color.class, canvas.getDisplay());
			// When a single color is received, make it 100% with that
			// single color.
			if (H_BOTTOM_KEYLINE_1.equals(property)) {
				canvas.putColor(GradientCanvas.H_BOTTOM_KEYLINE1, color);
			} else if (H_BOTTOM_KEYLINE_2.equals(property)) {
				canvas.putColor(GradientCanvas.H_BOTTOM_KEYLINE2, color);
			} else {
				canvas.setBackground(color);
			}
		}
	}

	@Override
	protected String retrieveCSSProperty(Control control, String property, String pseudo, CSSEngine engine)
			throws Exception {
		return null;
	}
}