/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.wizard;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.internal.mpc.ui.wizards.AbstractTagFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.ComboTagFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.FeatureSelectionWizardPage;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizardDialog;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.tests.ui.wizard.matcher.NodeMatcher;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.ScreenshotCaptureListener;
import org.eclipse.swtbot.swt.finder.results.ArrayResult;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferenceConstants;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.waits.WaitForObjectCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotBrowser;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.ui.IEditorReference;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runners.model.Statement;

public abstract class AbstractMarketplaceWizardBotTest {

	private static final Logger logger = Logger.getLogger(AbstractMarketplaceWizardBotTest.class);

	private static boolean dumpThreadsOnTearDownError = true;

	protected SWTBot bot;

	protected SWTBotShell wizardShell;

	public AbstractMarketplaceWizardBotTest() {
		super();
	}

	@Rule
	public TestRule screenshotOnFailureRule = new TestRule() {

		public Statement apply(final Statement base, final Description description) {
			String targetDir = System.getProperty(SWTBotPreferenceConstants.KEY_SCREENSHOTS_DIR);
			if (targetDir == null && new File("target").isDirectory()) {
				SWTBotPreferences.SCREENSHOTS_DIR = "target/screenshots";
			}
			return new Statement() {

				private final ScreenshotCaptureListener capturer = new ScreenshotCaptureListener();

				@Override
				public void evaluate() throws Throwable {
					try {
						base.evaluate();
					} catch (Throwable t) {
						capturer.testFailure(new Failure(description, t));
						throw t;
					} finally {
						tearDownBot();
					}
				}

			};
		}
	};

	@Before
	public void setUp() {
		launchMarketplaceWizard();
		initWizardBot();
	}

	//tear-down is done in test rule above, since we need to do this after the rule has been applied
	//@After
	public void tearDownBot() {
		if (bot != null) {
			closeWizard();
		}
	}

	protected void launchMarketplaceWizard() {
		final MarketplaceWizardCommand marketplaceWizardCommand = new MarketplaceWizardCommand();
		UIThreadRunnable.asyncExec(new VoidResult() {

			public void run() {
				try {
					marketplaceWizardCommand.execute(new ExecutionEvent());
				} catch (ExecutionException e) {
					fail("ExecutionException: " + e.getMessage());
					//otherwise ignore, we'll notice in the test thread when we don't get the wizard dialog in time
				}
			}
		});
	}

	protected void initWizardBot() {
		bot = new SWTBot();
		bot.waitUntil(shellIsActive("Eclipse Marketplace"));
		wizardShell = bot.shell("Eclipse Marketplace");
		bot = wizardShell.bot();
		assertNotNull(getWizardDialog());
		waitForWizardProgress();
	}

	protected void waitForWizardProgress() {
		SWTBotButton cancelButton = bot.button("Cancel");
		bot.waitUntil(Conditions.widgetIsEnabled(cancelButton), 30000);
	}

	protected void closeWizard() {
		String problem = null;
		try {
			//check if dialog is still open
			SWTBotShell mpcShell = bot.shell("Eclipse Marketplace");
			try {
				//check if any message dialogs are open
				WaitForObjectCondition<Shell> subShellResult = Conditions.waitForShell(Matchers.any(Shell.class),
						mpcShell.widget);
				bot.waitUntil(subShellResult, 100, 60);
				List<Shell> subShells = subShellResult.getAllMatches();
				for (Shell shell : subShells) {
					if (shell == mpcShell.widget) {
						continue;
					}

					SWTBotShell botShell = new SWTBotShell(shell);
					//children are unexpected, so let's cry foul...
					if (problem == null) {
						problem = "MPC wizard has open child dialog:";
					}
					problem += "\n" + describeShell(botShell);
					logger.info(problem);

					captureShellScreenshot(botShell);

					//also dump threads, since this is often caused by the wizard not being cancellable due to a still running operation:
					//"Wizard can not be closed due to an active operation"
					dumpThreads();

					//kill message dialog
					botShell.close();
				}
			} catch (TimeoutException ex) {
			}
			//try killing it softly
			try {
				mpcShell.bot().button("Cancel").click();
				try {
					ICondition shellCloses = Conditions.shellCloses(mpcShell);
					bot.waitUntil(shellCloses);
					return;
				} catch (TimeoutException ex) {
				}
			} catch (TimeoutException ex) {
			}
			//now kill it hard - this is a last resort, because it can cause spurious errors in MPC jobs
			mpcShell.close();
		} catch (TimeoutException e) {
			//no MPC wizard found - maybe a bit strange, but so be it...
		} finally {
			if (problem != null) {
				//something happened
				fail(problem);
			}
		}
	}

