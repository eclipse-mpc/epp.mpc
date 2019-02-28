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
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceInfo;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.tests.ui.catalog.MarketplaceInfoSerializationTest.TestMarketplaceInfo.TestRegistryFile;
import org.eclipse.epp.mpc.tests.util.TemporaryEnvironment;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test {@link MarketplaceInfo}
 *
 * @author David Green
 */
public class MarketplaceInfoSerializationTest {

	public static class TestMarketplaceInfo extends MarketplaceInfo {

		public File loadedFrom;

		public TestMarketplaceInfo() {
			super();
		}

		public TestMarketplaceInfo(MarketplaceInfo info) {
			super(info);
		}

		public File computeRegistryFile(boolean save) {
			return save ? createRegistryFile().save() : createRegistryFile().load();
		}

		@Override
		public TestMarketplaceInfo load() {
			return (TestMarketplaceInfo) super.load();
		}

		@Override
		protected TestMarketplaceInfo doLoad(File loadFile) {
			MarketplaceInfo loaded = super.doLoad(loadFile);
			if (loaded == null) {
				return null;
			}
			TestMarketplaceInfo testInfo = new TestMarketplaceInfo(loaded);
			testInfo.loadedFrom = loadFile;
			return testInfo;
		}

		@Override
		public File computeBundleRegistryFile() {
			return super.computeBundleRegistryFile();
		}

		@Override
		public File computeConfigurationAreaRegistryFile() {
			return super.computeConfigurationAreaRegistryFile();
		}

		@Override
		public File computeUserHomeRegistryFile(File userHome) {
			return super.computeUserHomeRegistryFile(userHome);
		}

		@Override
		public File computeLegacyUserHomeRegistryFile(File userHome) {
			return super.computeLegacyUserHomeRegistryFile(userHome);
		}

		@Override
		public File getConfigurationArea() {
			return super.getConfigurationArea();
		}

		@Override
		public Location getConfigurationLocation() {
			return super.getConfigurationLocation();
		}

		@Override
		protected TestRegistryFile createRegistryFile() {
			RegistryFile registryFile = super.createRegistryFile();
			return new TestRegistryFile(registryFile);
		}

		public static class TestRegistryFile extends RegistryFile {

			public TestRegistryFile(RegistryFile registryFile) {
				super(registryFile);
			}

			@Override
			public File[] getLocations() {
				return super.getLocations();
			}

			@Override
			public boolean isFile(File file) {
				return super.isFile(file);
			}

			@Override
			public boolean isDirectory(File file) {
				return super.isDirectory(file);
			}

			@Override
			public boolean exists(File file) {
				return super.exists(file);
			}

			@Override
			public boolean canRead(File file) {
				return super.canRead(file);
			}

			@Override
			protected boolean canWrite(File file) {
				return super.canWrite(file);
			}

			@Override
			public boolean mkdirs(File file) {
				return super.mkdirs(file);
			}

			@Override
			public boolean createNewFile(File file) throws IOException {
				return super.createNewFile(file);
			}
		}
	}

	@Rule
	public TemporaryFolder testData = new TemporaryFolder(getWorkDir());

	@Rule
	public TemporaryEnvironment env = new TemporaryEnvironment();

	private TestMarketplaceInfo catalogRegistry;

	private File userHome;

	private File configurationDirectory;

	private Location configurationLocation;

	private static File getWorkDir() {
		URL target = MarketplaceInfoSerializationTest.class.getResource("/target");
		assertNotNull(target);
		try {
			target = FileLocator.resolve(target);
			assertEquals("file", target.getProtocol());
			return new File(new File(target.toURI()), "work");
		} catch (Exception e) {
			throw new UnsupportedOperationException("Workdir construction failed", e);
		}
	}

	@Before
	public void before() throws Exception {
		catalogRegistry = spy(new TestMarketplaceInfo());

		userHome = testData.newFolder("user home");
		env.set("user.home", userHome.getAbsolutePath());

		configurationDirectory = testData.newFolder("user home", "my eclipse", "configuration");
		configurationLocation = mock(Location.class);
		when(configurationLocation.getURL()).thenReturn(new URL(configurationDirectory.toURI().toString().replace("%20",
				" ")));
		when(catalogRegistry.getConfigurationLocation()).thenReturn(configurationLocation);
	}

