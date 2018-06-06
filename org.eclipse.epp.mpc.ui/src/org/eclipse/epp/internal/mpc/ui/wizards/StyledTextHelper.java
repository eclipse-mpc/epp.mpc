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
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

class StyledTextHelper {

	/**
	 * Create a StyledText that acts much like a Label, i.e. isn't editable and doesn't get focus
	 */
	protected static StyledText createStyledTextLabel(Composite parent) {
		StyledText styledText = new StyledText(parent, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI | SWT.NO_FOCUS);
		styledText.setEditable(false);
		styledText.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		styledText.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_LABEL;
			}
		});
		return styledText;
	}

	protected static StyleRange appendLink(final StyledText styledText, String text, Object href, int style) {
		StyleRange range = createDynamicForegroundRange(styledText, 0, 0, style);
		range.underline = true;
		range.underlineStyle = SWT.UNDERLINE_LINK;

		appendStyled(styledText, text, range);
		range.data = href;

		return range;
	}

	protected static StyleRange createDynamicForegroundRange(final StyledText styledText, int start, int length,
			int style) {
		final Color currentForeground = styledText.getForeground();
		final StyleRange range = new StyleRange(start, length, currentForeground, null, style);
		styledText.addPaintListener(new PaintListener() {

			Color color = currentForeground;

			@Override
			public void paintControl(PaintEvent e) {
				if ((color != null && (color.isDisposed() || !color.equals(styledText.getForeground())))
						|| (color == null && styledText.getForeground() != null)) {
					color = styledText.getForeground();
					range.foreground = color;
					styledText.setStyleRange(range);
				}
			}
		});
		return range;
	}

	protected static void appendStyled(StyledText styledText, String text, StyleRange style) {
		style.start = styledText.getCharCount();
		style.length = text.length();

		styledText.append(text);
		styledText.setStyleRange(style);
	}

}
