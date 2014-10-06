/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.catalog;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceInfo;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Test {@link MarketplaceInfo}
 *
 * @author David Green
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class MarketplaceInfoSerializationTest {

	private final class TestMarketplaceInfo extends MarketplaceInfo {
		@Override
		protected File computeRegistryFile() {
			return getUserHomeRegistryFile();
		}
	}

	private File registryLocation;
	private TestMarketplaceInfo catalogRegistry;

	@Before
	public void before() throws Exception {
		File targetDir = null;
		URL target = MarketplaceInfoSerializationTest.class.getResource("/target");
		if (target != null) {
			target = FileLocator.resolve(target);
			if ("file".equals(target.getProtocol())) {
				targetDir = new File(target.toURI());
			}
		}
		registryLocation = File.createTempFile("mpc-registry-", null, targetDir);
		registryLocation.delete();
		registryLocation.mkdirs();
		assertTrue(registryLocation.isDirectory());
		assertTrue(!getUserHomeRegistryFile().exists() || getUserHomeRegistryFile().delete());

		catalogRegistry = new TestMarketplaceInfo();
	}

	@After
	public void after() throws Exception {
		delete(registryLocation);
	}

	private static void delete(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (File child : children) {
				delete(child);
			}
		}
		if (!file.delete()) {
			file.deleteOnExit();
		}
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
		new MarketplaceInfo(catalogRegistry).save(getUserHomeRegistryFile());

		File registryFile = getUserHomeRegistryFile();
		assertTrue(MessageFormat.format("Registry file ''{0}'' does not exist", registryFile.getAbsolutePath()),
				registryFile.exists());
		assertTrue(MessageFormat.format("Registry file ''{0}'' is empty", registryFile.getAbsolutePath()), registryFile
				.length() > 0);

		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNotNull(loaded);
		assertFalse(loaded.getIuToNodeKey().isEmpty());
		assertFalse(loaded.getNodeKeyToIU().isEmpty());
		assertEquals(catalogRegistry.getIuToNodeKey(), loaded.getIuToNodeKey());
		assertEquals(catalogRegistry.getNodeKeyToIU(), loaded.getNodeKeyToIU());
	}

	@Test
	public void loadNonExisting() {
		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull(loaded);
	}

	@Test
	public void loadEmpty() throws Exception {
		createEmptyRegistryFile(getUserHomeRegistryFile());
		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull(loaded);
	}

	@Test
	public void loadIncomplete() throws Exception {
		File registryFile = getUserHomeRegistryFile();
		copyRegistryFile(registryFile, 20);

		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull(loaded);
	}

	@Test
	public void loadGarbage() throws Exception {
		createEmptyRegistryFile(getUserHomeRegistryFile());
		FileOutputStream os = new FileOutputStream(getUserHomeRegistryFile());
		os.write(new byte[] { (byte) 0x31, (byte) 0xcc, (byte) 0x2a, (byte) 0x99, (byte) 0x84, (byte) 0xc2,
				(byte) 0x5a, (byte) 0xe2, (byte) 0xd6, (byte) 0xc8, (byte) 0xbb, (byte) 0x46, (byte) 0x67, (byte) 0xbb,
				(byte) 0x5d, (byte) 0x63, (byte) 0x2f, (byte) 0x16, (byte) 0x68, (byte) 0xf1, (byte) 0x09, (byte) 0xf4,
				(byte) 0x35, (byte) 0x92 });
		os.close();
		MarketplaceInfo loaded = loadMarketplaceInfo();
		assertNull(loaded);
		assertFalse(MessageFormat.format("Corrupted registry file ''{0}'' was not deleted", getUserHomeRegistryFile()
				.getAbsolutePath()), getUserHomeRegistryFile().exists());
	}

	private void copyRegistryFile(File target, int percent) throws Exception {
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

	private void createEmptyRegistryFile(File target) throws IOException {
		target.getParentFile().mkdirs();
		assertTrue(target.getParentFile().isDirectory());
		target.createNewFile();
		assertTrue(target.isFile());
	}

	private MarketplaceInfo loadMarketplaceInfo() {
		MarketplaceInfo cleanInfo = new TestMarketplaceInfo();
		assertTrue(cleanInfo.getIuToNodeKey().isEmpty());
		assertTrue(cleanInfo.getNodeKeyToIU().isEmpty());
		MarketplaceInfo loaded = cleanInfo.load();
		return loaded;
	}

	private File getUserHomeRegistryFile() {
		return new File(registryLocation, ".eclipse_mpc/MarketplaceInfo.xml");
	}
}
