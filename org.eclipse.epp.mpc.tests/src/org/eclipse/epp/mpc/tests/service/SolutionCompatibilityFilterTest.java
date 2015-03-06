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
package org.eclipse.epp.mpc.tests.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assume.*;

import java.net.URL;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.mpc.core.model.IIu;
import org.eclipse.epp.mpc.core.model.IIus;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.model.ISearchResult;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.UIServices;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.ComparisonFailure;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.Statement;

@RunWith(Parameterized.class)
public class SolutionCompatibilityFilterTest {
	private static final String BASE_URL = "http://marketplace-staging.eclipse.org";

	public static enum System {
		WIN32(Platform.OS_WIN32, Platform.WS_WIN32), //
		LINUX(Platform.OS_LINUX, Platform.WS_GTK), //
		MACOS(Platform.OS_MACOSX, Platform.WS_COCOA);

		private final String os;

		private final String ws;

		private System(String os, String ws) {
			this.os = os;
			this.ws = ws;
		}

		public void applyTo(Map<String, String> metaParams) {
			metaParams.put(DefaultMarketplaceService.META_PARAM_OS, os);
			metaParams.put(DefaultMarketplaceService.META_PARAM_WS, ws);
		}
	}

	public static enum EclipseRelease {
		UNKNOWN(null, null, null), //
		INDIGO("3.7.0.v201106131736", "3.7.0.v20110110", null), //
		JUNO_3_8("3.8.0.v201206081200", "3.8.0.v20120521-2346", null), //
		JUNO_3_8_WITH_PLATFORM(JUNO_3_8.productVersion(), JUNO_3_8.runtimeVersion(), "3.8.0.v201206081200"), //
		JUNO("4.2.0.v201206081400", "3.8.0.v20120521-2346", null), //
		JUNO_WITH_PLATFORM(JUNO.productVersion(), JUNO.runtimeVersion(), "4.2.0.v201206081400"), //
		JUNO_SR2("4.2.2.v201302041200", "3.8.0.v20120521-2346", "4.2.2.v201302041200"), //
		KEPLER("4.3.0.v20130605-2000", "3.9.0.v20130326-1255", "4.3.0.v20130605-2000"), //
		KEPLER_SR2("4.3.2.v20140221-1700", "3.9.100.v20131218-1515", "4.3.2.v20140221-1700"), //
		LUNA("4.4.0.v20140606-1215", "3.10.0.v20140318-2214", "4.4.0.v20140606-1215"), //
		LUNA_SR2("4.4.2.v20150204-1700", "3.10.0.v20140318-2214", "4.4.2.v20150204-1700"), //
		MARS("4.5.0.v20150203-1300", "3.10.0.v20150112-1422", "4.5.0.v20150203-1300");

		private static final String PRODUCT_ID = "org.eclipse.sdk.ide";

		private final String productVersion;

		private final String runtimeVersion;

		private final String platformVersion;

		private EclipseRelease(String productVersion, String runtimeVersion, String platformVersion) {
			this.productVersion = productVersion;
			this.runtimeVersion = runtimeVersion;
			this.platformVersion = platformVersion;
		}

		public String productVersion() {
			return productVersion;
		}

		public String runtimeVersion() {
			return runtimeVersion;
		}

		public String platformVersion() {
			return platformVersion;
		}

		public void applyTo(Map<String, String> metaParams) {
			if (productVersion == null) {
				metaParams.remove(DefaultMarketplaceService.META_PARAM_PRODUCT);
				metaParams.remove(DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION);
			} else {
				metaParams.put(DefaultMarketplaceService.META_PARAM_PRODUCT, PRODUCT_ID);
				metaParams.put(DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION, productVersion);
			}
			if (runtimeVersion == null) {
				metaParams.remove(DefaultMarketplaceService.META_PARAM_RUNTIME_VERSION);
			} else {
				metaParams.put(DefaultMarketplaceService.META_PARAM_RUNTIME_VERSION, runtimeVersion);
			}
			if (platformVersion == null) {
				metaParams.remove(DefaultMarketplaceService.META_PARAM_PLATFORM_VERSION);
			} else {
				metaParams.put(DefaultMarketplaceService.META_PARAM_PLATFORM_VERSION, platformVersion);
			}
		}

