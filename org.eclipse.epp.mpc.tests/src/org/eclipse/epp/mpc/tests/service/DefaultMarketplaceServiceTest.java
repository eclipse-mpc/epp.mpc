/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.Category;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.epp.internal.mpc.core.service.RemoteMarketplaceService;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * @author David Green
 * @author Carsten Reckord
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class DefaultMarketplaceServiceTest {

	private DefaultMarketplaceService marketplaceService;

	@Before
	public void setUp() throws Exception {
		marketplaceService = new DefaultMarketplaceService();
		Map<String, String> requestMetaParameters = new HashMap<String, String>();
		// bug 397004 - this is the only valid id for REST API calls
		requestMetaParameters.put(DefaultMarketplaceService.META_PARAM_CLIENT, MarketplaceClientCore.BUNDLE_ID);
		marketplaceService.setRequestMetaParameters(requestMetaParameters);
	}

	@Test
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
		assertEquals(category.getName(), result.getName());
		assertEquals(category.getUrl(), result.getUrl());
	}

	//
	//	@Test
	//	public void testGetMarket() throws CoreException {
	////		Failing due to bug 302670: REST API market response inconsistency
	////		https://bugs.eclipse.org/bugs/show_bug.cgi?id=302670
	//		List<Market> markets = marketplaceService.listMarkets(new NullProgressMonitor());
	//		assertNotNull(markets);
	//		assertFalse(markets.isEmpty());
	//
	//		final String marketName = "Tools";
	//
	//		Market market = null;
	//		for (Market m: markets) {
	//			if (marketName.equals(m.getName())) {
	//				market = m;
	//			}
	//		}
	//		assertNotNull("Expected market "+marketName,market);
	//
	//		Market result = marketplaceService.getMarket(market, new NullProgressMonitor());
	//
	//		assertEquals(market.getId(),result.getId());
	//		assertEquals(market.getName(),result.getName());
	//		assertEquals(market.getUrl(),result.getUrl());
	//	}
	//

	/**
	 * bug 302825 - Make sure that search URLs have the following form:<br/>
	 * <code>http://marketplace.eclipse.org/api/p/search/apachesolr_search/WikiText?filters=tid:38%20tid:31</code>
	 * <p>
	 * bug 397004 - If both market and category are provided, make sure market is listed first
	 */
	@Test
	public void computeRelativeSearchUrl() {
		DefaultMarketplaceService service = new DefaultMarketplaceService();

		Market market = new Market();
		market.setId("31");
		Category category = new Category();
		category.setId("38");
		String query = "some query";

		String apiSearchPrefix = DefaultMarketplaceService.API_SEARCH_URI_FULL;

		String searchUrl = service.computeRelativeSearchUrl(null, null, query, true);
		assertEquals(apiSearchPrefix + "some+query", searchUrl);

		searchUrl = service.computeRelativeSearchUrl(market, null, query, true);
		assertEquals(apiSearchPrefix + "some+query?filters=tid:31", searchUrl);

		searchUrl = service.computeRelativeSearchUrl(null, category, query, true);
		assertEquals(apiSearchPrefix + "some+query?filters=tid:38", searchUrl);

		// bug 397004 - make sure market comes first for api
		searchUrl = service.computeRelativeSearchUrl(market, category, query, true);
		assertEquals(apiSearchPrefix + "some+query?filters=tid:31%20tid:38", searchUrl);
		// bug 397004 - make sure category comes first for browser
		searchUrl = service.computeRelativeSearchUrl(market, category, query, false);
		assertEquals(DefaultMarketplaceService.API_SEARCH_URI + "some+query?filters=tid:38%20tid:31", searchUrl);

		searchUrl = service.computeRelativeSearchUrl(market, null, null, true);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + "31/" + RemoteMarketplaceService.API_URI_SUFFIX,
				searchUrl);

		searchUrl = service.computeRelativeSearchUrl(null, category, null, false);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + "38", searchUrl);

		// taxonomy uri is category-first for both API and browser
		searchUrl = service.computeRelativeSearchUrl(market, category, null, true);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + "38,31/" + RemoteMarketplaceService.API_URI_SUFFIX,
				searchUrl);
		searchUrl = service.computeRelativeSearchUrl(market, category, null, false);
		assertEquals(DefaultMarketplaceService.API_TAXONOMY_URI + "38,31", searchUrl);
	}

	@Test
	public void search() throws CoreException {
		List<? extends IMarket> markets = marketplaceService.listMarkets(new NullProgressMonitor());
		assertTrue(!markets.isEmpty());

		IMarket toolsMarket = null;
		for (IMarket market : markets) {
			if ("Tools".equals(market.getName())) {
				toolsMarket = market;
				break;
			}
		}
		assertNotNull(toolsMarket);
		ICategory mylynCategory = null;
		for (ICategory category : toolsMarket.getCategory()) {
			if ("Mylyn Connectors".equals(category.getName())) {
				mylynCategory = category;
				break;
			}
		}
		assertNotNull(mylynCategory);

		ISearchResult result = marketplaceService.search(toolsMarket, mylynCategory, "WikiText",
				new NullProgressMonitor());
		assertNotNull(result);
		assertNotNull(result.getNodes());
		assertEquals(Integer.valueOf(1), result.getMatchCount());
		assertEquals(1, result.getNodes().size());

		INode node = result.getNodes().get(0);

		assertTrue(node.getName().startsWith("Mylyn WikiText"));
		assertEquals("1065", node.getId());
	}

	@Test
	public void featured() throws CoreException {
		ISearchResult result = marketplaceService.featured(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	public void favorites() throws CoreException {
		ISearchResult result = marketplaceService.favorites(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	public void popular() throws CoreException {
		//	 NOTE: this test is failing until the following bug is fixed
		//			bug 303275: REST API popular returns count of 6 with 10 nodes returned
		ISearchResult result = marketplaceService.popular(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	@Test
	public void recent() throws CoreException {
		ISearchResult result = marketplaceService.recent(new NullProgressMonitor());
		assertSearchResultSanity(result);
	}

	protected void assertSearchResultSanity(ISearchResult result) {
		assertNotNull(result);
		assertNotNull(result.getNodes());
		assertNotNull(result.getMatchCount());
		assertTrue(result.getMatchCount() >= result.getNodes().size());
		assertTrue(result.getNodes().size() > 0);

		Set<String> ids = new HashSet<String>();
		for (INode node : result.getNodes()) {
			assertNotNull(node.getId());
			assertTrue(ids.add(node.getId()));
			assertNotNull(node.getName());
		}
	}

	@Test
	public void news() throws CoreException {
		//TODO test once API is live on the server
	}
}
