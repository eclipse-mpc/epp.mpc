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

public abstract class ActionLink {
	private final String id;

	private final String label;

	private final String tooltip;

	public ActionLink(String id, String label, String tooltip) {
		this.id = id;
		this.label = label;
		this.tooltip = tooltip;
	}

	public final String getId() {
		return id;
	}

	public final String getLabel() {
		return label;
	}

	public final String getTooltip() {
		return tooltip;
	}

	public abstract void selected();
}