	private void dumpThreads() {
		if (!dumpThreadsOnTearDownError) {
			return;
		}

		try {
			ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
			Method dumpMethod = ThreadMXBean.class.getMethod("dumpAllThreads", Boolean.TYPE, Boolean.TYPE);
			ThreadInfo[] threadInfos = (ThreadInfo[]) dumpMethod.invoke(threadMXBean, true, true);
			for (ThreadInfo threadInfo : threadInfos) {
				logger.debug(threadInfo);
			}
		} catch (NoSuchMethodException e) {
			dumpThreadsOnTearDownError = false;
			logger.warn("Method ThreadMXBean.dumpAllThreads(boolean, boolean) does not exist. Try running on Java 6 or later.");
		} catch (Throwable t) {
			logger.warn("Error dumping threads: " + t, t);
		}
	}

	private void captureShellScreenshot(SWTBotShell botShell) {
		if (botShell.isVisible()) {
			try {
				//try to bring to front
				botShell.activate();
			} catch (Throwable ex) {
			}
			//make a screenshot
			String fileName = "dialog_" + System.currentTimeMillis() + "."
					+ SWTBotPreferences.SCREENSHOT_FORMAT.toLowerCase();
			logger.info("Capturing screenshot of open shell in " + fileName);
			SWTUtils.captureScreenshot(SWTBotPreferences.SCREENSHOTS_DIR + "/" + fileName);
		}
	}

	private String describeShell(SWTBotShell botShell) {
		StringBuilder description = new StringBuilder("    Shell(\"").append(botShell.getText()).append("\")");

		try {
			SWTBot childBot = botShell.bot();
			@SuppressWarnings("unchecked")
			Matcher<Label> matcher = allOf(widgetOfType(Label.class));
			List<? extends Label> widgets = childBot.widgets(matcher);
			for (Label label : widgets) {
				if (label != null) {//TODO why can this be null?
					String labelText = new SWTBotLabel(label, matcher).getText();
					description.append("\n    > ").append(labelText);
				}
			}
		} catch (Exception ex) {
			description.append("\n    > Error describing shell contents: ").append(ex);
		}
		return description.toString();
	}

	protected SWTBot itemBot(NodeMatcher<? extends Widget> matcher) {
		List<? extends Widget> controls = bot.getFinder().findControls(matcher);
		assertThat(controls.size(), greaterThanOrEqualTo(1));
		Widget firstItem = controls.get(0);
		return new SWTBot(firstItem);
	}

	protected SWTBot itemBot(String id) {
		return itemBot(NodeMatcher.withId(id));
	}

	protected void checkSelectedTab(String tabLabel) {
		SWTBotTabItem searchTab = bot.tabItem(tabLabel);
		final TabItem tab = searchTab.widget;
		TabItem[] selection = UIThreadRunnable.syncExec(new ArrayResult<TabItem>() {

			public TabItem[] run() {
				return tab.getParent().getSelection();
			}
		});
		assertEquals(1, selection.length);
		assertSame(tab, selection[0]);
	}

	protected void filterMarket(String term) {
		SWTBotCombo comboBox = marketCombo();
		select(comboBox, IMarket.class, term);
	}

	protected SWTBotCombo marketCombo() {
		return bot.comboBox(0);
	}

	protected void filterCategory(String term) {
		SWTBotCombo comboBox = categoryCombo();
		select(comboBox, ICategory.class, term);
	}

	protected SWTBotCombo categoryCombo() {
		return bot.comboBox(1);
	}

	protected void search(String term) {
		SWTBotText searchField = searchField();
		searchField.setFocus();
		searchField.setText(term);
		bot.button("Go").click();
		waitForWizardProgress();
	}

	protected SWTBotText searchField() {
		return bot.text(0);
	}

	protected SWTBotBrowser marketplaceBrowser() {
		SWTWorkbenchBot wbBot = new SWTWorkbenchBot();
		Matcher<IEditorReference> marketplaceBrowserMatch = allOf(WidgetMatcherFactory.<IEditorReference> withPartId("org.eclipse.ui.browser.editor"), WidgetMatcherFactory
				.<IEditorReference> withTitle(containsString("Marketplace")));
		SWTBotEditor browserEditor = wbBot.editor(marketplaceBrowserMatch);
		SWTBotBrowser browser = browserEditor.bot().browser();
		return browser;
	}

