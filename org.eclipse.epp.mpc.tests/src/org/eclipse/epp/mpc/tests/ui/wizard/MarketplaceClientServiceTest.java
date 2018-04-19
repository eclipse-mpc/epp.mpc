/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceClientService;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.epp.mpc.tests.Categories.RemoteTests;
import org.eclipse.epp.mpc.tests.Categories.UITests;
import org.eclipse.epp.mpc.tests.ui.wizard.matcher.NodeMatcher;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category({ RemoteTests.class, UITests.class })
public class MarketplaceClientServiceTest extends AbstractMarketplaceWizardBotTest {

	private static final String ITEM_ID = "1743547";

	private Display display;

	private IMarketplaceClientConfiguration config;

	private MarketplaceClientService service;

	@Override
	@Before
	public void setUp() {
		service = new MarketplaceClientService();
		config = service.newConfiguration();
		display = PlatformUI.getWorkbench().getDisplay();
	}

	@Override
	protected void launchMarketplaceWizard() {
		throw new UnsupportedOperationException();//this should NOT be called during test setup
	}

	@Test
	public void testClientServiceAvailable() {
		IMarketplaceClientService marketplaceClientService = MarketplaceClient.getMarketplaceClientService();
		assertNotNull(marketplaceClientService);
	}

	@Test
	public void testOpenDefault() throws Exception {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> service.open(config));

		initWizardBot();
		checkSelectedTab("Search");
	}

	@Test
	public void testOpenInstalled() throws Exception {
		display.asyncExec(() -> service.openInstalled(config));

		initWizardBot();
		checkSelectedTab("Installed");
		checkNoItems();
	}

	@Test
	public void testOpenNodes() throws Exception {
		display.asyncExec(() -> service.open(config, Collections.singleton(QueryHelper.nodeById(ITEM_ID))));

		initWizardBot();
		checkSelectedTab("Search");
		itemBot(ITEM_ID);
	}

	@Test
	public void testOpenSearch() throws Exception {
		final IMarket toolsMarket = QueryHelper.marketByName("Tools");
		final ICategory mylynCategory = QueryHelper.categoryByName("Editor");

		display.asyncExec(() -> service.openSearch(config, toolsMarket, mylynCategory, "snipmatch"));

		initWizardBot();
		checkSelectedTab("Search");

		SWTBotCombo marketCombo = bot.comboBox(0);
		SWTBotCombo categoryCombo = bot.comboBox(1);
		assertEquals("Tools", marketCombo.getText());
		assertEquals("Editor", categoryCombo.getText());

		SWTBotText searchText = bot.text(0);
		assertEquals("snipmatch", searchText.getText());

		itemBot(NodeMatcher.withNameRegex(".*Snipmatch.*"));

	}

	@Test
	public void testOpenWithSelection() throws Exception {
		config.setInitialOperations(Collections.singletonMap(ITEM_ID, Operation.INSTALL));

		display.asyncExec(() -> service.open(config));

		initWizardBot();
		checkSelectedTab("Search");
		SWTBot itemBot = itemBot(ITEM_ID);
		itemBot.button("Install Pending").isEnabled();
		bot.button("Install Now >").isEnabled();
	}

	@Test
	public void testOpenSelected() throws Exception {
		config.setInitialOperations(Collections.singletonMap(ITEM_ID, Operation.INSTALL));

		display.asyncExec(() -> service.openSelected(config));

		initWizardBot();
		checkSelectedTab("Search");
		SWTBot itemBot = itemBot(ITEM_ID);
		assertTrue(itemBot.button("Install Pending").isEnabled());
		assertTrue(bot.button("Install Now >").isEnabled());
	}

	@Test
	public void testOpenProvisioning() throws Exception {
		config.setInitialOperations(Collections.singletonMap(ITEM_ID, Operation.INSTALL));

		display.asyncExec(() -> service.openProvisioning(config));

		initWizardBot();

		//make sure we are on the second page and can proceed
		bot.label("Confirm Selected Features");
		bot.button("< Install More").isEnabled();
		bot.button("Confirm >").isEnabled();

		//make sure we have one top-level item with multiple children
		SWTBotTreeItem[] nodeItems = bot.tree().getAllItems();
		assertEquals(1, nodeItems.length);
		SWTBotTreeItem[] featureItems = nodeItems[0].getItems();
		assertTrue(featureItems.length > 0);
	}

	@Test
	public void testOpenFavorites() throws Exception {
		display.asyncExec(() -> service.openFavorites(config));

		initWizardBot();
		checkSelectedTab("Favorites");
	}
}