		public static EclipseRelease previous(EclipseRelease release) {
			if (release == UNKNOWN) {
				return null;
			}
			int previousOrdinal = release.ordinal() - 1;
			return previousOrdinal > UNKNOWN.ordinal() ? values()[previousOrdinal] : null;
		}

		public static EclipseRelease next(EclipseRelease release) {
			if (release == UNKNOWN) {
				return null;
			}
			int nextOrdinal = release.ordinal() + 1;
			return values().length > nextOrdinal ? values()[nextOrdinal] : null;
		}
	}

	public static enum Solution {
		JUNO("test-entry-juno", EclipseRelease.JUNO_3_8, EclipseRelease.JUNO_SR2), //
		KEPLER("test-entry-kepler", EclipseRelease.KEPLER, EclipseRelease.KEPLER_SR2), //
		LUNA("test-entry-luna", EclipseRelease.LUNA, EclipseRelease.LUNA_SR2), //
		MARS("test-entry-mars", EclipseRelease.MARS, EclipseRelease.MARS), //
		JUNO_AND_EARLIER("test-entry-juno-and-earlier", null, EclipseRelease.JUNO_SR2), //
		KEPLER_AND_EARLIER("test-entry-kepler-and-earlier", null, EclipseRelease.KEPLER_SR2), //
		KEPLER_LUNA("test-entry-kepler-luna", EclipseRelease.KEPLER, EclipseRelease.LUNA_SR2), //
		KEPLER_MARS("test-entry-kepler-luna-mars", EclipseRelease.KEPLER, EclipseRelease.MARS), //
		LUNA_WIN32("test-entry-luna-win32", EclipseRelease.LUNA, EclipseRelease.LUNA_SR2, System.WIN32), //
		LUNA_LINUX_MACOS("test-entry-luna-mac-linux", EclipseRelease.LUNA, EclipseRelease.LUNA_SR2, System.LINUX,
				System.MACOS), //
				MULTI_VERSION("test-entry-multi-version", null, EclipseRelease.MARS), //
				PSEUDO_CONFLICT("test-entry-pseudo-conflict", EclipseRelease.KEPLER, EclipseRelease.MARS), //
				CONFLICT("test-entry-conflict", EclipseRelease.KEPLER, EclipseRelease.MARS), //
				UNINSTALLABLE(""/* TODO */, null, null, System.WIN32, System.MACOS, System.LINUX);

		private final String id;

		private final String shortName;

		private final EclipseRelease minRelease;

		private final EclipseRelease maxRelease;

		private final System[] systems;

		private Solution(String shortName, EclipseRelease minRelease, EclipseRelease maxRelease, System... systems) {
			this.id = null;
			this.shortName = shortName;
			this.minRelease = minRelease;
			this.maxRelease = maxRelease;
			this.systems = systems;
		}

		public String id() {
			return id;
		}

		public String shortName() {
			return shortName;
		}

		public String url() {
			return BASE_URL + "/content/" + shortName;
		}

		public String query() {
			return shortName;
		}

		public boolean installable() {
			//TODO
			return true;
		}

		public EclipseRelease minRelease() {
			return minRelease;
		}

		public EclipseRelease maxRelease() {
			return maxRelease;
		}

		public System[] systems() {
			return systems == null || systems.length == 0 ? System.values() : systems;
		}

		public boolean isCompatible(System system) {
			if (systems == null || system == null) {
				return true;
			}
			for (System aSystem : systems()) {
				if (aSystem == system) {
					return true;
				}
			}
			return false;
		}

		public boolean isCompatible(EclipseRelease release) {
			if (release == EclipseRelease.UNKNOWN) {
				return true;
			}
			if (minRelease() != null) {
				if (minRelease().ordinal() > release.ordinal()) {
					return false;
				}
			}
			if (maxRelease() != null) {
				if (maxRelease().ordinal() < release.ordinal()) {
					return false;
				}
			}
			return true;
		}
	}