	@Before
	@After
	public void clearTestBundleRegistry() {
		File dataFile = Platform.getBundle(MarketplaceClientUi.BUNDLE_ID).getBundleContext().getDataFile(
				"MarketplaceInfo.xml");
		assertTrue(dataFile == null || !dataFile.isFile() || dataFile.delete());
	}

	@Test
	public void testResolveConfigurationArea() {
		File configurationArea = catalogRegistry.getConfigurationArea();
		assertEquals(this.configurationDirectory, configurationArea);
	}

	@Test
	public void testResolveUrlEncodedConfigurationArea() throws MalformedURLException {
		when(configurationLocation.getURL()).thenReturn(configurationDirectory.toURI().toURL());
		File configurationArea = catalogRegistry.getConfigurationArea();
		assertEquals(this.configurationDirectory, configurationArea);
	}

	@Test
	public void testResolveNullConfigurationArea() {
		when(catalogRegistry.getConfigurationLocation()).thenReturn(null);
		File configurationArea = catalogRegistry.getConfigurationArea();
		assertNull(configurationArea);
	}

	@Test
	public void testResolveNonFileConfigurationArea() throws MalformedURLException {
		when(configurationLocation.getURL()).thenReturn(new URL("http://example.org/configuration/"));
		File configurationArea = catalogRegistry.getConfigurationArea();
		assertNull(configurationArea);
	}

	@Test
	public void testResolveConfigurationAreaRegistryFile() throws MalformedURLException {
		File configurationAreaRegistryFile = catalogRegistry.computeConfigurationAreaRegistryFile();
		assertEquals(new File(configurationDirectory, "org.eclipse.epp.mpc.ui/MarketplaceInfo.xml"),
				configurationAreaRegistryFile);
	}

	@Test
	public void testResolveNullConfigurationAreaRegistryFile() throws MalformedURLException {
		when(catalogRegistry.getConfigurationArea()).thenReturn(null);
		File configurationAreaRegistryFile = catalogRegistry.computeConfigurationAreaRegistryFile();
		assertNull(configurationAreaRegistryFile);
	}

