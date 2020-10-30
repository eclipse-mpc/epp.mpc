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
 *      The Eclipse Foundation  - initial API and implementation
 *      Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.model;

import org.eclipse.epp.mpc.core.model.INode;

/**
 * @author David Green
 * @author Benjamin Muskalla
 */
public class Node extends Identifiable implements INode {

	protected Integer favorited;
	protected Integer installsTotal;
	protected Integer installsRecent;
	protected String type;

	protected Categories categories = new Categories();

	protected Tags tags = new Tags();
	protected String owner;
	protected String shortdescription;
	protected String body;
	protected java.util.Date created;
	protected java.util.Date changed;
	protected Boolean foundationmember;
	protected String homepageurl;
	protected String image;
	protected String screenshot;
	protected String version;
	protected String license;
	protected String companyname;
	protected String status;
	protected String eclipseversion;
	protected String supporturl;
	protected String updateurl;

	protected Ius ius = new Ius();

	protected Platforms platforms = new Platforms();

	protected Boolean userFavorite;

	public Node() {
	}

	/**
	 * The number of times this node has been favorited.
	 */
	@Override
	public Integer getFavorited() {
		return favorited;
	}

	public void setFavorited(Integer favorited) {
		this.favorited = favorited;
	}

	@Override
	public Boolean getUserFavorite() {
		return userFavorite;
	}

	public void setUserFavorite(Boolean userFavorite) {
		this.userFavorite = userFavorite;
	}

	/**
	 * The number of times this node has been installed.
	 */
	@Override
	public Integer getInstallsTotal() {
		return installsTotal;
	}

	public void setInstallsTotal(Integer installsTotal) {
		this.installsTotal = installsTotal;
	}

	/**
	 * The number of times this node has been installed recently (last 30 days).
	 */
	@Override
	public Integer getInstallsRecent() {
		return installsRecent;
	}

	public void setInstallsRecent(Integer installsRecent) {
		this.installsRecent = installsRecent;
	}

	/**
	 * The type of listing, for example 'resource' or 'training'.
	 */
	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * the categories of this listing.
	 */
	@Override
	public Categories getCategories() {
		return categories;
	}

	public void setCategories(Categories categories) {
		this.categories = categories;
	}

	@Override
	public Tags getTags() {
		return tags;
	}

	public void setTags(Tags tags) {
		this.tags = tags;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * The short description of this listing, may include HTML markup (escaped). Note that the sort description may or
	 * may not be shorter than the body.
	 */
	@Override
	public String getShortdescription() {
		return shortdescription;
	}

	public void setShortdescription(String shortdescription) {
		this.shortdescription = shortdescription;
	}

	/**
	 * The description of this listing, may include HTML markup (escaped).
	 */
	@Override
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * The number of seconds since the epoch. See http://us.php.net/manual/en/function.time.php for more details.
	 */
	@Override
	public java.util.Date getCreated() {
		return created;
	}

	public void setCreated(java.util.Date created) {
		this.created = created;
	}

	/**
	 * It is the number of seconds since the epoch. See http://us.php.net/manual/en/function.time.php for more details.
	 */
	@Override
	public java.util.Date getChanged() {
		return changed;
	}

	public void setChanged(java.util.Date changed) {
		this.changed = changed;
	}

	@Override
	public Boolean getFoundationmember() {
		return foundationmember;
	}

	public void setFoundationmember(Boolean foundationmember) {
		this.foundationmember = foundationmember;
	}

	@Override
	public String getHomepageurl() {
		return homepageurl;
	}

	public void setHomepageurl(String homepageurl) {
		this.homepageurl = homepageurl;
	}

	@Override
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Override
	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	@Override
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	@Override
	public String getCompanyname() {
		return companyname;
	}

	public void setCompanyname(String companyname) {
		this.companyname = companyname;
	}

	@Override
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String getEclipseversion() {
		return eclipseversion;
	}

	public void setEclipseversion(String eclipseversion) {
		this.eclipseversion = eclipseversion;
	}

	@Override
	public String getSupporturl() {
		return supporturl;
	}

	public void setSupporturl(String supporturl) {
		this.supporturl = supporturl;
	}

	@Override
	public String getUpdateurl() {
		return updateurl;
	}

	public void setUpdateurl(String updateurl) {
		this.updateurl = updateurl;
	}

	@Override
	public Ius getIus() {
		return ius;
	}

	public void setIus(Ius ius) {
		this.ius = ius;
	}

	@Override
	public Platforms getPlatforms() {
		return platforms;
	}

	public void setPlatforms(Platforms platforms) {
		this.platforms = platforms;
	}

	@Override
	protected boolean equalsType(Object obj) {
		return obj instanceof INode;
	}
}
