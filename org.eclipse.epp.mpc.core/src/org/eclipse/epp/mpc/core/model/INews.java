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
 * Information about a published news item for a catalog. This can be used to e.g. integrate a regularly published
 * newsletter with the marketplace wizard.
 *
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface INews {

	/**
	 * @return the URL of the published news item
	 */
	String getUrl();

	/**
	 * @return a short news title suitable to be presented before the actual news is opened.
	 */
	String getShortTitle();

	/**
	 * @return a timestamp for the last update to the news. Any change to the published news should result in an updated
	 *         timestamp.
	 */
	Long getTimestamp();

}