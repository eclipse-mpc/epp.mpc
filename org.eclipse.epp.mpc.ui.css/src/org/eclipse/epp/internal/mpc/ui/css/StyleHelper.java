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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.css.swt.internal.theme.Theme;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.swt.widgets.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Element;

@SuppressWarnings("restriction")
public class StyleHelper {

	private static final String PLATFORM_THEME_PREFIX = "org.eclipse.e4.ui.css.theme."; //$NON-NLS-1$
	private Widget widget;

	private static String latestThemeId;

	private static List<String> latestThemeVariants;

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
	
	private ITheme getCurrentTheme() {
		BundleContext bundleContext = FrameworkUtil.getBundle(StyleHelper.class).getBundleContext();
		ServiceReference<IThemeManager> serviceReference = bundleContext.getServiceReference(IThemeManager.class);
		try {
			IThemeManager themeManager = bundleContext.getService(serviceReference);
			IThemeEngine engineForDisplay = themeManager == null ? null
					: themeManager.getEngineForDisplay(widget.getDisplay());
			return engineForDisplay == null ? null : engineForDisplay.getActiveTheme();
		} finally {
			bundleContext.ungetService(serviceReference);
		}
	}

	private List<String> listThemeVariants(ITheme theme) {
		List<String> themeVariants = new ArrayList<>(2);
		if (theme == null) {
			return themeVariants;
		}
		String themeId = theme.getId();
		if (themeId.equals(latestThemeId)) {
			return latestThemeVariants;
		}

		String themeBaseId = themeId;
		themeVariants.add(themeId);
		int variantPos = themeId.lastIndexOf('_');
		if (variantPos == -1) {
			variantPos = themeId.lastIndexOf('-');
		}
		if (variantPos != -1) {
			String variantBaseId = themeId.substring(0, variantPos);
			if (getTheme(variantBaseId) != null) {
				themeBaseId = variantBaseId;
				themeVariants.add(themeBaseId);
			}
		}
		if (theme instanceof Theme) {
			Theme internalTheme = (Theme) theme;
			String osVersion = internalTheme.getOsVersion();
			if (osVersion != null && themeBaseId.endsWith(osVersion)) {
				themeBaseId = themeBaseId.substring(0, themeBaseId.length() - osVersion.length());
				themeVariants.add(themeBaseId);
			}
		}
		latestThemeId = themeId;
		latestThemeVariants = themeVariants;
		return themeVariants;
	}

	private ITheme getTheme(String themeId) {
		BundleContext bundleContext = FrameworkUtil.getBundle(StyleHelper.class).getBundleContext();
		ServiceReference<IThemeManager> serviceReference = bundleContext.getServiceReference(IThemeManager.class);
		try {
			IThemeManager themeManager = bundleContext.getService(serviceReference);
			IThemeEngine engineForDisplay = themeManager == null ? null
					: themeManager.getEngineForDisplay(widget.getDisplay());
			if (engineForDisplay == null) {
				return null;
			}
			List<ITheme> themes = engineForDisplay.getThemes();
			for (ITheme theme : themes) {
				if (themeId.equals(theme.getId())) {
					return theme;
				}
			}
			return null;
		} finally {
			bundleContext.ungetService(serviceReference);
		}
	}

	public String getCurrentThemeId() {
		ITheme theme = getCurrentTheme();
		return theme == null ? null : theme.getId();
	}

	public URL getStylesheet(String theme, String path) {
		String simpleTheme = theme;
		if (theme != null && theme.startsWith(PLATFORM_THEME_PREFIX)) {
			simpleTheme = theme.substring(PLATFORM_THEME_PREFIX.length());
		}
		return FrameworkUtil.getBundle(StyleHelper.class)
				.getEntry("/css/" + (simpleTheme == null ? "default" : simpleTheme) + "/" + path); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	private URL getStylesheet(ITheme theme, String path) {
		for (String themeId : listThemeVariants(theme)) {
			URL cssUrl = getStylesheet(themeId, path);
			if (cssUrl != null) {
				return cssUrl;
			}
		}
		//default
		return getStylesheet((String) null, path);
	}

	public URL getCurrentThemeStylesheet(String path) {
		return getStylesheet(getCurrentTheme(), path);
	}
}