	@Parameters(name = "{index}__{0}__with__{1}_{2}")
	public static Iterable<Object[]> data() {
		List<Object[]> data = new ArrayList<Object[]>();
		checkSolutionReleaseBounds(data, Solution.JUNO);
		checkSolutionReleaseBounds(data, Solution.KEPLER);
		checkSolutionReleaseBounds(data, Solution.LUNA);
		checkSolutionReleaseBounds(data, Solution.MARS);
		checkSolutionReleaseBounds(data, Solution.JUNO_AND_EARLIER);
		checkSolutionReleaseBounds(data, Solution.KEPLER_AND_EARLIER);
		checkSolutionReleaseBounds(data, Solution.KEPLER_LUNA);
		checkSolutionData(data, "Solution should have version 1.0.0 features for Kepler release", Solution.KEPLER_LUNA,
				EclipseRelease.KEPLER, System.WIN32, "1.0.0",
				"http://example.org/kepler", "org.example.feature.kepler");
		checkSolutionData(data, "Solution should have version 1.1.0 features for Luna release", Solution.KEPLER_LUNA,
				EclipseRelease.LUNA, System.WIN32, "1.1.0",
				"http://example.org/luna", "org.example.feature.luna");
		checkSolutionReleaseBounds(data, Solution.KEPLER_MARS);
		checkSolutionData(data, "Solution should have version 1.0.0 features for Kepler release", Solution.KEPLER_MARS,
				EclipseRelease.KEPLER, System.WIN32, "1.0.0",
				"http://example.org/kepler-luna", "org.example.feature.kepler.luna");
		checkSolutionData(data, "Solution should have version 1.0.0 features for Luna release", Solution.KEPLER_MARS,
				EclipseRelease.LUNA, System.WIN32, "1.0.0",
				"http://example.org/kepler-luna", "org.example.feature.kepler.luna");
		checkSolutionData(data, "Solution should have version 1.1.0 features for Mars release", Solution.KEPLER_MARS,
				EclipseRelease.MARS, System.WIN32, "1.1.0",
				"http://example.org/mars", "org.example.feature.mars");
		checkSolutionWithEclipse(data, "Solution should be installable in a compatible release and os",
				Solution.LUNA_WIN32, EclipseRelease.LUNA, System.WIN32);
		checkSolutionWithEclipse(data, "Solution should not be installable in an incompatible os", Solution.LUNA_WIN32,
				EclipseRelease.LUNA, System.LINUX);
		checkSolutionWithEclipse(data, "Solution should not be installable in an older release", Solution.LUNA_WIN32,
				EclipseRelease.KEPLER, System.WIN32);
		checkSolutionWithEclipse(data, "Solution should not be installable in an older release and incompatible os",
				Solution.LUNA_WIN32, EclipseRelease.KEPLER, System.LINUX);
		checkSolutionWithEclipse(data, "Solution should not be installable in a newer release", Solution.LUNA_WIN32,
				EclipseRelease.MARS, System.WIN32);
		checkSolutionWithEclipse(data, "Solution should not be installable in a newer release and incompatible os",
				Solution.LUNA_WIN32, EclipseRelease.MARS, System.LINUX);
		checkSolutionWithEclipse(data, "Solution should be installable in a compatible release and os",
				Solution.LUNA_LINUX_MACOS, EclipseRelease.LUNA, System.LINUX);
		checkSolutionWithEclipse(data, "Solution should be installable in a compatible release and os",
				Solution.LUNA_LINUX_MACOS, EclipseRelease.LUNA, System.MACOS);
		checkSolutionWithEclipse(data, "Solution should not installable in an incompatible os",
				Solution.LUNA_LINUX_MACOS, EclipseRelease.LUNA, System.WIN32);
		checkSolutionData(data, "Solution should have version 1.1.0 features for Juno release", Solution.MULTI_VERSION,
				EclipseRelease.JUNO, System.LINUX, "1.1.0",
				"http://example.org/juno-kepler", "org.example.feature.juno.kepler");
		checkSolutionData(data, "Solution should have version 1.1.0 features for Kepler release",
				Solution.MULTI_VERSION, EclipseRelease.KEPLER, System.MACOS, "1.1.0",
				"http://example.org/juno-kepler", "org.example.feature.juno.kepler");
		checkSolutionData(data, "Solution should have version 1.1.1 features for Kepler release on Windows",
				Solution.MULTI_VERSION, EclipseRelease.KEPLER, System.WIN32, "1.1.1",
				"http://example.org/juno-kepler-win32", "org.example.feature.juno.kepler.win32");
		checkSolutionData(data, "Solution should have version 1.2.0 features for Luna release", Solution.MULTI_VERSION,
				EclipseRelease.LUNA, System.WIN32, "1.2.0",
				"http://example.org/luna", "org.example.feature.luna.nolinux");
		checkSolutionWithEclipse(data, "Solution should be incompatible with Linux for Luna release",
				Solution.MULTI_VERSION, EclipseRelease.LUNA, System.LINUX, false);
		checkSolutionData(data, "Solution should have version 1.3.0 features for Mars release", Solution.MULTI_VERSION,
				EclipseRelease.MARS, System.MACOS, "1.3.0",
				"http://example.org/mars", "org.example.feature.mars.nolinux");
		checkSolutionWithEclipse(data, "Solution should be incompatible with Linux for Mars release",
				Solution.MULTI_VERSION, EclipseRelease.MARS, System.LINUX, false);
		checkSolutionData(
				data,
				"Solution with overlapping versions but separate os (pseudo-conflict) should have version 1.0.0 for Luna on Mac",
				Solution.PSEUDO_CONFLICT, EclipseRelease.LUNA, System.MACOS, "1.0.0",
				"http://example.org/maclinux", "org.example.feature.maclinux");
		checkSolutionData(
				data,
				"Solution with overlapping versions but separate os (pseudo-conflict) should have version 1.1.0 for Luna on Windows",
				Solution.PSEUDO_CONFLICT, EclipseRelease.LUNA, System.WIN32, "1.1.0",
				"http://example.org/win", "org.example.feature.win");
		checkSolutionData(
				data,
				"Solution with overlapping versions (real conflict) should have correct version for non-overlapping release (older version)",
				Solution.CONFLICT, EclipseRelease.KEPLER, System.WIN32, "1.0.0",
				"http://example.org/kepler-luna", "org.example.feature.keplerluna");
		checkSolutionData(
				data,
				"Solution with overlapping versions (real conflict) should have latest version for overlapping release",
				Solution.CONFLICT, EclipseRelease.LUNA, System.WIN32, "1.1.0",
				"http://example.org/luna-mars", "org.example.feature.lunamars");
		checkSolutionData(
				data,
				"Solution with overlapping versions (real conflict) should have latest version for overlapping release",
				Solution.CONFLICT, EclipseRelease.LUNA, System.MACOS, "1.1.0",
				"http://example.org/luna-mars", "org.example.feature.lunamars");
		checkSolutionData(
				data,
				"Solution with overlapping versions (real conflict) should have correct version for non-overlapping release (newer version)",
				Solution.CONFLICT, EclipseRelease.MARS, System.WIN32, "1.1.0",
				"http://example.org/luna-mars", "org.example.feature.lunamars");
		//		checkSolutionWithEclipse(data, Solution.UNINSTALLABLE, EclipseRelease.UNKNOWN, System.WIN32);
		//		checkSolutionWithEclipse(data, Solution.UNINSTALLABLE, EclipseRelease.UNKNOWN, System.LINUX);
		//		checkSolutionWithEclipse(data, Solution.UNINSTALLABLE, EclipseRelease.UNKNOWN, System.MACOS);
		return data;
	}

