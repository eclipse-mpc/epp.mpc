/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *  Yatta Solutions - bug 397004
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.service;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.model.Category;
import org.eclipse.epp.internal.mpc.core.model.Market;
import org.eclipse.epp.internal.mpc.core.model.News;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.RemoteMarketplaceService;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.epp.mpc.tests.Categories.RemoteTests;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class DefaultMarketplaceServiceTest {

	private DefaultMarketplaceService marketplaceService;

	@Before
	public void setUp() throws Exception {
		marketplaceService = new DefaultMarketplaceService();
		//configure client id (bug 397004), as well as eclipse and marketplace versions (bug 418865)
		Map<String, String> requestMetaParameters = ServiceLocator.computeDefaultRequestMetaParameters();
		marketplaceService.setRequestMetaParameters(requestMetaParameters);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void listMarkets() throws CoreException {
		List<? extends IMarket> markets = marketplaceService.listMarkets(new NullProgressMonitor());
		assertNotNull(markets);
		assertFalse(markets.isEmpty());

		for (IMarket market : markets) {
			assertNotNull(market.getId());
			assertNotNull(market.getUrl());
			assertNotNull(market.getName());
		}
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void getCategory() throws CoreException {
		List<? extends IMarket> markets = marketplaceService.listMarkets(new NullProgressMonitor());
		assertNotNull(markets);
		assertFalse(markets.isEmpty());

		final String marketName = "Tools";

		IMarket market = null;
		for (IMarket m : markets) {
			if (marketName.equals(m.getName())) {
				market = m;
				break;
			}
		}
		assertNotNull("Expected market " + marketName, market);

		assertFalse(market.getCategory().isEmpty());

		final String categoryName = "Mylyn Connectors";

		ICategory category = null;
		for (ICategory c : market.getCategory()) {
			if (categoryName.equals(c.getName())) {
				category = c;
				break;
			}
		}
		assertNotNull("Expected category " + categoryName, category);

		ICategory result = marketplaceService.getCategory(category, new NullProgressMonitor());
		assertNotNull(result);

		// FIXME: pending bug 302671
		// assertEquals(category.getId(),result.getId());
		// FIXME: pending bug 497242
		// assertEquals(category.getName(), result.getName());
		// assertEquals(category.getUrl(), result.getUrl());
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void testGetMarket() throws CoreException {
		//		Failing due to bug 302670: REST API market response inconsistency
		//		https://bugs.eclipse.org/bugs/show_bug.cgi?id=302670
		List<Market> markets = marketplaceService.listMarkets(new NullProgressMonitor());
		assertNotNull(markets);
		assertFalse(markets.isEmpty());

		final String marketName = "Tools";

		Market market = null;
		for (Market m : markets) {
			if (marketName.equals(m.getName())) {
				market = m;
			}
		}
		assertNotNull("Expected market " + marketName, market);

		Market result = marketplaceService.getMarket(market, new NullProgressMonitor());

		assertEquals(market.getId(), result.getId());
		assertEquals(market.getName(), result.getName());
		assertEquals(market.getUrl(), result.getUrl());
	}

	/**
	 * bug 302825 - Make sure that search URLs have the following form:<br/>
	 * <code>http://marketplace.eclipse.org/api/p/search/apachesolr_search/WikiText?filters=tid:38%20tid:31</code>
	 * <p>
	 * bug 397004 - If both market and category are provided, make sure market is listed first
	 */
	@Test
	public void computeRelativeSearchUrl() {
		DefaultMarketplaceService service = new DefaultMarketplaceService();

		String marketId = "31";
		String categoryId = "38";
		String query = "some query";

		Market market = new Market();
		market.setId(marketId);
		Category category = new Category();
		category.setId(categoryId);

		String apiSearchPrefix = DefaultMarketplaceService.API_SEARCH_URI_FULL;

		String searchUrl = service.computeRelativeSearchUrl(null, null, query, true);
		assertEquals(apiSearchPrefix + "some+query", searchUrl);

		searchUrl = service.computeRelativeSearchUrl(market, null, query, true);
		assertEquals(apiSearchPrefix + "some+query?filters=tid:" + marketId, searchUrl);

		searchUrl = service.computeRelativeSearchUrl(null, category, query, true);
		assertEquals(apiSearchPrefix + "some+query?filters=tid:" + categoryId, searchUrl);

		// bug 397004 - make sure market comes first for api
		searchUrl = service.computeRelativeSearchUrl(market, category, query, true);
		assertEquals(apiSearchPrefix + "some+query?filters=tid:" + marketId + "%20tid:" + categoryId, searchUrl);
		// bug 397004 - make sure category comes first for browser
		searchUrl = service.computeRelativeSearchUrl(market, category, query, false);
		assertEquals(DefaultMarketplaceService.API_SEARCH_URI + "some+query?filters=tid:" + categoryId + "%20tid:"
				+ marketId, searchUrl);

		searchUrl = service.computeRelativeSearchUrl(market, null, null, true);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + marketId + "/"
				+ RemoteMarketplaceService.API_URI_SUFFIX,
				searchUrl);

		searchUrl = service.computeRelativeSearchUrl(null, category, null, false);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + categoryId, searchUrl);

		// taxonomy uri is category-first for both API and browser
		searchUrl = service.computeRelativeSearchUrl(market, category, null, true);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + categoryId + "," + marketId + "/"
				+ RemoteMarketplaceService.API_URI_SUFFIX,
				searchUrl);
		searchUrl = service.computeRelativeSearchUrl(market, category, null, false);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + categoryId + "," + marketId, searchUrl);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void search() throws CoreException {
		ISearchResult result = search("Tools", "Editor", "snipmatch");
		assertNotNull(result);
		assertNotNull(result.getNodes());
		assertEquals(Integer.valueOf(1), result.getMatchCount());
		assertEquals(1, result.getNodes().size());

		INode node = result.getNodes().get(0);

		assertTrue(node.getName().startsWith("Snipmatch"));
		assertEquals("1743547", node.getId());
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void search_bug448453() throws CoreException {
		ISearchResult result = search(null, null, "play!");
		assertNotNull(result);
		assertNotNull(result.getNodes());
	}

	private ISearchResult search(String marketName, String categoryName, String queryText) throws CoreException {
		IMarket toolsMarket = marketName == null ? null : findMarket(marketName);
		ICategory mylynCategory = categoryName == null ? null : findCategory(toolsMarket, categoryName);

		ISearchResult result = marketplaceService.search(toolsMarket, mylynCategory, queryText,
				new NullProgressMonitor());
		return result;
	}

	private ICategory findCategory(IMarket toolsMarket, String categoryName) {
		ICategory namedCategory = null;
		for (ICategory category : toolsMarket.getCategory()) {
			if (categoryName.equals(category.getName())) {
				namedCategory = category;
				break;
			}
		}
		assertNotNull(namedCategory);
		return namedCategory;
	}

	private IMarket findMarket(String marketName) throws CoreException {
		List<? extends IMarket> markets = marketplaceService.listMarkets(new NullProgressMonitor());
		assertTrue(!markets.isEmpty());

		IMarket toolsMarket = null;
		for (IMarket market : markets) {
			if (marketName.equals(market.getName())) {
				toolsMarket = market;
				break;
			}
		}
		assertNotNull(toolsMarket);
		return toolsMarket;
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void featured() throws CoreException {
		ISearchResult result = marketplaceService.featured(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	@Ignore //top favorites are not exposed in MPC anymore - this fails with a 503 due to the varnish cache failing
	public void favorites() throws CoreException {
		ISearchResult result = marketplaceService.topFavorites(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void popular() throws CoreException {
		ISearchResult result = marketplaceService.popular(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void recent() throws CoreException {
		ISearchResult result = marketplaceService.recent(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void related() throws Exception {
		List<INode> basedOn = Arrays.asList(QueryHelper.nodeById("1139"), QueryHelper.nodeById("206"), QueryHelper
				.nodeById("1147"));
		related(basedOn);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void relatedEmpty() throws Exception {
		related(Collections.<INode> emptyList());
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void relatedNull() throws Exception {
		related(null);
	}

	private void related(List<INode> basedOn) throws Exception {
		MappedTransportFactory fileTransportFactory = MappedTransportFactory.get();
		try {
			String marketplaceRelatedApiUri = marketplaceService.getBaseUrl() + "/"
					+ DefaultMarketplaceService.API_RELATED_URI + "/" + RemoteMarketplaceService.API_URI_SUFFIX;
			//basically, we just expect any node list here.
			//FIXME use actual server api call
			fileTransportFactory.map(marketplaceRelatedApiUri, DefaultMarketplaceServiceTest.class.getResource(
					"xml/resources/related.xml").toExternalForm());
			setUp();
			{
				ISearchResult result = marketplaceService.related(basedOn, new NullProgressMonitor());
				assertSearchResultSanity(result);
			}
		} finally {
			fileTransportFactory.unregister();
		}
	}

	protected void assertSearchResultSanity(ISearchResult result) {
		assertNotNull(result);
		assertNotNull(result.getNodes());
		assertNotNull(result.getMatchCount());
		int promotedCount = 0;
		for (INode node : result.getNodes()) {
			if (node.getShortdescription() != null && node.getShortdescription().startsWith("**Promoted**")) {
				promotedCount++;
			}
		}
		assertTrue(result.getMatchCount() >= result.getNodes().size() - promotedCount);
		assertTrue(result.getNodes().size() > 0);

		Set<String> ids = new HashSet<>();
		for (INode node : result.getNodes()) {
			assertNotNull(node.getId());
			assertTrue(ids.add(node.getId()));
			assertNotNull(node.getName());
		}
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void news() throws CoreException, ParseException, MalformedURLException {
		News news = marketplaceService.news(new NullProgressMonitor());
		assertNotNull(news);
		Long timestamp = news.getTimestamp();
		assertNotNull(timestamp);
		assertThat(new Date(timestamp * 1000), greaterThan(new SimpleDateFormat("yyyy-MM-dd").parse("2014-06-24")));

		String shortTitle = news.getShortTitle();
		assertNotNull(shortTitle);

		String url = news.getUrl();
		assertNotNull(url);
		new URL(url);
	}

	@Test
	@org.junit.experimental.categories.Category(RemoteTests.class)
	public void getNodes() throws CoreException {
		INode idNode1 = QueryHelper.nodeById("206");//Mylyn
		INode idNode2 = QueryHelper.nodeById("1139");//Subversive
		INode idNode3 = QueryHelper.nodeById("252");//M2E
		INode urlNode = QueryHelper.nodeByUrl("https://marketplace.eclipse.org/content/egit-git-integration-eclipse");
		List<INode> query = Arrays.asList(idNode1, idNode2, urlNode, idNode3);

		List<INode> result = marketplaceService.getNodes(query, new NullProgressMonitor());
		assertEquals(query.size(), result.size());
		for (int i = 0; i < query.size(); i++) {
			INode queryNode = query.get(i);
			INode resultNode = query.get(i);
			if (queryNode.getId() != null) {
				assertEquals(queryNode.getId(), resultNode.getId());
			}
			if (queryNode.getUrl() != null) {
				assertEquals(queryNode.getUrl(), resultNode.getUrl());
			}
		}
	}
}
