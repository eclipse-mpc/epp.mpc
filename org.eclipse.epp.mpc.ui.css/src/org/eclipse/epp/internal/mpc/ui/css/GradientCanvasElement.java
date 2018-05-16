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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.equinox.internal.p2.ui.discovery.util.GradientCanvas;
import org.eclipse.swt.graphics.Color;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class GradientCanvasElement extends CompositeElement {

	private static class GradientCanvasAccess {
		private static final String[] COLOR_KEYS = { GradientCanvas.COLOR_BASE_BG, GradientCanvas.H_BOTTOM_KEYLINE1,
				GradientCanvas.H_BOTTOM_KEYLINE2 };

		private static final String FIELD_NAME_VERTICAL = "vertical"; //$NON-NLS-1$

		private static final String FIELD_NAME_PERCENTS = "percents"; //$NON-NLS-1$

		private static final String FIELD_NAME_GRADIENT_COLORS = "gradientColors"; //$NON-NLS-1$

		private static final String FIELD_NAME_GRADIENT_INFO = "gradientInfo"; //$NON-NLS-1$

		private static final boolean ENABLED;

		private static final Field FIELD_GRADIENT_INFO;

		private static final Field FIELD_GRADIENT_COLORS;

		private static final Field FIELD_PERCENTS;

		private static final Field FIELD_VERTICAL;

		static {
			Field f_gradientInfo = null;
			Field f_gradientColors = null;
			Field f_percents = null;
			Field f_vertical = null;
			boolean enabled = false;
			try {
				f_gradientInfo = GradientCanvas.class.getDeclaredField(FIELD_NAME_GRADIENT_INFO);
				f_gradientInfo.setAccessible(true);
				Class<?> c_gradientInfo = f_gradientInfo.getType();
				f_gradientColors = c_gradientInfo.getDeclaredField(FIELD_NAME_GRADIENT_COLORS);
				f_gradientColors.setAccessible(true);
				f_percents = c_gradientInfo.getDeclaredField(FIELD_NAME_PERCENTS);
				f_percents.setAccessible(true);
				f_vertical = c_gradientInfo.getDeclaredField(FIELD_NAME_VERTICAL);
				f_vertical.setAccessible(true);
				enabled = true;
			} catch (Exception ex) {
				Bundle bundle = FrameworkUtil.getBundle(GradientCanvasElement.class);
				Platform.getLog(bundle).log(new Status(IStatus.INFO, bundle.getSymbolicName(),
						"Dynamic theming for GradientCanvas is limited", ex));
			}
			if (enabled) {
				FIELD_GRADIENT_INFO = f_gradientInfo;
				FIELD_GRADIENT_COLORS = f_gradientColors;
				FIELD_PERCENTS = f_percents;
				FIELD_VERTICAL = f_vertical;
			} else {
				FIELD_GRADIENT_INFO = null;
				FIELD_GRADIENT_COLORS = null;
				FIELD_PERCENTS = null;
				FIELD_VERTICAL = null;
			}
			ENABLED = enabled;
		}

		static GradientCanvasData read(GradientCanvas gradientCanvas) {
			if (!ENABLED) {
				return null;
			}
			GradientCanvasData data = new GradientCanvasData();

			try {
				Object gradientInfo = FIELD_GRADIENT_INFO.get(gradientCanvas);
				if (gradientInfo != null) {
					data.gradientColors = (Color[]) FIELD_GRADIENT_COLORS.get(gradientInfo);
					data.gradientPercent = (int[]) FIELD_PERCENTS.get(gradientInfo);
					data.vertical = !Boolean.FALSE.equals(FIELD_VERTICAL.get(gradientInfo));
				}
			} catch (Exception ex) {
				//ignore - we just won't be able to restore colors on the fly
				//also don't return a partial state
				return null;
			}
			data.colors = new HashMap<>();
			for (String key : COLOR_KEYS) {
				if (gradientCanvas.hasColor(key)) {
					data.colors.put(key, gradientCanvas.getColor(key));
				}
			}
			return data;
		}

		static void restore(GradientCanvas canvas, GradientCanvasData data) {
			if (data == null) {
				return;
			}
			for (String key : COLOR_KEYS) {
				canvas.putColor(key, data.colors.get(key));
			}
			if (data.gradientColors == null) {
				canvas.setBackgroundGradient(null, null, true);
			} else {
				canvas.setBackgroundGradient(data.gradientColors, data.gradientPercent, data.vertical);
			}
		}
	}

	private static class GradientCanvasData {

		private Color[] gradientColors;

		private int[] gradientPercent;

		private boolean vertical;

		private Map<String, Color> colors;

	}

	private final GradientCanvasData gradientCanvasData;

	public GradientCanvasElement(GradientCanvas gradientCanvas, CSSEngine engine) {
		super(gradientCanvas, engine);
		gradientCanvasData = GradientCanvasAccess.read(gradientCanvas);
	}

	@Override
	protected GradientCanvas getComposite() {
		return (GradientCanvas) super.getComposite();
	}

	@Override
	protected GradientCanvas getControl() {
		return (GradientCanvas) super.getControl();
	}

	@Override
	public void reset() {
		GradientCanvas canvas = getControl();
		GradientCanvasAccess.restore(canvas, gradientCanvasData);
		super.reset();
	}
}
