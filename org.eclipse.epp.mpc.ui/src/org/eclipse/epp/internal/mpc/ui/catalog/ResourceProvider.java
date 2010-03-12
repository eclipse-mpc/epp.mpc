/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUI;

/**
 * @author David Green
 */
class ResourceProvider {

	private File dir;

	private Map<String, File> resources = new ConcurrentHashMap<String, File>();

	public ResourceProvider() throws IOException {
		dir = File.createTempFile(ResourceProvider.class.getSimpleName(), ".tmp");
		dir.delete();
		if (!dir.mkdirs()) {
			throw new IOException(dir.getAbsolutePath());
		}
	}

	public URL getResource(String resourceName) {
		File resource = resources.get(resourceName);
		try {
			return resource == null ? null : resource.toURL();
		} catch (MalformedURLException e) {
			MarketplaceClientUI.error(e);
			return null;
		}
	}

	public boolean containsResource(String resourceName) {
		return resources.containsKey(resourceName);
	}

	public void putResource(String resourceName, InputStream input) throws IOException {
		String filenameHint = resourceName;
		if (filenameHint.lastIndexOf('/') != -1) {
			filenameHint = filenameHint.substring(filenameHint.lastIndexOf('/') + 1);
		}
		filenameHint = filenameHint.replaceAll("[^a-zA-Z0-9\\.]", "_");
		if (filenameHint.length() > 32) {
			filenameHint = filenameHint.substring(filenameHint.length() - 32);
		}
		File outputFile = File.createTempFile("res_", filenameHint, dir);
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
		try {
			input = new BufferedInputStream(input);
			int i;
			while ((i = input.read()) != -1) {
				output.write(i);
			}
		} finally {
			output.close();
		}
		resources.put(resourceName, outputFile);
	}

	public void dispose() {
		if (dir != null && dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					file.delete();
				}
			}
			dir.delete();
		}
	}
}
