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

import java.util.Date;

/**
 * A node represents an entry on a marketplace. It is associated with one or more categories, under which it is listed.
 * Additionally, tags can be specified to define related technologies.
 * <p>
 * A node contains all information about a marketplace entry necessary to present it to users, including a
 * {@link #getName()}, {@link #getOwner() owner}, {@link #getShortdescription() short} and {@link #getBody() long}
 * description, {@link #getImage() icon} and optional {@link #getScreenshot()}. Some social feedback on the node entry
 * is provided by means of its {@link #getInstallsTotal() total} and {@link #getInstallsRecent() recent} installation
 * counts, as well as the number of {@link #getFavorited() favorite votes} it has received.
 * <p>
 * Nodes can describe different kinds of {@link #getType() contributions}, like installable Eclipse plug-ins, consulting
 * services and so on. In case of installable Eclipse plug-ins, the {@link #getIus() installable units} are provided for
 * installation from the node's {@link #getUpdateurl() update site}.
 *
 * @author David Green
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface INode extends IIdentifiable {

	/**
	 * The number of times this node has been favorited.
	 */
	Integer getFavorited();

	/**
	 * The number of times this node has been installed.
	 */
	Integer getInstallsTotal();

	/**
	 * The number of times this node has been installed recently (last 30 days).
	 */
	Integer getInstallsRecent();

	/**
	 * The type of listing, for example 'resource' or 'training'.
	 */
	String getType();

	/**
	 * the categories of this listing.
	 */
	ICategories getCategories();

	ITags getTags();

	String getOwner();

	/**
	 * The short description of this listing, may include HTML markup (escaped). Note that the short description may or
	 * may not be shorter than the body.
	 */
	String getShortdescription();

	/**
	 * The description of this listing, may include HTML markup (escaped).
	 */
	String getBody();

	/**
	 * The time of creation for this entry.
	 */
	Date getCreated();

	/**
	 * The last change time for this entry.
	 */
	Date getChanged();

	/**
	 * @return true if this node's owner is a foundation member, false otherwise, null if unknown.
	 */
	Boolean getFoundationmember();

	/**
	 * An URL for the homepage for this entry or its owner.
	 */
	String getHomepageurl();

	/**
	 * The image used as this entry's logo
	 */
	String getImage();

	/**
	 * An optional screenshot used in conjunction with the {@link #getBody() full description}.
	 */
	String getScreenshot();

	/**
	 * The version of the solution represented by this node. It is encouraged to use a valid OSGi version, but this
	 * isn't guaranteed.
	 */
	String getVersion();

	/**
	 * The license for the plug-in represented by this node, e.g. 'EPL'.
	 */
	String getLicense();

	/**
	 * The owner's company name
	 */
	String getCompanyname();

	/**
	 * The development status of this plug-in entry, e.g. "Production/Stable"
	 */
	String getStatus();

	/**
	 * A comma-separated list of supported Eclipse versions. Currently, this can take any form, although it is
	 * encouraged to use a comma-separated list of Eclipse versions like "3.6-3.8, 4.2.1, 4.2.2, 4.3".
	 * <p>
	 * This might get more standardized in the future.
	 */
	String getEclipseversion();

	/**
	 * @return a contact URL to get support for this entry
	 */
	String getSupporturl();

	/**
	 * The URL of an Eclipse update site containing this entry's {@link #getIus() installable units}.
	 */
	String getUpdateurl();

	/**
	 * The installable units that will be installed for this node.
	 */
	IIus getIus();

	/**
	 * The supported platforms under which this plug-in node can be installed.
	 */
	IPlatforms getPlatforms();

}