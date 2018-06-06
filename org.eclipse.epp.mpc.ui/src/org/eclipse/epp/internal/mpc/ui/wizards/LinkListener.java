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

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Point;

abstract class LinkListener implements MouseListener, SelectionListener {

	private boolean active = false;

	public void register(StyledText styledText) {
		styledText.addSelectionListener(this);
		styledText.addMouseListener(this);
	}

	public void unregister(StyledText styledText) {
		styledText.removeSelectionListener(this);
		styledText.removeMouseListener(this);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		StyledText link = (StyledText) e.getSource();
		if (link.getSelectionCount() != 0) {
			active = false;
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
	}

	@Override
	public void mouseDown(MouseEvent e) {
		StyledText link = (StyledText) e.getSource();
		active = (e.button == 1) && link.getSelectionCount() == 0;
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (!active) {
			return;
		}
		active = false;
		if (e.button != 1) {
			return;
		}
		StyledText link = (StyledText) e.getSource();
		int offset;
		try {
			offset = link.getOffsetAtLocation(new Point(e.x, e.y));
		} catch (IllegalArgumentException ex) {
			offset = -1;
		}
		if (offset >= 0 && offset < link.getCharCount()) {
			StyleRange style = link.getStyleRangeAtOffset(offset);
			if (style != null) {
				Object data = style.data;
				if (data != null) {
					selected(data, e);
				}
			}
		}
	}

	protected abstract void selected(Object href, TypedEvent event);
}