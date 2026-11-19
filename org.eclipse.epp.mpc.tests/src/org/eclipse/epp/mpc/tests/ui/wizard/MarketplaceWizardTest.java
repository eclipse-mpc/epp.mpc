/*******************************************************************************
 * Copyright (c) 2010, 2026 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.wizard;

import static org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory.withPartId;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withId;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.epp.internal.mpc.ui.wizards.AbstractMarketplaceDiscoveryItem;
import org.eclipse.epp.internal.mpc.ui.wizards.DiscoveryItem;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplacePage;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.tests.ui.wizard.matcher.NodeMatcher;
import org.eclipse.epp.mpc.tests.ui.wizard.widgets.SWTBotClickableStyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotBrowser;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLink;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;


@Tag("RemoteTests")
@Tag("UITests")
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
		search("WindowBuilder");
		itemBot(NodeMatcher.withNameRegex(".*WindowBuilder.*"));
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
		String listingId = listingIdOf(tagsLabel.widget);
		assertNotNull(listingId);
		StyleRange linkRange = findLink(tagsLabel);
		String tag = getText(linkRange, tagsLabel);
		tagsLabel.click(linkRange);
		waitForWizardProgress();
		String searchTerm = searchField().getText();
		assertEquals(MarketplaceViewer.QUERY_TAG_KEYWORD + tag, searchTerm);
		//the listing the tag was clicked on has to be among the results
		itemBot(NodeMatcher.withId(listingId));
	}

	/**
	 * Returns the id of the marketplace listing the given widget belongs to.
	 */
	private static String listingIdOf(Widget widget) {
		return UIThreadRunnable.syncExec((Result<String>) () -> {
			for (Control control = (Control) widget; control != null; control = control.getParent()) {
				Object data = control.getData();
				if (data instanceof CatalogItem catalogItem) {
					data = catalogItem.getData();
				}
				if (data instanceof INode node) {
					return node.getId();
				}
			}
			return null;
		});
	}

	@Test
	public void testSearchMarketplaceUrl() {
		search("https://marketplace.eclipse.org/content/mylyn");
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

	/**
	 * A long-standing listing without an update site, which the wizard shows with a "learn more" link instead of an
	 * install button.
	 */
	private static final INode NONINSTALLABLE_NODE = testNode("131", "https://marketplace.eclipse.org/content/jinto",
			"JInto");

	//TODO conditional on embedded browser availability
	@Test
	public void testMoreInfoLearnMore() {
		runWithInternalBrowser(() -> {
			SWTBotClickableStyledText descriptionLabel = SWTBotClickableStyledText.from(bot.styledTextWithId(
					AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, AbstractMarketplaceDiscoveryItem.WIDGET_ID_DESCRIPTION));
			StyleRange linkRange = findLink(descriptionLabel, "more\u00a0info");
			//the tooltip closes when the mouse leaves the description, so click without the final mouse exit
			descriptionLabel.clickWithoutMouseExit(linkRange);
			SWTBotShell tooltip = bot.shellWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_OVERVIEW);
			SWTBotLink moreLink = tooltip.bot().linkWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_LEARNMORE);
			moreLink.click();

			checkMarketplaceBrowser();
		});
	}

	@Test
	public void testNoninstallableLearnMore() {
		//the default test content only contains installable solutions
		closeWizard();
		launchMarketplaceWizard(NONINSTALLABLE_NODE);
		initWizardBot();

		runWithInternalBrowser(() -> {
			SWTBotClickableStyledText learnMoreLabel = SWTBotClickableStyledText.from(bot.styledTextWithId(
					AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_LEARNMORE));
			StyleRange linkRange = findLink(learnMoreLabel);
			learnMoreLabel.click(linkRange);

			checkMarketplaceBrowser();
		});
	}

	/**
	 * Runs the given test code with the workbench configured to use the internal web browser, which
	 * {@link org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard#openUrl(String)} requires to open marketplace
	 * pages in a browser editor. Afterwards, closes the browser editors, as the marketplace browser editor is reused.
	 */
	private static void runWithInternalBrowser(Runnable test) {
		IEclipsePreferences browserPreferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui.browser");
		int browserChoice = browserPreferences.getInt("browser-choice", -1);
		browserPreferences.putInt("browser-choice", 0);
		try {
			test.run();
		} finally {
			for (SWTBotEditor editor : new SWTWorkbenchBot().editors(withPartId("org.eclipse.ui.browser.editor"))) {
				editor.close();
			}
			if (browserChoice == -1) {
				browserPreferences.remove("browser-choice");
			} else {
				browserPreferences.putInt("browser-choice", browserChoice);
			}
		}
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
		bot.cTabItemWithId(MarketplacePage.WIDGET_ID_KEY, MarketplacePage.WIDGET_ID_TAB_NEWS).activate();
		bot.sleep(500);
		tryWaitForBrowser(bot.browser());
		String url = bot.browser().getUrl();
		assertTrue(url.contains("newsletter"));
	}

	@Test
	public void testRecentBackToSearch() {
		bot.cTabItem("Recent").activate();
		waitForWizardProgress();
		bot.cTabItem("Search").activate();
		waitForWizardProgress();
		searchField();
		assertTrue(bot.label("Featured").isVisible());
	}

	@Test
	public void testRecent() {
		bot.cTabItem("Recent").activate();
		waitForWizardProgress();
		//TODO test something useful
	}

	@Test
	public void testPopular() {
		bot.cTabItem("Popular").activate();
		waitForWizardProgress();
		//TODO test something useful
	}

	@Test
	public void testShare() {
		SWTBotButton share = bot.buttonWithId(AbstractMarketplaceDiscoveryItem.WIDGET_ID_KEY, DiscoveryItem.WIDGET_ID_SHARE);
		share.click();
		share.contextMenu("Twitter");//.click();
		//TODO test something useful - clicking would open external browser, which is not good for tests
	}
}
