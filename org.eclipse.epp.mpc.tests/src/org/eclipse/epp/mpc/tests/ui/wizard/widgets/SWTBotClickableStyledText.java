/*******************************************************************************
 * Copyright (c) 2010, 2026 The Eclipse Foundation and others.
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
package org.eclipse.epp.mpc.tests.ui.wizard.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.hamcrest.SelfDescribing;

public class SWTBotClickableStyledText extends SWTBotStyledText {

	public SWTBotClickableStyledText(StyledText styledText, SelfDescribing description) throws WidgetNotFoundException {
		super(styledText, description);
	}

	public SWTBotClickableStyledText(StyledText styledText) throws WidgetNotFoundException {
		super(styledText);
	}

	public static SWTBotClickableStyledText from(SWTBotStyledText botText) {
		if (botText instanceof SWTBotClickableStyledText clickableBotText) {
			return clickableBotText;
		}
		return new SWTBotClickableStyledText(botText.widget);
	}

	public void click(final int offset) {
		Point location = locationAtOffset(offset);
		click(location.x, location.y, false);
	}

	private Point locationAtOffset(int offset) {
		return UIThreadRunnable.syncExec((Result<Point>) () -> {
			Point location = widget.getLocationAtOffset(offset);
			location.y += widget.getLineHeight(offset) / 2;
			return location;
		});
	}

	public void click(StyleRange range) {
		click(range.start + range.length / 2);
	}

	/**
	 * Clicks the given range with just a mouse down and up event. Unlike {@link #click(StyleRange)}, this doesn't end
	 * with a mouse exit event, so that popups closing when the mouse leaves the widget, like the overview tooltip, stay
	 * open.
	 */
	public void clickWithoutMouseExit(StyleRange range) {
		Point location = locationAtOffset(range.start + range.length / 2);
		notify(SWT.MouseDown, createMouseEvent(location.x, location.y, 1, SWT.NONE, 1));
		notify(SWT.MouseUp, createMouseEvent(location.x, location.y, 1, SWT.BUTTON1, 1));
	}
}
