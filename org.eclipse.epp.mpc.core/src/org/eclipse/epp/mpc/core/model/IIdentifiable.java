/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.model;

/**
 * Base class for marketplace entities like nodes or categories. Instances are identified by an id unique to their
 * {@link ICatalog marketplace server} and/or their url. Optionally, they may have a name suitable for presentation to
 * the user.
 *
 * @author David Green
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IIdentifiable {

	/**
	 * @return this element's unique id in its {@link ICatalog catalog}
	 */
	String getId();

	/**
	 * @return this element's name
	 */
	String getName();

	/**
	 * @return the URL from which this element can be re-retrieved from the marketplace server
	 */
	String getUrl();

	/**
	 * Check if the given object's type and id match.
	 *
	 * @param obj
	 *            the object to compare (can be null)
	 * @return true if <code>obj</code> has the same {@link #getClass() type} and {@link #getId() id} as this object
	 */
	boolean equalsId(Object obj);

	/**
	 * Check if the given object's type and url match.
	 *
	 * @param obj
	 *            the object to compare (can be null)
	 * @return true if <code>obj</code> has the same {@link #getClass() type} and {@link #getUrl() url} as this object
	 */
	boolean equalsUrl(Object obj);

	/**
	 * Check if the given object's type and name match.
	 *
	 * @param obj
	 *            the object to compare (can be null)
	 * @return true if <code>obj</code> has the same {@link #getClass() type} and {@link #getName() name} as this object
	 */
	boolean equalsName(Object obj);

}