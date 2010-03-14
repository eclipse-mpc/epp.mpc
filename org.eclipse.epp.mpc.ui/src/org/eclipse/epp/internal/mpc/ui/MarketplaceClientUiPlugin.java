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
package org.eclipse.epp.internal.mpc.ui;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * bundle activator. Prefer {@link MarketplaceClientUi} where possible.
 * 
 * @author David Green
 */
public class MarketplaceClientUiPlugin extends AbstractUIPlugin {

	/**
	 * image registry key
	 */
	public static final String IU_ICON_UPDATE = "IU_ICON_UPDATE"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON = "IU_ICON"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String NO_ICON_PROVIDED = "NO_ICON_PROVIDED"; //$NON-NLS-1$

	private static MarketplaceClientUiPlugin instance;

	public MarketplaceClientUiPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		instance = null;
	}

	/**
	 * Get the singleton instance. Prefer {@link MarketplaceClientUi} where possible.
	 */
	public static MarketplaceClientUiPlugin getInstance() {
		return instance;
	}

	@Override
	protected ImageRegistry createImageRegistry() {
		ImageRegistry imageRegistry = super.createImageRegistry();
		imageRegistry.put(NO_ICON_PROVIDED, imageDescriptorFromPlugin(getBundle().getSymbolicName(),
				"icons/noiconprovided.png"));
		imageRegistry.put(IU_ICON, imageDescriptorFromPlugin(getBundle().getSymbolicName(), "icons/iu_obj.gif"));
		imageRegistry.put(IU_ICON_UPDATE, imageDescriptorFromPlugin(getBundle().getSymbolicName(),
				"icons/iu_update_obj.gif"));
		return imageRegistry;
	}
}