	private static void checkSolutionData(List<Object[]> data, String testDescription, Solution solution,
			EclipseRelease release,
			System system, String version, String site, String... features) {
		if (release == null) {
			release = EclipseRelease.UNKNOWN;
		}
		if (system == null) {
			system = System.WIN32;
		}

		boolean releaseCompatible = solution.isCompatible(release);
		boolean systemCompatible = solution.isCompatible(system);
		boolean compatible = releaseCompatible && systemCompatible;

		checkSolutionData(data, testDescription, solution, release, system, compatible, version, site, features);
	}

	private static void checkSolutionData(List<Object[]> data, String testDescription, Solution solution,
			EclipseRelease release,
			System system, boolean compatible, String version, String site, String... features) {
		if (release == null) {
			release = EclipseRelease.UNKNOWN;
		}
		if (system == null) {
			system = System.WIN32;
		}
		if (features != null && features.length == 0) {
			features = null;
		}

		for (Object[] objects : data) {
			if (objects[0] == solution && objects[1] == release && objects[2] == system) {
				if (((version == null && objects[3] == null) || (version != null && version.equals(objects[3])))
						&& ((site == null && objects[4] == null) || (site != null && site.equals(objects[4])))) {
					if (features == null && objects[5] == null) {
						return;
					}
					Set<String> allFeatures = new HashSet<String>(Arrays.asList(features));
					Set<String> allDataFeatures = new HashSet<String>(Arrays.asList((String[]) objects[5]));
					if (allFeatures.equals(allDataFeatures)) {
						return;
					}
				}
			}
		}
		data.add(new Object[] { solution, release, system, version, site, features, compatible, testDescription });
	}

