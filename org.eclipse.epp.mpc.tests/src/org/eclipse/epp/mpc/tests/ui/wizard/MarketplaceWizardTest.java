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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.epp.internal.mpc.ui.wizards.AbstractMarketplaceDiscoveryItem;
import org.eclipse.epp.internal.mpc.ui.wizards.DiscoveryItem;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplacePage;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer;
import org.eclipse.epp.mpc.tests.Categories.RemoteTests;
import org.eclipse.epp.mpc.tests.Categories.UITests;
import org.eclipse.epp.mpc.tests.ui.wizard.matcher.NodeMatcher;
import org.eclipse.epp.mpc.tests.ui.wizard.widgets.SWTBotClickableStyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotBrowser;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLink;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category({ RemoteTests.class, UITests.class })
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
		search("Snipmatch");
		itemBot(NodeMatcher.withNameRegex(".*Snipmatch.*"));
	}

	@Test
	public void testSearchTag() {
		Matcher<StyledText> widgetOfType = widgetOfType(StyledText.class);
		Matcher<StyledText> withId = withId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_TAGS);
		Matcher<StyledText> emptyText = withText("");
		@SuppressWarnings("unchecked")
		Matcher<StyledText> nonEmptyTagMatcher = allOf(widgetOfType, withId, not(emptyText));
		SWTBotClickableStyledText tagsLabel = SWTBotClickableStyledText.from(new SWTBotStyledText(bot.widget(
				nonEmptyTagMatcher, 0), nonEmptyTagMatcher));
		StyleRange linkRange = findLink(tagsLabel);
		String tag = getText(linkRange, tagsLabel);
		tagsLabel.click(linkRange);
		waitForWizardProgress();
		String searchTerm = searchField().getText();
		assertEquals(MarketplaceViewer.QUERY_TAG_KEYWORD + tag, searchTerm);
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
		SWTBotLink link = selectToInstall(3);
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
				AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, AbstractMarketplaceDiscoveryItem.WIDGET_ID_DESCRIPTION));
		StyleRange linkRange = findLink(descriptionLabel, "more\u00a0info");
		bot.sleep(5000);
		descriptionLabel.click(linkRange);
		bot.sleep(5000);
		SWTBotShell tooltip = bot.shellWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_OVERVIEW);
		SWTBotLink moreLink = tooltip.bot().linkWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_LEARNMORE);
		moreLink.click();

		checkMarketplaceBrowser();
	}

	@Test
	public void testNoninstallableLearnMore() {
		SWTBot assumeLearnMoreBot = assume(bot);
		SWTBotClickableStyledText learnMoreLabel = SWTBotClickableStyledText.from(assumeLearnMoreBot.styledTextWithId(
				AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_LEARNMORE));
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

	@Test
	public void testFavorite() {
		SWTBotButton favorite = bot.buttonWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_RATING);
		bot.sleep(5000);
		favorite.click();
		SWTBotShell login = bot.shell("Eclipse User Storage Service");
		login.bot().button("Cancel").click();
		//TODO test something useful - we'd need a proper login on the server to do this...
		//better to get started with some server mocking in the ui tests...
	}

	@Test
	public void testShare() {
		SWTBotButton share = bot.buttonWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_SHARE);
		share.click();
		share.contextMenu("Twitter");//.click();
		//TODO test something useful - clicking would open external browser, which is not good for tests
	}
}
