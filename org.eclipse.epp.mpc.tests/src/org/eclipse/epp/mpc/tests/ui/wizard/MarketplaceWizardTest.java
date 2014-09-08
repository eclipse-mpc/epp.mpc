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

import java.util.List;

import org.eclipse.epp.internal.mpc.ui.wizards.DiscoveryItem;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplacePage;
import org.eclipse.epp.mpc.tests.ui.wizard.matcher.NodeMatcher;
import org.eclipse.epp.mpc.tests.ui.wizard.widgets.SWTBotClickableStyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotBrowser;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLink;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Ignore;
import org.junit.Test;


public class MarketplaceWizardTest extends AbstractMarketplaceWizardBotTest {

	@Test
	public void testSelectMarket() {
		String term = "Tools";
		filterMarket(term);
		filterMarket(null);
	}

	@Test
	public void testSelectCategory() {
		filterCategory("Modeling");
		filterCategory(null);
	}

	@Test
	public void testSearch() {
		search("Mylyn");
		itemBot(NodeMatcher.withNameRegex(".*Mylyn.*"));
	}

	@Test
	public void testSearchTag() {
		SWTBotClickableStyledText tagsLabel = SWTBotClickableStyledText.from(bot.styledTextWithId(
				DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_TAGS));
		StyleRange linkRange = findLink(tagsLabel);
		String tag = getText(linkRange, tagsLabel);
		tagsLabel.click(linkRange);
		waitForWizardProgress();
		String searchTerm = searchField().getText();
		assertEquals(tag, searchTerm);
	}

	@Test
	public void testSearchMarketplaceUrl() {
		search("http://marketplace.eclipse.org/content/mylyn");
		itemBot(NodeMatcher.withNameRegex(".*Mylyn.*"));
	}

	@Test
	public void testSelectToInstall() {
		selectToInstall(2);
		deselectPending(2);
	}

	@Test
	public void testShowSelected() {
		selectToInstall(3);
		SWTBotLink link = bot.link("<a>3 solutions selected</a>");
		link.click();
		//wait for the action to be processed
		bot.waitUntil(new DefaultCondition() {
			public boolean test() throws Exception {
				List<Widget> items = this.bot.getFinder().findControls(NodeMatcher.any());
				return items.size() == 3;
			}

			public String getFailureMessage() {
				return "Not getting expected selection";
			}
		}, 10000);
	}

	//TODO conditional on embedded browser availability
	//FIXME
	@Ignore("Tooltip doesn't stay open")
	@Test
	public void testMoreInfoLearnMore() {
		SWTBotClickableStyledText descriptionLabel = SWTBotClickableStyledText.from(bot.styledTextWithId(
				DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_DESCRIPTION));
		StyleRange linkRange = findLink(descriptionLabel, "more\u00a0info");
		bot.sleep(5000);
		descriptionLabel.click(linkRange);
		bot.sleep(5000);
		SWTBotShell tooltip = bot.shellWithId(DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_OVERVIEW);
		SWTBotLink moreLink = tooltip.bot().linkWithId(DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_LEARNMORE);
		moreLink.click();

		checkMarketplaceBrowser();
	}

	//TODO conditional on embedded browser availability
	@Test
	public void testNoninstallableLearnMore() {
		searchField().setFocus();
		searchField().setText("nodeclipse");
		filterMarket("RCP Applications");
		SWTBotClickableStyledText learnMoreLabel = SWTBotClickableStyledText.from(bot.styledTextWithId(
				DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_LEARNMORE));
		StyleRange linkRange = findLink(learnMoreLabel);
		learnMoreLabel.click(linkRange);

		checkMarketplaceBrowser();
	}

	private void checkMarketplaceBrowser() {
		SWTBotBrowser browser = marketplaceBrowser();
		tryWaitForBrowser(browser);
		String url = browser.getUrl();
		assertTrue(url.contains("://marketplace.eclipse.org/content/"));
		assertTrue(wizardShell.isOpen());//wizard is still open
		assertFalse(wizardShell.isActive());//but no longer the active shell
	}

	@Ignore("Temporarily disabled due to build server problems - bug 443493")
	@Test
	public void testNews() {
		bot.tabItemWithId(MarketplacePage.WIDGET_ID_KEY, MarketplacePage.WIDGET_ID_TAB_NEWS).activate();
		bot.sleep(500);
		tryWaitForBrowser(bot.browser());
		String url = bot.browser().getUrl();
		assertTrue(url.contains("newsletter"));
	}

	@Test
	public void testRecentBackToSearch() {
		bot.tabItem("Recent").activate();
		waitForWizardProgress();
		testSearchTag();
	}

	@Test
	public void testRecent() {
		bot.tabItem("Recent").activate();
		waitForWizardProgress();
		//TODO test something useful
	}

	@Test
	public void testPopular() {
		bot.tabItem("Popular").activate();
		waitForWizardProgress();
		//TODO test something useful
	}

	//FIXME
	@Ignore("Tooltip doesn't stay open")
	@Test
	public void testFavorite() {
		SWTBotButton favorite = bot.buttonWithId(DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_RATING);
		bot.sleep(5000);
		favorite.click();
		SWTBotShell tooltip = bot.shellWithId(DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_RATING);
		tooltip.bot().button("Continue >");//.click();
		//TODO test something useful - clicking would open external browser, which is not good for tests
	}

	@Test
	public void testShare() {
		SWTBotButton share = bot.buttonWithId(DiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_SHARE);
		share.click();
		share.contextMenu("Twitter");//.click();
		//TODO test something useful - clicking would open external browser, which is not good for tests
	}
}