	private static void checkSolutionReleaseBounds(List<Object[]> data, Solution solution) {
		EclipseRelease minRelease = solution.minRelease();
		EclipseRelease maxRelease = solution.maxRelease();
		EclipseRelease beforeMin = minRelease == null ? null : EclipseRelease.previous(minRelease);
		EclipseRelease afterMax = maxRelease == null ? null : EclipseRelease.next(maxRelease);
		if (beforeMin != null) {
			checkSolutionWithEclipse(data, "Solution should not be installable in an older release", solution,
					beforeMin, null);
		}
		for (EclipseRelease release : EclipseRelease.values()) {
			if (solution.isCompatible(release)) {
				checkSolutionWithEclipse(data, "Solution should be installable in a compatible release", solution,
						release, null);
			}
		}
		checkSolutionWithEclipse(data, "Solution should be installable in an unknown release", solution,
				EclipseRelease.UNKNOWN, null);
		if (afterMax != null) {
			checkSolutionWithEclipse(data, "Solution should not be installable in a newer release", solution, afterMax,
					null);
		}
	}

	private static void checkSolutionWithEclipse(List<Object[]> data, String testDescription, Solution solution,
			EclipseRelease release,
			System system) {
		checkSolutionData(data, testDescription, solution, release, system, null, null);
	}

	private static void checkSolutionWithEclipse(List<Object[]> data, String testDescription, Solution solution,
			EclipseRelease release,
			System system, boolean compatible) {
		checkSolutionData(data, testDescription, solution, release, system, compatible, null, null);
	}

	private static void assertTrue(String message, boolean b, Object... details) {
		Assert.assertTrue(format(message, flatten(b, details)), b);
	}

	private static void assertNotNull(Object value) {
		Assert.assertNotNull(value);
	}

	private static void assertEquals(String message, Object o1, Object o2, Object... details) {
		Assert.assertEquals(format(message, flatten(o1, o2, details)), o1, o2);
	}

	private static void assertNotNull(String message, Object value, Object... details) {
		Assert.assertNotNull(format(message, flatten(value, details)), value);
	}