	@Test
	public void loadFromUserHomeRegistryFile() throws Exception {
		File userHomeRegistryFile = getUserHomeRegistryFile();
		createEmptyRegistryFile(userHomeRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(false);
		assertEquals(userHomeRegistryFile, registryFile);

		File saveRegistryFile = catalogRegistry.computeRegistryFile(true);
		assertEquals(catalogRegistry.computeConfigurationAreaRegistryFile(), saveRegistryFile);
	}

	@Test
	public void loadFromLegacyUserHomeRegistryFile() throws Exception {
		File legacyRegistryFile = getLegacyUserHomeRegistryFile();
		createEmptyRegistryFile(legacyRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(false);
		assertEquals(legacyRegistryFile, registryFile);

		File saveRegistryFile = catalogRegistry.computeRegistryFile(true);
		assertEquals(catalogRegistry.computeConfigurationAreaRegistryFile(), saveRegistryFile);
	}

	@Test
	public void loadFromUserHomeRegistryFileWithLegacy() throws Exception {
		File userHomeRegistryFile = getUserHomeRegistryFile();
		createEmptyRegistryFile(userHomeRegistryFile);

		File legacyRegistryFile = getLegacyUserHomeRegistryFile();
		createEmptyRegistryFile(legacyRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(false);
		assertEquals(getUserHomeRegistryFile(), registryFile);
	}

	@Test
	public void loadFromBundleLocation() throws Exception {
		File bundleRegistryFile = catalogRegistry.computeBundleRegistryFile();
		createEmptyRegistryFile(bundleRegistryFile);
		File userHomeRegistryFile = getUserHomeRegistryFile();
		createEmptyRegistryFile(userHomeRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(false);
		assertEquals(bundleRegistryFile, registryFile);
	}

	@Test
	public void loadFromConfigurationArea() throws Exception {
		File configurationAreaFile = catalogRegistry.computeConfigurationAreaRegistryFile();
		createEmptyRegistryFile(configurationAreaFile);
		File userHomeRegistryFile = getUserHomeRegistryFile();
		createEmptyRegistryFile(userHomeRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(false);
		assertEquals(configurationAreaFile, registryFile);
	}

	@Test
	public void saveToUserHomeRegistryFileWithLegacy() throws Exception {
		File userHomeRegistryFile = getUserHomeRegistryFile();
		File legacyUserHomeRegistryFile = getLegacyUserHomeRegistryFile();
		createEmptyRegistryFile(legacyUserHomeRegistryFile);

		setupRestrictedRegistry(userHomeRegistryFile, legacyUserHomeRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(true);
		assertEquals(getUserHomeRegistryFile(), registryFile);
		assertTrue(registryFile.isFile());
		assertFalse(MessageFormat.format("Migration to new location failed, ''{0}'' still exists",
				getLegacyUserHomeRegistryFile().getAbsolutePath()), getLegacyUserHomeRegistryFile().exists());
	}

	@Test
	public void saveToUserHomeRegistryFileWithCurrentAndLegacy() throws Exception {
		File userHomeRegistryFile = getUserHomeRegistryFile();
		File legacyUserHomeRegistryFile = getLegacyUserHomeRegistryFile();
		createEmptyRegistryFile(userHomeRegistryFile);
		createEmptyRegistryFile(legacyUserHomeRegistryFile);

		setupRestrictedRegistry(userHomeRegistryFile, legacyUserHomeRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(true);
		assertEquals(getUserHomeRegistryFile(), registryFile);
		assertTrue(registryFile.isFile());
		assertFalse(MessageFormat.format("Migration to new location failed, ''{0}'' still exists",
				getLegacyUserHomeRegistryFile().getAbsolutePath()), getLegacyUserHomeRegistryFile().exists());
	}

	@Test
	public void saveToUserHomeForReadOnlyConfigurationArea() throws Exception {
		File userHomeRegistryFile = getUserHomeRegistryFile();
		File configurationAreaRegistryFile = catalogRegistry.computeConfigurationAreaRegistryFile();
		createEmptyRegistryFile(configurationAreaRegistryFile);

		setupRestrictedRegistry(userHomeRegistryFile);

		File registryFile = catalogRegistry.computeRegistryFile(true);
		assertEquals(userHomeRegistryFile, registryFile);
		assertTrue(registryFile.isFile());
		assertTrue(configurationAreaRegistryFile.exists());
	}

	@Test
	public void load() throws Exception {
		File registryFile = getUserHomeRegistryFile();
		copyRegistryFile(registryFile, 100);

		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertFalse(loaded.getIuToNodeKey().isEmpty());
		assertFalse(loaded.getNodeKeyToIU().isEmpty());

		MarketplaceNodeCatalogItem item = MarketplaceInfoTest.createTestItem();
		catalogRegistry.map(item.getMarketplaceUrl(), item.getData());
		assertEquals(catalogRegistry.getIuToNodeKey(), loaded.getIuToNodeKey());
		assertEquals(catalogRegistry.getNodeKeyToIU(), loaded.getNodeKeyToIU());
	}

	@Test
	public void saveAndLoad() throws Exception {
		MarketplaceNodeCatalogItem item = MarketplaceInfoTest.createTestItem();
		catalogRegistry.map(item.getMarketplaceUrl(), item.getData());

		File registryFile = getUserHomeRegistryFile();
		new MarketplaceInfo(catalogRegistry).save(registryFile);

		assertTrue(MessageFormat.format("Registry file ''{0}'' does not exist", registryFile.getAbsolutePath()),
				registryFile.exists());
		assertTrue(MessageFormat.format("Registry file ''{0}'' is empty", registryFile.getAbsolutePath()), registryFile
				.length() > 0);

		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNotNull(loaded);
		assertNotSame(catalogRegistry, loaded);
		assertFalse(loaded.getIuToNodeKey().isEmpty());
		assertFalse(loaded.getNodeKeyToIU().isEmpty());
		assertEquals(catalogRegistry.getIuToNodeKey(), loaded.getIuToNodeKey());
		assertEquals(catalogRegistry.getNodeKeyToIU(), loaded.getNodeKeyToIU());
	}

	@Test
	public void loadNonExisting() {
		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull("Unexpectedly loaded from " + loadPath(loaded), loaded);
	}

	private static String loadPath(MarketplaceInfo loaded) {
		if (loaded == null) {
			return "nowhere";
		}
		if (loaded instanceof TestMarketplaceInfo) {
			TestMarketplaceInfo testInfo = (TestMarketplaceInfo) loaded;
			return testInfo.loadedFrom == null ? "unknown" : testInfo.loadedFrom.getAbsolutePath();
		}
		return "unknown";
	}

	@Test
	public void loadEmpty() throws Exception {
		createEmptyRegistryFile(getUserHomeRegistryFile());
		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull("Unexpectedly loaded from " + loadPath(loaded), loaded);
	}

	@Test
	public void loadIncomplete() throws Exception {
		File registryFile = getUserHomeRegistryFile();
		copyRegistryFile(registryFile, 20);

		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull("Unexpectedly loaded from " + loadPath(loaded), loaded);
	}

	@Test
	public void loadGarbage() throws Exception {
		createEmptyRegistryFile(getUserHomeRegistryFile());
		FileOutputStream os = new FileOutputStream(getUserHomeRegistryFile());
		os.write(new byte[] { (byte) 0x31, (byte) 0xcc, (byte) 0x2a, (byte) 0x99, (byte) 0x84, (byte) 0xc2, (byte) 0x5a,
				(byte) 0xe2, (byte) 0xd6, (byte) 0xc8, (byte) 0xbb, (byte) 0x46, (byte) 0x67, (byte) 0xbb, (byte) 0x5d,
				(byte) 0x63, (byte) 0x2f, (byte) 0x16, (byte) 0x68, (byte) 0xf1, (byte) 0x09, (byte) 0xf4, (byte) 0x35,
				(byte) 0x92 });
		os.close();
		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull("Unexpectedly loaded from " + loadPath(loaded), loaded);
		assertFalse(MessageFormat.format("Corrupted registry file ''{0}'' was not deleted", getUserHomeRegistryFile()
				.getAbsolutePath()), getUserHomeRegistryFile().exists());
	}

	private TestRegistryFile setupRestrictedRegistry(File... writable) throws IOException {
		//Set up registry so bundle and config locations aren't writable
		TestRegistryFile registry = spy(catalogRegistry.createRegistryFile());
		doReturn(false).when(registry).createNewFile(any());
		doReturn(false).when(registry).canWrite(any());
		doReturn(false).when(registry).mkdirs(any());
		doReturn(registry).when(catalogRegistry).createRegistryFile();

		for (File file : writable) {
			doCallRealMethod().when(registry).canWrite(file);
			doCallRealMethod().when(registry).createNewFile(file);
			doCallRealMethod().when(registry).mkdirs(file.getParentFile());
		}
		return registry;
	}

	private static void copyRegistryFile(File target, int percent) throws Exception {
		URL registryContent = MarketplaceInfoSerializationTest.class.getResource("MarketplaceInfo.xml");
		ReadableByteChannel in = null;
		FileChannel out = null;
		FileOutputStream fs = null;
		try {
			URLConnection connection = registryContent.openConnection();
			in = Channels.newChannel(connection.getInputStream());
			int size = connection.getContentLength();
			target.getParentFile().mkdirs();
			fs = new FileOutputStream(target);
			out = fs.getChannel();
			out.transferFrom(in, 0, (percent == 100 ? size : (int) (0.01 * percent * size)));
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (fs != null) {
				fs.close();
			}
		}
	}

	private static void createEmptyRegistryFile(File target) throws IOException {
		target.getParentFile().mkdirs();
		assertTrue(target.getParentFile().isDirectory());
		target.createNewFile();
		assertTrue(target.isFile());
	}

	private MarketplaceInfo loadMarketplaceInfo() {
		MarketplaceInfo loaded = catalogRegistry.load();
		return loaded;
	}

	private File getUserHomeRegistryFile() {
		return new File(userHome, ".eclipse/mpc/MarketplaceInfo.xml");
	}

	private File getLegacyUserHomeRegistryFile() {
		return new File(userHome, ".eclipse_mpc/MarketplaceInfo.xml");
	}
}
