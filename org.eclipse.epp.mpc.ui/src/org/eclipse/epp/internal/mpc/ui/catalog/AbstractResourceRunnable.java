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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUI;
import org.eclipse.osgi.util.NLS;

/**
 * A runnable that downloads a resource from an URL
 * 
 * @author David Green
 */
abstract class AbstractResourceRunnable implements Runnable {

	protected ResourceProvider resourceProvider;

	protected String resourceUrl;

	public AbstractResourceRunnable(ResourceProvider resourceProvider, String resourceUrl) {
		this.resourceProvider = resourceProvider;
		this.resourceUrl = resourceUrl;
	}

	public void run() {
		try {
			URL imageUrl = new URL(resourceUrl);

			// FIXME replace by InputStream in =
			// RepositoryTransport.getInstance().stream(location,
			// monitor);
			InputStream in = imageUrl.openStream();
			try {
				resourceProvider.putResource(resourceUrl, in);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			MarketplaceClientUI.error(NLS.bind("Resource not found: {0}", resourceUrl), e);
		} catch (IOException e) {
			MarketplaceClientUI.error(e);
		}
		if (resourceProvider.containsResource(resourceUrl)) {
			resourceRetrieved();
		}
	}

	protected abstract void resourceRetrieved();

}