	private static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher, Object... details) {
		MatcherAssert.assertThat(format(reason, flatten(actual, details)), actual, matcher);
	}

	private static void assertNull(String message, Object value, Object... details) {
		Assert.assertNull(format(message, flatten(value, details)), value);
	}

	private static String format(String message, Object... details) {
		if (details == null || details.length == 0) {
			return message;
		}

		String contentPrefix = BASE_URL + "/content/";
		for (int i = 0; i < details.length; i++) {
			Object detail = details[i];
			if (detail instanceof INode) {
				INode node = (INode) detail;
				String url = node.getUrl();
				if (url != null) {
					if (url.startsWith(contentPrefix)) {
						String shortName = url.substring(contentPrefix.length());
						detail = shortName;
					} else {
						detail = url;
					}
				} else {
					detail = node.getId();
				}
			} else if (detail instanceof Solution) {
				Solution solution = (Solution) detail;
				detail = solution.shortName();
			}
			details[i] = detail;
		}
		return MessageFormat.format(message, details);
	}

	private static Object[] flatten(Object... values) {
		List<Object> flattened = new ArrayList<Object>();
		for (Object object : values) {
			flatten(flattened, object);
		}
		return flattened.toArray(new Object[flattened.size()]);
	}

	private static void flatten(List<Object> flattened, Object value) {
		if (value instanceof Object[]) {
			Object[] child = (Object[]) value;
			for (Object object : child) {
				flatten(flattened, object);
			}
		} else {
			flattened.add(value);
		}
	}

	@ClassRule
	public static TestRule stageCredentialsRule = new ExternalResource() {
		private UIServices originalService;

		private UIServices credentialsService;

		@Override
		protected void before() throws Throwable {
			IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(Activator.getContext(),
					IProvisioningAgent.SERVICE_NAME);
			UIServices adminUIService = (UIServices) agent.getService(UIServices.SERVICE_NAME);
			originalService = adminUIService;
			credentialsService = new UIServices() {
				@Override
				public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
					if (previousInfo == null) {
						return getUsernamePassword(location);
					}
					return null;
				}

				@Override
				public AuthenticationInfo getUsernamePassword(String location) {
					if (location != null && location.contains("marketplace-staging")) {
						return new AuthenticationInfo("testuser", "plaintext", false);
					}
					throw new AssertionError("Unexpectedly required authentication for host " + location);
				}

				@Override
				public TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail) {
					throw new AssertionError("Unexpectedly required trustinfo");
				}
			};
			agent.registerService(UIServices.SERVICE_NAME, credentialsService);
		}

		@Override
		protected void after() {
			UIServices originalService = this.originalService;
			UIServices credentialsService = this.credentialsService;
			this.originalService = null;
			this.credentialsService = null;
			IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(Activator.getContext(),
					IProvisioningAgent.SERVICE_NAME);
			Object currentUIService = agent.getService(UIServices.SERVICE_NAME);
			if (currentUIService == credentialsService && originalService != null) {
				agent.registerService(UIServices.SERVICE_NAME, originalService);
			}
		}
	};

	@Rule
	public TestRule logRule = new TestRule() {

		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					try {
						MarketplaceClientCore.error("Starting test " + description.getDisplayName(), null);
						base.evaluate();
					} finally {
						MarketplaceClientCore.error("Finished test " + description.getDisplayName(), null);
					}
				}
			};
		}
	};

	@Rule
	public TestRule requestInfoRule = new TestRule() {
		public Statement apply(final Statement base, final Description description) {
			String methodName = description.getMethodName();
			if (methodName.endsWith("Search")) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						try {
							base.evaluate();
						} catch (AssertionError t) {
							failedSearchQuery(t, description);
						}
					}
				};
			} else {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						try {
							base.evaluate();
						} catch (AssertionError t) {
							failedNodeQuery(t, description);
						}
					}
				};
			}
		}

		protected void failedSearchQuery(AssertionError error, Description description) {
		}

		protected void failedNodeQuery(AssertionError error, Description description) {
			String queryDetail = "Unexpected result in query for node " + solution.shortName() + "\n   "
					+ marketplaceService.addMetaParameters(solution.url() + "/api/p");
			failedWithDetails(error, queryDetail);
		}

		protected void failedWithDetails(AssertionError error, String queryDetail) throws AssertionError {
			String message = error.getMessage() == null ? queryDetail : queryDetail + "\n\n" + error.getMessage();

			String testDescription = SolutionCompatibilityFilterTest.this.testDescription;
			if (testDescription != null) {
				message = testDescription + "\n\n" + message;
			}
			throw adaptAssertionError(error, message);
		}

		protected AssertionError adaptAssertionError(AssertionError error, String message) {
			if (message == null || message.equals(error.getMessage())) {
				return error;
			}

			AssertionError newError;
			if (error.getClass() == AssertionError.class) {
				newError = new AssertionError(message);
			} else if (error.getClass() == ComparisonFailure.class) {
				ComparisonFailure comparisonFailure = (ComparisonFailure) error;
				newError = new ComparisonFailure(message, comparisonFailure.getExpected(), comparisonFailure
						.getActual());
			} else {
				newError = new AssertionError(message);
				newError.initCause(error);
			}
			newError.setStackTrace(error.getStackTrace());
			return newError;
		}
	};

	@Parameter(0)
	public Solution solution;

	@Parameter(1)
	public EclipseRelease eclipseRelease;

	@Parameter(2)
	public System system;

	@Parameter(3)
	public String version;

	@Parameter(4)
	public String site;

	@Parameter(5)
	public String[] features;

	@Parameter(6)
	public boolean compatible;

	@Parameter(7)
	public String testDescription;

	private DefaultMarketplaceService marketplaceService;

	@Before
	public void setupMarketplaceService() throws Exception {
		marketplaceService = new DefaultMarketplaceService(new URL(BASE_URL));
		marketplaceService.setRequestMetaParameters(computeRequestMetaParameters(system, eclipseRelease));
	}

	protected Map<String, String> computeRequestMetaParameters(System system, EclipseRelease eclipseRelease) {
		Map<String, String> requestMetaParameters = ServiceLocator.computeDefaultRequestMetaParameters();
		system.applyTo(requestMetaParameters);
		eclipseRelease.applyTo(requestMetaParameters);
		return requestMetaParameters;
	}

	protected INode queryNode() throws CoreException {
		INode node = marketplaceService.getNode(QueryHelper.nodeByUrl(solution.url()), new NullProgressMonitor());
		assertNotNull("Node {0} not found", node);
		if (solution.id() != null) {
			assertEquals("Node {2} returned with wrong id {1}", solution.id(), node.getId(), solution);
		}
		if (solution.url() != null) {
			assertEquals("Node {2} returned with wrong url {1}", solution.url(), node.getUrl(), solution);
		}
		return node;
	}

	protected INode searchForNode() throws CoreException {
		ISearchResult searchResult = marketplaceService.search(null, null, solution.query(), new NullProgressMonitor());
		assertSearchResultSanity(searchResult);
		List<? extends INode> nodes = searchResult.getNodes();
		INode foundNode = null;
		for (INode node : nodes) {
			if ((solution.id() != null && solution.id().equals(node.getId()))
					|| (solution.url() != null && solution.url().equals(node.getUrl()))) {
				foundNode = node;
				break;
			}
		}
		return foundNode;
	}

	protected void assertSearchResultSanity(ISearchResult result) {
		assertNotNull(result);
		assertNotNull(result.getNodes());
		assertNotNull(result.getMatchCount());
		assertTrue("Total search result count {1} has to be at least the number of returned nodes {2}", result
				.getMatchCount() >= result.getNodes().size(), result.getMatchCount(), result.getNodes().size());

		Set<String> ids = new HashSet<String>();
		for (INode node : result.getNodes()) {
			assertNotNull("Search result node {1} without id", node.getId(), node);
			assertTrue("Duplicate search result node {1}", ids.add(node.getId()), node);
		}
	}

	@Test
	public void testCompatibleInstallableNode() throws CoreException {
		assumeTrue("Skipping test - this solution and Eclipse/OS are incompatible", compatible);
		assumeTrue("Skipping test - this solution is not installable", solution.installable());
		INode node = queryNode();
		String updateurl = node.getUpdateurl();
		assertThat("Node {1} has no update url", updateurl, not(isEmptyOrNullString()), node);
		IIus ius = node.getIus();
		assertNotNull("Node {1} is missing <ius> element", ius, node);
		List<IIu> iuElements = ius.getIuElements();
		assertNotNull(iuElements);
		assertThat("Node {1} has no IUs", iuElements, not(empty()), node);

		if (version != null) {
			assertEquals("Node {2} has wrong version", version, node.getVersion(), node);
		}
		if (site != null) {
			assertEquals("Node {2} has wrong update site", site, node.getUpdateurl(), node);
		}
		if (features != null) {
			Set<String> allIUs = new HashSet<String>();
			for (IIu iu : iuElements) {
				allIUs.add(iu.getId());
			}
			assertThat("Node {1} is missing some features", allIUs, hasItems(features), node);
			assertThat("Node {1} has some unexpected features", allIUs, hasSize(features.length), node);
		}
	}

	@Test
	public void testCompatibleNonInstallableNode() throws CoreException {
		assumeTrue("Skipping test - this solution and Eclipse/OS are incompatible", compatible);
		assumeFalse("Skipping test - this solution is installable", solution.installable());
		INode node = queryNode();
		String updateurl = node.getUpdateurl();
		assertNull("Uninstallable node {1} should not have an update url, but has {0}", updateurl, node);
		IIus ius = node.getIus();
		assertNull("Uninstallable node {1} should not have an <ius> element", ius, node);
	}

	@Test
	public void testIncompatibleNode() throws CoreException {
		assumeFalse("Skipping test - this solution and Eclipse/OS are compatible", compatible);
		INode node = queryNode();
		String updateurl = node.getUpdateurl();
		assertNull("Incompatible node {1} should not have an update url, but has {0}", updateurl, node);
		IIus ius = node.getIus();
		assertNull("Incompatible node {1} should not have an <ius> element", ius, node);
	}

	@Test
	@Ignore
	public void testCompatibleSearchResult() throws CoreException {
		assumeTrue("Skipping test - this solution and Eclipse/OS are incompatible", compatible);
		INode foundNode = searchForNode();
		assertNotNull("Compatible node {1} not found in search", foundNode, solution);
	}

	@Test
	@Ignore
	public void testIncompatibleSearchResult() throws CoreException {
		assumeFalse("Skipping test - this solution and Eclipse/OS are compatible", compatible);
		INode foundNode = searchForNode();
		assertNull("Incompatible node {0} found in search", foundNode);
	}
}