	protected List<StyleRange> findLinks(final SWTBotStyledText styledText) {
		StyleRange[] ranges = findStyleRanges(styledText);
		List<StyleRange> links = new ArrayList<StyleRange>();
		for (StyleRange range : ranges) {
			if (range.underline == true && range.underlineStyle == SWT.UNDERLINE_LINK) {
				links.add(range);
			}
		}
		assertFalse(links.isEmpty());
		return links;
	}

	private StyleRange[] findStyleRanges(final SWTBotStyledText styledText) {
		StyleRange[] ranges = UIThreadRunnable.syncExec(new ArrayResult<StyleRange>() {

			public StyleRange[] run() {
				return styledText.widget.getStyleRanges();
			}
		});
		return ranges;
	}

	protected StyleRange findLink(final SWTBotStyledText styledText, String linkText) {
		List<StyleRange> links = findLinks(styledText);
		String text = styledText.getText();
		for (StyleRange link : links) {
			if (linkText.equals(getText(link, text))) {
				return link;
			}
		}
		fail("No link found with text '" + linkText + "'");
		return null;
	}

	protected StyleRange findLink(final SWTBotStyledText styledText) {
		List<StyleRange> links = findLinks(styledText);
		return links.get(0);
	}

	protected String getText(StyleRange range, String text) {
		return text.substring(range.start, range.start + range.length);
	}

	protected String getText(StyleRange range, SWTBotStyledText styledText) {
		String text = styledText.getText();
		return text.substring(range.start, range.start + range.length);
	}

	protected void select(SWTBotCombo comboBox, Class<?> classifier, String choice) {
		AbstractTagFilter filter = findFilter(classifier);
		String choiceText = choice != null ? choice : ((ComboTagFilter) filter).getNoSelectionLabel();

		comboBox.setSelection(choiceText);
		waitForWizardProgress();
		assertEquals(choiceText, comboBox.getText());
		checkSelected(filter, choice);
	}

	private void checkSelected(AbstractTagFilter filter, String selection) {
		Set<Tag> selected = filter.getSelected();
		if (selection == null) {
			assertTrue(selected.isEmpty());
			return;
		}
		for (Tag tag : selected) {
			if (tag.getLabel().equals(selection)) {
				return;
			}
			if (tag.getValue().equals(selection)) {
				return;
			}
		}
		fail(NLS.bind("Expected value {0} not selected in filter", selection));
	}

	private AbstractTagFilter findFilter(Class<?> classifier) {
		List<CatalogFilter> filters = getWizard().getConfiguration().getFilters();
		for (CatalogFilter filter : filters) {
			if (filter instanceof AbstractTagFilter) {
				AbstractTagFilter tagFilter = (AbstractTagFilter) filter;
				List<Tag> choices = tagFilter.getChoices();
				Object classification = choices.isEmpty() ? null : choices.get(0).getTagClassifier();
				if (classification == classifier) {
					return tagFilter;
				}
			}
		}
		fail("No filter found for " + classifier.getName());
		return null;//unreachable
	}

	protected void deselectPending(int count) {
		for (int i = 0; i < count; i++) {
			bot.button("Install Pending").click();
		}
	}

	protected void selectToInstall(int count) {
		if (count == 0) {
			return;
		}
		bot.button("Install").click();
		waitForWizardProgress();
		assertSame(getWizard().getPage(FeatureSelectionWizardPage.class.getName()), getWizardDialog().getCurrentPage());
		bot.button("< Install More").click();
		waitForWizardProgress();
		bot.link("<a>One solution selected</a>");
		for (int i = 2; i <= count; i++) {
			bot.button("Install").click();
			bot.link("<a>" + i + " solutions selected</a>");
		}
	}

	protected void tryWaitForBrowser(SWTBotBrowser browser) {
		for (int i = 0; i < 3; i++) {
			try {
				browser.waitForPageLoaded();
			} catch (TimeoutException ex) {
				//ignore
			}
			String url = browser.getUrl();
			if (url != null && !"".equals(url) && !"about:blank".equals(url)) {
				return;
			} else {
				bot.sleep(1000);
			}
		}
	}

	protected MarketplaceWizardDialog getWizardDialog() {
		return (MarketplaceWizardDialog) UIThreadRunnable.syncExec(new Result<Object>() {

			public Object run() {
				return wizardShell.widget.getData();
			}
		});
	}

	protected MarketplaceWizard getWizard() {
		return getWizardDialog().getWizard();
	}

}