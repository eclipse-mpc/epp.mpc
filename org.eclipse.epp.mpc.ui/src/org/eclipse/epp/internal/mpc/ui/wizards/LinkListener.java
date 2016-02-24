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

	public void widgetSelected(SelectionEvent e) {
		StyledText link = (StyledText) e.getSource();
		if (link.getSelectionCount() != 0) {
			active = false;
		}
	}

	public void widgetDefaultSelected(SelectionEvent e) {
	}

	public void mouseDoubleClick(MouseEvent e) {
	}

	public void mouseDown(MouseEvent e) {
		StyledText link = (StyledText) e.getSource();
		active = (e.button == 1) && link.getSelectionCount() == 0;
	}

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
			if (style != null && style.data != null) {
				selected(style.data, e);
			}
		}
	}

	protected abstract void selected(Object href, TypedEvent event);
}