/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
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

import static org.junit.Assert.*;

import java.util.Collections;

import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceClientService;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.service.QueryHelper;
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


public class MarketplaceClientServiceTest extends AbstractMarketplaceWizardBotTest {

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
		display.asyncExec(new Runnable() {

			public void run() {
				service.open(config);
			}
		});

		initWizardBot();
		checkSelectedTab("Search");
	}

	@Test
	public void testOpenInstalled() throws Exception {
		display.asyncExec(new Runnable() {

			public void run() {
				service.openInstalled(config);
			}
		});

		initWizardBot();
		checkSelectedTab("Installed");
		//We should get a message dialog here. Let's check that and close it.
		bot.shell("No Extensions Found").close();
	}

	@Test
	public void testOpenNodes() throws Exception {
		display.asyncExec(new Runnable() {

			public void run() {
				service.open(config, Collections.singleton(QueryHelper.nodeById("206")));
			}
		});

		initWizardBot();
		checkSelectedTab("Search");
		itemBot("206");
	}

	@Test
	public void testOpenSearch() throws Exception {
		final IMarket toolsMarket = QueryHelper.marketByName("Tools");
		final ICategory mylynCategory = QueryHelper.categoryByName("Mylyn Connectors");

		display.asyncExec(new Runnable() {

			public void run() {
				service.openSearch(config, toolsMarket, mylynCategory, "WikiText");
			}
		});

		initWizardBot();
		checkSelectedTab("Search");

		SWTBotCombo marketCombo = bot.comboBox(0);
		SWTBotCombo categoryCombo = bot.comboBox(1);
		assertEquals("Tools", marketCombo.getText());
		assertEquals("Mylyn Connectors", categoryCombo.getText());

		SWTBotText searchText = bot.text(0);
		assertEquals("WikiText", searchText.getText());

		itemBot(NodeMatcher.withNameRegex(".*WikiText.*"));

	}

	@Test
	public void testOpenWithSelection() throws Exception {
		config.setInitialOperations(Collections.singletonMap("206", Operation.INSTALL));

		display.asyncExec(new Runnable() {

			public void run() {
				service.open(config);
			}
		});

		initWizardBot();
		checkSelectedTab("Search");
		SWTBot itemBot = itemBot("206");
		itemBot.button("Install Pending").isEnabled();
		bot.button("Install Now >").isEnabled();
	}

	@Test
	public void testOpenSelected() throws Exception {
		config.setInitialOperations(Collections.singletonMap("206", Operation.INSTALL));

		display.asyncExec(new Runnable() {

			public void run() {
				service.openSelected(config);
			}
		});

		initWizardBot();
		checkSelectedTab("Search");
		SWTBot itemBot = itemBot("206");
		itemBot.button("Install Pending").isEnabled();
		bot.button("Install Now >").isEnabled();
	}

	@Test
	public void testOpenProvisioning() throws Exception {
		config.setInitialOperations(Collections.singletonMap("206", Operation.INSTALL));

		display.asyncExec(new Runnable() {

			public void run() {
				service.openProvisioning(config);
			}
		});

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
}
