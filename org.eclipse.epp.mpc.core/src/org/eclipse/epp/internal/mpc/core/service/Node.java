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
	protected Categories categories;
	protected Tags tags;
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
	protected Ius ius;
	protected Platforms platforms;

	public Node() {
	}

	/**
	 * The number of times this node has been favorited.
	 */
	public Integer getFavorited() {
		return favorited;
	}

	public void setFavorited(Integer favorited) {
		this.favorited = favorited;
	}

	/**
	 * The number of times this node has been installed.
	 */
	public Integer getInstallsTotal() {
		return installsTotal;
	}

	public void setInstallsTotal(Integer installsTotal) {
		this.installsTotal = installsTotal;
	}

	/**
	 * The number of times this node has been installed recently (last 30 days).
	 */
	public Integer getInstallsRecent() {
		return installsRecent;
	}

	public void setInstallsRecent(Integer installsRecent) {
		this.installsRecent = installsRecent;
	}

	/**
	 * The type of listing, for example 'resource' or 'training'.
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * the categories of this listing.
	 */
	public Categories getCategories() {
		return categories;
	}

	public void setCategories(Categories categories) {
		this.categories = categories;
	}

	public Tags getTags() {
		return tags;
	}

	public void setTags(Tags tags) {
		this.tags = tags;
	}

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
	public String getShortdescription() {
		return shortdescription;
	}

	public void setShortdescription(String shortdescription) {
		this.shortdescription = shortdescription;
	}

	/**
	 * The description of this listing, may include HTML markup (escaped).
	 */
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * The number of seconds since the epoch. See http://us.php.net/manual/en/function.time.php for more details.
	 */
	public java.util.Date getCreated() {
		return created;
	}

	public void setCreated(java.util.Date created) {
		this.created = created;
	}

	/**
	 * It is the number of seconds since the epoch. See http://us.php.net/manual/en/function.time.php for more details.
	 */
	public java.util.Date getChanged() {
		return changed;
	}

	public void setChanged(java.util.Date changed) {
		this.changed = changed;
	}

	public Boolean getFoundationmember() {
		return foundationmember;
	}

	public void setFoundationmember(Boolean foundationmember) {
		this.foundationmember = foundationmember;
	}

	public String getHomepageurl() {
		return homepageurl;
	}

	public void setHomepageurl(String homepageurl) {
		this.homepageurl = homepageurl;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getCompanyname() {
		return companyname;
	}

	public void setCompanyname(String companyname) {
		this.companyname = companyname;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getEclipseversion() {
		return eclipseversion;
	}

	public void setEclipseversion(String eclipseversion) {
		this.eclipseversion = eclipseversion;
	}

	public String getSupporturl() {
		return supporturl;
	}

	public void setSupporturl(String supporturl) {
		this.supporturl = supporturl;
	}

	public String getUpdateurl() {
		return updateurl;
	}

	public void setUpdateurl(String updateurl) {
		this.updateurl = updateurl;
	}

	public Ius getIus() {
		return ius;
	}

	public void setIus(Ius ius) {
		this.ius = ius;
	}

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
