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
 *     The Eclipse Foundation  - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.service.xml;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.model.Favorites;
import org.eclipse.epp.internal.mpc.core.model.Featured;
import org.eclipse.epp.internal.mpc.core.model.Marketplace;
import org.eclipse.epp.internal.mpc.core.model.News;
import org.eclipse.epp.internal.mpc.core.model.Recent;
import org.eclipse.epp.internal.mpc.core.model.Related;
import org.eclipse.epp.internal.mpc.core.model.Search;
import org.eclipse.epp.internal.mpc.core.service.MarketplaceUnmarshaller;
import org.eclipse.epp.mpc.core.model.ICatalog;
import org.eclipse.epp.mpc.core.model.ICatalogBranding;
import org.eclipse.epp.mpc.core.model.ICatalogs;
import org.eclipse.epp.mpc.core.model.ICategories;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ITag;
import org.eclipse.epp.mpc.core.service.UnmarshalException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author David Green
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public class UnmarshallerTest {

	private MarketplaceUnmarshaller unmarshaller;

	@Before
	public void before() throws SAXException, ParserConfigurationException {
		unmarshaller = new MarketplaceUnmarshaller();
	}

	@Test
	public void marketplaceRoot() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/xml
		Object model = processResource("resources/marketplace-root.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		assertEquals(4, marketplace.getMarket().size());

		IMarket market = marketplace.getMarket().get(0);

		assertEquals("31", market.getId());
		assertEquals("Tools", market.getName());
		assertEquals("http://www.eclipseplugincentral.net/category/markets/tools", market.getUrl());

		assertEquals(36, market.getCategory().size());
		ICategory category = market.getCategory().get(10);
		assertEquals("24", category.getId());
		assertEquals("IDE", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/taxonomy/term/24%2C31", category.getUrl());
	}

	@Test
	public void categoryTaxonomy() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/taxonomy/term/38,31/xml
		Object model = processResource("resources/category-taxonomy.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		assertEquals(1, marketplace.getCategory().size());

		ICategory category = marketplace.getCategory().get(0);
		assertEquals("38,31", category.getId());
		assertEquals("Mylyn Connectors", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/taxonomy/term/38%2C31", category.getUrl());

		assertEquals(9, category.getNode().size());

		INode node = category.getNode().get(0);
		//		<node id="641" name="Tasktop Pro">
		//        <url>http://www.eclipseplugincentral.net/content/tasktop-pro</url>
		//        <favorited>3</favorited>
		//      </node>
		assertEquals("641", node.getId());
		assertEquals("Tasktop Pro", node.getName());
		assertEquals("http://www.eclipseplugincentral.net/content/tasktop-pro", node.getUrl());
	}

	@Test
	public void node() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/content/mylyn-wikitext-lightweight-markup-editing-tools-and-framework/xml
		Object model = processResource("resources/node.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		assertEquals(1, marketplace.getNode().size());

		INode node = marketplace.getNode().get(0);
		assertEquals("1065", node.getId());
		assertEquals("Mylyn WikiText - Lightweight Markup Editing, Tools and Framework", node.getName());
		assertEquals(
				"http://www.eclipseplugincentral.net/content/mylyn-wikitext-lightweight-markup-editing-tools-and-framework",
				node.getUrl());

		assertNotNull(node.getBody());
		assertTrue(node.getBody().startsWith("Mylyn WikiText is a"));
		assertTrue(node.getBody().endsWith("FAQ</a>."));

		assertNotNull(node.getCategories());
		assertEquals(5, node.getCategories().getCategory().size());
		ICategory category = node.getCategories().getCategory().get(1);
		// <category name='Tools'>/taxonomy/term/17</category>
		assertEquals("Tools", category.getName());
		// FIXME category id.

		assertNotNull(node.getCreated());
		assertEquals(1259955243L, node.getCreated().getTime() / 1000);
		assertEquals(Integer.valueOf(3), node.getFavorited());
		assertEquals(Boolean.TRUE, node.getFoundationmember());
		assertNotNull(node.getChanged());
		assertEquals(1259964722L, node.getChanged().getTime() / 1000);
		assertEquals("David Green", node.getOwner());
		assertEquals("resource", node.getType());

	}

	@Test
	public void featured() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/api/v2/featured
		Object model = processResource("resources/featured.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		Featured featured = marketplace.getFeatured();
		assertNotNull(featured);
		assertEquals(Integer.valueOf(6), featured.getCount());

		assertEquals(6, featured.getNode().size());
		INode node = featured.getNode().get(0);
		assertEquals("248", node.getId());
		assertEquals("eUML2 free edition", node.getName());
		assertEquals("http://www.eclipseplugincentral.net/content/euml2-free-edition", node.getUrl());
		assertEquals("resource", node.getType());
		ICategories categories = node.getCategories();
		assertNotNull(categories);
		assertEquals(1, categories.getCategory().size());
		ICategory category = categories.getCategory().get(0);
		assertEquals("19", category.getId());
		assertEquals("UML", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/taxonomy/term/19", category.getUrl());
		assertEquals("Yves YANG", node.getOwner());
		assertEquals(Integer.valueOf(0), node.getFavorited());
		assertNotNull(node.getBody());
		//bug 303149		assertTrue(node.getBody().startsWith("<P><STRONG>eUML2 for Java<"));
		//bug 303149		assertTrue(node.getBody().endsWith("</LI></UL>"));
		assertTrue(node.getFoundationmember());
		assertEquals("http://www.soyatec.com/", node.getHomepageurl());
		assertEquals("http://www.soyatec.com/euml2/images/product_euml2_110x80.png", node.getImage());
		assertEquals("3.4", node.getVersion());
		assertEquals("Free for non-commercial use", node.getLicense());
		assertEquals("Soyatec", node.getCompanyname());
		assertEquals("Mature", node.getStatus());
		assertEquals("3.4.x/3.5.x", node.getEclipseversion());
		assertEquals("http://www.soyatec.com/forum", node.getSupporturl());
		assertEquals("http://www.soyatec.com/update", node.getUpdateurl());

	}

	@Test
	public void search() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/api/v2/search/apachesolr_search/test?filters=tid:16%20tid:31
		Object model = processResource("resources/search.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		Search search = marketplace.getSearch();
		assertNotNull(search);

		assertEquals("test", search.getTerm());
		assertEquals("http://www.eclipseplugincentral.net/search/apachesolr/test?filters=tid%3A16%20tid%3A31",
				search.getUrl());
		assertEquals(Integer.valueOf(62), search.getCount());
		assertEquals(7, search.getNode().size());
		INode node = search.getNode().get(0);

		assertEquals("983", node.getId());
		assertEquals("Run All Tests", node.getName());
		assertEquals("http://www.eclipseplugincentral.net/content/run-all-tests", node.getUrl());
		assertEquals("resource", node.getType());
		ICategories categories = node.getCategories();
		assertNotNull(categories);
		assertEquals(1, categories.getCategory().size());
		ICategory category = categories.getCategory().get(0);
		assertEquals("16", category.getId());
		assertEquals("Testing", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/taxonomy/term/16", category.getUrl());
		assertEquals("ipreuss", node.getOwner());
		assertNotNull(node.getBody());
		assertEquals("Allows the execution of JUnit tests for several projects at once.", node.getBody());
		assertTrue(!node.getFoundationmember());
		assertEquals("https://sourceforge.net/projects/e-rat/", node.getHomepageurl());
		assertNull(node.getImage());
		assertEquals("1.0.1", node.getVersion());
		assertEquals("Other", node.getLicense());
		assertEquals("Ilja Preu\u00DF", node.getCompanyname());
		assertEquals("Production/Stable", node.getStatus());
		assertEquals("3.5", node.getEclipseversion());
		assertEquals("https://sourceforge.net/projects/e-rat/support", node.getSupporturl());
		assertEquals("http://e-rat.sf.net/updatesite", node.getUpdateurl());

		assertEquals(Integer.valueOf(136), node.getFavorited());
		assertEquals(Integer.valueOf(299995), node.getInstallsTotal());
		assertEquals(Integer.valueOf(34540), node.getInstallsRecent());

		INode lastNode = search.getNode().get(search.getNode().size() - 1);

		assertEquals("1011", lastNode.getId());
		assertEquals("JUnit Flux", lastNode.getName());

		assertNull(lastNode.getFavorited());
		assertNull(lastNode.getInstallsTotal());
		assertNull(lastNode.getInstallsRecent());
	}

	@Test
	public void favorites() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/favorites/top/api/p

		Object model = processResource("resources/favorites.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		Favorites favorites = marketplace.getFavorites();
		assertNotNull(favorites);
		assertEquals(Integer.valueOf(6), favorites.getCount());

		assertEquals(6, favorites.getNode().size());

		INode node = favorites.getNode().get(0);

		assertEquals("206", node.getId());
		assertEquals("Mylyn", node.getName());
		assertEquals("http://www.eclipseplugincentral.net/content/mylyn", node.getUrl());
		assertEquals("resource", node.getType());
		ICategories categories = node.getCategories();
		assertNotNull(categories);
		assertEquals(1, categories.getCategory().size());
		ICategory category = categories.getCategory().get(0);
		assertEquals("18", category.getId());
		assertEquals("UI", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/taxonomy/term/18", category.getUrl());
		assertEquals("Robert Elves", node.getOwner());
		assertEquals(Integer.valueOf(16), node.getFavorited());
		assertNotNull(node.getBody());
		assertEquals(
				"Mylyn is a task-focused interface for Eclipse that reduces information overload and makes multi-tasking easy. It does this by making tasks a first class part of Eclipse, and integrating rich and offline editing for repositories such as Bugzilla, Trac, and JIRA. Once your tasks are integrated, Mylyn monitors your work activity to identify information relevant to the task-at-hand, and uses this task context to focus the Eclipse UI on the interesting information, hide the uninteresting, and automatically find what&#039;s related. This puts the information you need to get work done at your fingertips and improves productivity by reducing searching, scrolling, and navigation. By making task context explicit Mylyn also facilitates multitasking, planning, reusing past efforts, and sharing expertise. ",
				node.getBody());
		assertTrue(node.getFoundationmember());
		assertEquals("http://eclipse.org/mylyn", node.getHomepageurl());
		assertEquals("http://www.eclipse.org/mylyn/images/image-epic.gif", node.getImage());
		assertEquals("3.3", node.getVersion());
		assertEquals("EPL", node.getLicense());
		assertEquals("Eclipse.org", node.getCompanyname());
		assertEquals("Production/Stable", node.getStatus());
		assertEquals("3.5, 3.4 and 3.3", node.getEclipseversion());
		assertEquals("http://eclipse.org/mylyn/community/", node.getSupporturl());
		assertEquals("http://download.eclipse.org/tools/mylyn/update/e3.4", node.getUpdateurl());
	}

	@Test
	public void recent() throws IOException, UnmarshalException {
		// from http://www.eclipseplugincentral.net/featured/top/api/p

		Object model = processResource("resources/recent.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		Recent recent = marketplace.getRecent();
		assertNotNull(recent);
		assertEquals(Integer.valueOf(6), recent.getCount());

		assertEquals(6, recent.getNode().size());

		INode node = recent.getNode().get(0);

		assertEquals("1091", node.getId());
		assertEquals("API Demonstration Listing", node.getName());
		assertEquals("http://www.eclipseplugincentral.net/content/api-demonstration-listing", node.getUrl());

		assertEquals("resource", node.getType());
		ICategories categories = node.getCategories();
		assertNotNull(categories);
		assertEquals(6, categories.getCategory().size());
		ICategory category = categories.getCategory().get(0);
		assertEquals("3", category.getId());
		assertEquals("Database", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/taxonomy/term/3", category.getUrl());
		category = categories.getCategory().get(5);
		assertEquals("38", category.getId());
		assertEquals("Mylyn Connectors", category.getName());
		assertEquals("http://www.eclipseplugincentral.net/category/categories/mylyn-connectors", category.getUrl());

		assertEquals("admin", node.getOwner());
		assertEquals(Integer.valueOf(0), node.getFavorited());
		assertNotNull(node.getBody());
		assertTrue(node.getBody().startsWith("Lorem ipsum dolor"));
		assertTrue(node.getBody().endsWith("vitae aliquam lectus."));
		assertTrue(node.getFoundationmember());
		assertEquals("http://marketplace.eclipse.org/xmlapi", node.getHomepageurl());
		assertEquals("http://marketplace.eclipse.org/sites/default/files/equinox.png", node.getImage());
		assertEquals("1.0", node.getVersion());
		assertEquals("EPL", node.getLicense());
		assertEquals("Eclipse Foundation Inc.", node.getCompanyname());
		assertEquals("Mature", node.getStatus());
		assertEquals("3.5", node.getEclipseversion());
		assertEquals("http://marketplace.eclipse.org/support", node.getSupporturl());
		assertEquals("http://update.eclipse.org/marketplace", node.getUpdateurl());

		{
			String[] expectedIus = new String[] { "org.eclipse.one.one", "org.eclipse.one.two", "org.eclipse.two.one",
					"org.eclipse.three.one", };
			assertNotNull(node.getIus());
			assertEquals(expectedIus.length, node.getIus().getIu().size());
			for (int x = 0; x < expectedIus.length; ++x) {
				assertEquals(expectedIus[x], node.getIus().getIu().get(x));
			}
		}
		{
			String[] expectedPlatforms = new String[] { "Windows", "Mac", "Linux/GTK", };
			assertNotNull(node.getPlatforms());
			assertEquals(expectedPlatforms.length, node.getPlatforms().getPlatform().size());
			for (int x = 0; x < expectedPlatforms.length; ++x) {
				assertEquals(expectedPlatforms[x], node.getPlatforms().getPlatform().get(x));
			}
		}

	}

	public void related() throws Exception {
		Object model = processResource("resources/related.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);
		Marketplace marketplace = (Marketplace) model;

		Related related = marketplace.getRelated();
		assertNotNull(related);
		assertEquals(Integer.valueOf(6), related.getCount());

		assertEquals(6, related.getNode().size());
	}

	@Test
	public void tags() throws Exception {
		Object model = processResource("resources/node.xml");
		Marketplace marketplace = (Marketplace) model;
		INode node = marketplace.getNode().get(0);

		assertNotNull(node.getTags());
		assertEquals(5, node.getCategories().getCategory().size());
		ITag tag = node.getTags().getTags().get(3);
		assertEquals("mylyn", tag.getName());
		assertEquals("88", tag.getId());
		assertEquals("http://marketplace.eclipse.org/category/free-tagging/mylyn", tag.getUrl());
	}

	@Test
	public void marketplaceCatalogs() throws IOException, UnmarshalException {
		Object model = processResource("resources/catalogs.xml");
		assertNotNull(model);
		assertTrue(model instanceof ICatalogs);
		ICatalogs catalogs = (ICatalogs) model;

		assertEquals(3, catalogs.getCatalogs().size());

		//	     <catalog id="35656" title="Marketplace Catalog" url="http://marketplace.eclipse.org" selfContained="1"  dependencyRepository="http://download.eclipse.org/releases/helios">
		//	        <description>Here is a description</description>
		//	        <icon>http://marketplace.eclipse.org/sites/default/files/jacket.jpg</icon>
		//	        <wizard title="Eclipse Marketplace Catalog">
		//	          <icon>http://marketplace.eclipse.org/sites/default/files/giant-rabbit2.jpg</icon>
		//	          <searchtab enabled='1'>Search</searchtab>
		//	          <populartab enabled='1'>Popular</populartab>
		//	          <recenttab enabled='1'>Recent</recenttab>
		//	        </wizard>
		//	      </catalog>

		ICatalog catalog = catalogs.getCatalogs().get(0);
		assertEquals("35656", catalog.getId());
		assertEquals("Marketplace Catalog", catalog.getName());
		assertEquals("http://marketplace.eclipse.org", catalog.getUrl());
		assertEquals("Here is a description", catalog.getDescription());
		assertTrue(catalog.isSelfContained());
		assertEquals("http://marketplace.eclipse.org/sites/default/files/marketplace32.png", catalog.getImageUrl());
		assertEquals("http://download.eclipse.org/releases/helios", catalog.getDependencyRepository());

		ICatalogBranding branding = catalog.getBranding();
		assertNotNull(branding);
		assertEquals("Eclipse Marketplace Catalog", branding.getWizardTitle());
		assertEquals("http://marketplace.eclipse.org/sites/default/files/giant-rabbit2.jpg", branding.getWizardIcon());
		assertEquals("Search", branding.getSearchTabName());
		assertEquals("Popular", branding.getPopularTabName());
		assertEquals("Recent", branding.getRecentTabName());
		assertTrue(branding.hasSearchTab());
		assertFalse(branding.hasPopularTab());
		assertTrue(branding.hasRecentTab());

		INews news = catalog.getNews();
		assertNotNull(news);
		assertEquals("http://marketplace.eclipse.org/news", news.getUrl());
		assertEquals("News", news.getShortTitle());
		assertEquals(Long.valueOf(1363181064000l), news.getTimestamp());
		assertNull(catalogs.getCatalogs().get(1).getNews());
		assertNull(catalogs.getCatalogs().get(2).getNews());
	}

	@Test
	public void news() throws IOException, UnmarshalException {
		Object model = processResource("resources/news.xml");
		assertNotNull(model);
		assertTrue(model instanceof Marketplace);

		Marketplace marketplace = (Marketplace) model;
		News news = marketplace.getNews();
		assertNotNull(news);
		assertEquals("http://marketplace.eclipse.org/news", news.getUrl());
		assertEquals("News", news.getShortTitle());
		assertEquals(Long.valueOf(1363181064000l), news.getTimestamp());
	}

	@Test(expected = UnmarshalException.class)
	public void invalidLeadingSpace() throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		buffer.put("  ".getBytes("UTF-8"));
		buffer = readResource("resources/catalogs.xml", buffer);
		buffer.flip();
		process(buffer);
	}

	@Test(expected = UnmarshalException.class)
	public void invalidLeadingBytes() throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		buffer = readResource("resources/catalogs.xml", buffer);
		buffer.flip();
		process(buffer);
	}

	@Test(expected = UnmarshalException.class)
	public void invalidBytes() throws Exception {
		ByteBuffer buffer = readResource("resources/catalogs.xml", null);
		buffer.put(45, (byte) 0);
		buffer.put(46, (byte) 0);
		buffer.put(120, (byte) 0);
		buffer.put(121, (byte) 0);
		buffer.flip();
		process(buffer);
	}

	@Test
	public void invalidLeadingBytesErrorMessage() throws Exception {
		try {
			invalidLeadingBytes();
			fail("Expected UnmarshalException");
		} catch (UnmarshalException e) {
			IStatus contentChild = getErrorContentInfo(e);
			assertThat(contentChild.getMessage(), containsString("<?xml version='1.0' encoding='UTF-8'?>"));
		}
	}

	@Test
	public void invalidBytesErrorMessage() throws Exception {
		try {
			invalidBytes();
			fail("Expected UnmarshalException");
		} catch (UnmarshalException e) {
			IStatus contentChild = getErrorContentInfo(e);
			assertThat(contentChild.getMessage(), containsString("<mar??tplace>"));
		}
	}

	@Test(expected = UnmarshalException.class)
	public void emptyStream() throws Exception {
		process(new byte[0]);
	}

	@Test(expected = UnmarshalException.class)
	public void invalidContent() throws Exception {
		process("This is some arbitrary test content\ninstead of the expected\nmarketplace rest xml data.");
	}

	@Test
	public void invalidContentErrorMessage() throws Exception {
		try {
			invalidContent();
			fail("Expected UnmarshalException");
		} catch (UnmarshalException e) {
			IStatus contentChild = getErrorContentInfo(e);
			assertThat(contentChild.getMessage(), containsString("This is some arbitrary test content"));
		}
	}

	private static IStatus getErrorContentInfo(UnmarshalException e) {
		IStatus status = e.getStatus();
		assertTrue(status.isMultiStatus());
		IStatus[] children = status.getChildren();
		assertTrue(children.length > 0);
		IStatus contentChild = children[0];
		return contentChild;
	}

	private static ByteBuffer readResource(String resource, ByteBuffer buffer) throws IOException {
		ReadableByteChannel in = Channels.newChannel(getResourceAsStream(resource));
		if (buffer == null) {
			buffer = ByteBuffer.allocate(32768);
		}
		while (true) {
			int read = in.read(buffer);
			if (read == -1) {
				break;
			}
			if (buffer.remaining() < read) {
				ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + Math.max(8192, 2 * read));
				buffer.flip();
				newBuffer.put(buffer);
				buffer = newBuffer;
			}
		}
		return buffer;
	}

	private static InputStream getResourceAsStream(String resource) {
		InputStream in = UnmarshallerTest.class.getResourceAsStream(resource);
		if (in == null) {
			throw new IllegalStateException(resource);
		}
		return in;
	}

	private Object processResource(String resource) throws IOException, UnmarshalException {
		InputStream in = getResourceAsStream(resource);
		return process(in);
	}

	private Object process(InputStream in) throws IOException, UnmarshalException {
		try {
			return unmarshaller.unmarshal(in, Object.class, new NullProgressMonitor());
		} finally {
			in.close();
		}
	}

	private Object process(String content) throws IOException, UnmarshalException {
		return process(content.getBytes("UTF-8"));
	}

	private Object process(ByteBuffer buffer) throws IOException, UnmarshalException {
		InputStream in = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.remaining());
		return process(in);
	}

	private Object process(byte[] content) throws IOException, UnmarshalException {
		InputStream in = new ByteArrayInputStream(content);
		return process(in);
	}
}
