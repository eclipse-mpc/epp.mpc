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
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
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

	protected static StyleRange appendLink(StyledText styledText, String text, Object href, int style) {
		StyleRange range = new StyleRange(0, 0, styledText.getForeground(), null, style);
		range.underline = true;
		range.underlineStyle = SWT.UNDERLINE_LINK;

		appendStyled(styledText, text, range);
		range.data = href;

		return range;
	}

	protected static void appendStyled(StyledText styledText, String text, StyleRange style) {
		style.start = styledText.getCharCount();
		style.length = text.length();

		styledText.append(text);
		styledText.setStyleRange(style);
	}

}
