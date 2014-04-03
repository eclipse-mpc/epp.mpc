/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import org.eclipse.epp.mpc.core.model.IIdentifiable;


/**
 * @author David Green
 * @author Carsten Reckord
 */
public abstract class Identifiable implements IIdentifiable {

	protected String id;
	protected String name;
	protected String url;

	public Identifiable() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Check if the given object's type and id match.
	 *
	 * @param obj
	 *            the object to compare (can be null)
	 * @return true if <code>obj</code> has the same {@link #getClass() type} and {@link #getId() id} as this object
	 */
	public boolean equalsId(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!equalsType(obj)) {
			return false;
		}
		IIdentifiable other = (IIdentifiable) obj;
		if (id == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!id.equals(other.getId())) {
			return false;
		}
		return true;
	}

	/**
	 * Check if the given object's type and url match.
	 *
	 * @param obj
	 *            the object to compare (can be null)
	 * @return true if <code>obj</code> has the same {@link #getClass() type} and {@link #getUrl() url} as this object
	 */
	public boolean equalsUrl(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!equalsType(obj)) {
			return false;
		}
		IIdentifiable other = (IIdentifiable) obj;
		if (url == null) {
			if (other.getUrl() != null) {
				return false;
			}
		} else if (!url.equals(other.getUrl())) {
			return false;
		}
		return true;
	}

	/**
	 * Check if the given object's type and url match.
	 *
	 * @param obj
	 *            the object to compare (can be null)
	 * @return true if <code>obj</code> has the same {@link #getClass() type} and {@link #getUrl() url} as this object
	 */
	public boolean equalsName(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!equalsType(obj)) {
			return false;
		}
		IIdentifiable other = (IIdentifiable) obj;
		if (name == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!name.equals(other.getName())) {
			return false;
		}
		return true;
	}

	protected boolean equalsType(Object obj) {
		if (getClass() != obj.getClass()) {
			return false;
		}
		return true;
	}

	public static boolean matches(IIdentifiable id1, IIdentifiable id2) {
		return id2 == id1 || id2.equalsId(id1) || (id2.getId() == null && id2.equalsUrl(id1))
				|| (id2.getId() == null && id2.getUrl() == null && id2.equalsName(id1));
	}
}
