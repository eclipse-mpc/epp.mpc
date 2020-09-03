/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.util;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.anyOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;

public interface SWTBotComboAdapter {

	public void setSelection(String text);

	public String selection();

	public int selectionIndex();

	public void setSelection(int index);

	public int itemCount();

	public String[] items();

	public String getText();

	public String getId();

	public String getToolTipText();

	public boolean isEnabled();

	public boolean isVisible();

	public boolean isActive();

	static class ComboAdapter extends SWTBotCombo implements SWTBotComboAdapter {

		public ComboAdapter(Combo w, SelfDescribing description) throws WidgetNotFoundException {
			super(w, description);
			// ignore
		}

		public ComboAdapter(Combo w) throws WidgetNotFoundException {
			super(w);
			// ignore
		}

	}

	static class CComboAdapter extends SWTBotCCombo implements SWTBotComboAdapter {

		public CComboAdapter(CCombo w, SelfDescribing description) throws WidgetNotFoundException {
			super(w, description);
		}

		public CComboAdapter(CCombo w) throws WidgetNotFoundException {
			super(w);
		}
	}

	/**
	 * @return a {@link SWTBotCombo} with the specified <code>none</code>.
	 * @throws WidgetNotFoundException
	 *             if the widget is not found or is disposed.
	 */
	public static SWTBotComboAdapter comboBox(SWTBot bot) {
		return comboBox(bot, 0);
	}

	/**
	 * @param index
	 *            the index of the widget.
	 * @return a {@link SWTBotCombo} with the specified <code>none</code>.
	 * @throws WidgetNotFoundException
	 *             if the widget is not found or is disposed.
	 */
	@SuppressWarnings("unchecked")
	public static SWTBotComboAdapter comboBox(SWTBot bot, int index) {
		Matcher<Widget> matcher = anyOf(widgetOfType(Combo.class), widgetOfType(CCombo.class));
		Widget widget = bot.widget(matcher, index);
		if (widget instanceof CCombo) {
			return new CComboAdapter((CCombo) widget, matcher);
		}
		return new ComboAdapter((Combo) widget, matcher);
	}

}
