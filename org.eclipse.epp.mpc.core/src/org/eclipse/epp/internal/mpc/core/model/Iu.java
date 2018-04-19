/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import org.eclipse.epp.mpc.core.model.IIu;

public class Iu implements IIu {

	private String id;

	private boolean optional = true;

	private boolean selected = true;

	public Iu() {
	}

	public Iu(String id) {
		this.id = id;
	}

	public Iu(String id, boolean optional, boolean selected) {
		this.id = id;
		this.optional = optional;
		this.selected = selected;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	@Override
	public boolean isSelected() {
		return selected || !optional;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (optional ? 1231 : 1237);
		result = prime * result + (isSelected() ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof IIu)) {
			return false;
		}
		IIu other = (IIu) obj;
		if (id == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!id.equals(other.getId())) {
			return false;
		}
		if (optional != other.isOptional()) {
			return false;
		}
		if (isSelected() != other.isSelected()) {
			return false;
		}
		return true;
	}

	public void join(IIu other) {
		if (other.getId() == null || !other.getId().equals(this.id)) {
			throw new IllegalArgumentException();
		}
		if (!other.isOptional()) {
			// optional is the default - apply change to non-optional
			setOptional(false);
		}
		if (!other.isSelected()) {
			// selected is the default - apply change to unselected
			setSelected(false);
		}
	}
}
