/*******************************************************************************
 * Copyright (c) 2010, 2019 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API, bug 413871: performance
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import static org.eclipse.jface.resource.ResourceLocator.imageDescriptorFromBundle;

import java.util.Hashtable;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.core.util.DebugTraceUtil;
import org.eclipse.epp.internal.mpc.ui.catalog.ResourceProvider;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

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
	public static final String IU_ICON_INSTALL = "IU_ICON_INSTALL"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_UNINSTALL = "IU_ICON_UNINSTALL"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_DISABLED = "IU_ICON_DISABLED"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON = "IU_ICON"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String IU_ICON_ERROR = "IU_ICON_ERROR"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String NEWS_ICON_UPDATE = "NEWS_ICON_UPDATE"; //$NON-NLS-1$

	/**
	 * image registry key
	 */
	public static final String NO_ICON_PROVIDED = "NO_ICON_PROVIDED"; //$NON-NLS-1$

	public static final String NO_ICON_PROVIDED_CATALOG = "NO_ICON_PROVIDED_CATALOG"; //$NON-NLS-1$

	public static final String DEFAULT_MARKETPLACE_ICON = "DEFAULT_MARKETPLACE_ICON"; //$NON-NLS-1$

	public static final String ACTION_ICON_FAVORITES = "ACTION_ICON_FAVORITES"; //$NON-NLS-1$

	public static final String ACTION_ICON_LOGIN = "ACTION_ICON_LOGIN"; //$NON-NLS-1$

	public static final String ACTION_ICON_WARNING = "ACTION_ICON_WARNING"; //$NON-NLS-1$

	public static final String ACTION_ICON_UPDATE = "ACTION_ICON_UPDATE"; //$NON-NLS-1$

	public static final String FAVORITES_LIST_ICON = "FAVORITES_LIST_ICON"; //$NON-NLS-1$

	public static final String ITEM_ICON_STAR = "ITEM_ICON_STAR"; //$NON-NLS-1$

	public static final String ITEM_ICON_STAR_SELECTED = "ITEM_ICON_STAR_SELECTED"; //$NON-NLS-1$

	public static final String ITEM_ICON_SHARE = "ITEM_ICON_SHARE"; //$NON-NLS-1$

	public static final String DEBUG_OPTION = "/debug"; //$NON-NLS-1$

	public static final String DROP_ADAPTER_DEBUG_OPTION = DEBUG_OPTION + "/dnd"; //$NON-NLS-1$

	private static MarketplaceClientUiPlugin instance;

	private static BundleContext bundleContext;

	public static boolean DEBUG = false;

	private static DebugTrace debugTrace;

	private ServiceTracker<IMarketplaceClientService, IMarketplaceClientService> clientServiceTracker;

	private ResourceProvider resourceProvider;

	public MarketplaceClientUiPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
		super.start(context);
		MarketplaceClientUiPlugin.bundleContext = context;
		clientServiceTracker = new ServiceTracker<>(context, IMarketplaceClientService.class, null);
		clientServiceTracker.open();
		resourceProvider = new ResourceProvider();

		Hashtable<String, String> props = new Hashtable<>(2);
		props.put(org.eclipse.osgi.service.debug.DebugOptions.LISTENER_SYMBOLICNAME, MarketplaceClientUi.BUNDLE_ID);
		context.registerService(DebugOptionsListener.class.getName(), (DebugOptionsListener) options -> {
			DebugTrace debugTrace = null;
			boolean debug = options.getBooleanOption(MarketplaceClientUi.BUNDLE_ID + DEBUG_OPTION, false);
			if (debug) {
				debugTrace = options.newDebugTrace(MarketplaceClientUi.BUNDLE_ID);
			}
			DEBUG = debug;
			MarketplaceClientUiPlugin.debugTrace = debugTrace;
		}, props);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Job.getJobManager().cancel(context.getBundle());
		clientServiceTracker.close();
		clientServiceTracker = null;

		if (resourceProvider != null) {
			resourceProvider.dispose();
			resourceProvider = null;
		}
		MarketplaceClientUiPlugin.bundleContext = null;
		super.stop(context);
		debugTrace = null;
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
		imageRegistry.put(NO_ICON_PROVIDED,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/noiconprovided.png") //$NON-NLS-1$
				.get());
		imageRegistry.put(NO_ICON_PROVIDED_CATALOG,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/noiconprovided32.png").get()); //$NON-NLS-1$
		imageRegistry.put(DEFAULT_MARKETPLACE_ICON,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/marketplace_banner.png").get()); //$NON-NLS-1$
		imageRegistry.put(IU_ICON, imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/iu_obj.png").get()); //$NON-NLS-1$
		imageRegistry.put(IU_ICON_UPDATE,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/iu_update_obj.png").get()); //$NON-NLS-1$
		imageRegistry.put(IU_ICON_INSTALL,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/iu_install_obj.png").get()); //$NON-NLS-1$
		imageRegistry.put(IU_ICON_UNINSTALL,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/iu_uninstall_obj.png").get()); //$NON-NLS-1$
		imageRegistry.put(IU_ICON_DISABLED,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/iu_disabled_obj.png").get()); //$NON-NLS-1$
		{
			ImageDescriptor errorOverlay = PlatformUI.getWorkbench()
					.getSharedImages()
					.getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
			Image iuImage = imageRegistry.get(IU_ICON);
			DecorationOverlayIcon iuErrorIcon = new DecorationOverlayIcon(iuImage, errorOverlay,
					IDecoration.BOTTOM_RIGHT);
			imageRegistry.put(IU_ICON_ERROR, iuErrorIcon);
		}

		imageRegistry.put(NEWS_ICON_UPDATE,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/news_update.png").get()); //$NON-NLS-1$
		imageRegistry.put(ITEM_ICON_STAR,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/star.png").get()); //$NON-NLS-1$
		imageRegistry.put(ITEM_ICON_STAR_SELECTED,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/star-selected.png").get()); //$NON-NLS-1$
		imageRegistry.put(ITEM_ICON_SHARE,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/share.png").get()); //$NON-NLS-1$
		imageRegistry.put(ACTION_ICON_FAVORITES,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/action-item-favorites.png").get()); //$NON-NLS-1$
		imageRegistry.put(ACTION_ICON_LOGIN,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/action-item-login.png").get()); //$NON-NLS-1$
		imageRegistry.put(ACTION_ICON_WARNING,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/action-item-warning.png").get()); //$NON-NLS-1$
		imageRegistry.put(ACTION_ICON_UPDATE,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/action-item-update.png").get()); //$NON-NLS-1$
		imageRegistry.put(FAVORITES_LIST_ICON,
				imageDescriptorFromBundle(getBundle().getSymbolicName(), "icons/favorites-list.png").get()); //$NON-NLS-1$
		return imageRegistry;
	}

	public IMarketplaceClientService getClientService() {
		return clientServiceTracker == null ? null : clientServiceTracker.getService();
	}

	public ResourceProvider getResourceProvider() {
		return resourceProvider;
	}

	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	public static void trace(String option, String message) {
		final DebugTrace trace = debugTrace;
		if (DEBUG && trace != null) {
			trace.trace(option, message);
		}
	}

	public static void trace(String option, String message, Object... parameters) {
		final DebugTrace trace = debugTrace;
		if (DEBUG && trace != null) {
			DebugTraceUtil.trace(trace, option, message, parameters);
		}
	}
}
