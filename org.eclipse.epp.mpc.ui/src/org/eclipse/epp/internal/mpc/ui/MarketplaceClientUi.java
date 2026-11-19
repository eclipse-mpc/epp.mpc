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
 * 	The Eclipse Foundation - initial API and implementation
 *    Yatta Solutions - error handling (bug 374105), public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * @author Steffen Pingel
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceClientUi {

	private static final String DOT_FEATURE_DOT_GROUP = ".feature.group"; //$NON-NLS-1$

	public static final String BUNDLE_ID = "org.eclipse.epp.mpc.ui"; //$NON-NLS-1$

	public static void error(Throwable exception) {
		String exceptionMessage = exception.getMessage();
		if (exceptionMessage == null) {
			exceptionMessage = exception.getClass().getSimpleName();
		}
		ILog.get().error(NLS.bind(Messages.MarketplaceClientUi_unexpectedException_reason, exceptionMessage),
				exception);
	}

	public static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(MarketplaceClientDebug.class).getBundleContext();
	}

	public static Map<String, IInstallableUnit> computeInstalledIUsById(IProgressMonitor monitor) {
		Map<String, IInstallableUnit> iUs = new HashMap<>();
		BundleContext bundleContext = MarketplaceClientUi.getBundleContext();
		ServiceReference<IProvisioningAgent> serviceReference = bundleContext.getServiceReference(IProvisioningAgent.class);
		if (serviceReference != null) {
			IProvisioningAgent agent = bundleContext.getService(serviceReference);
			try {
				IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
				if (profileRegistry != null) {
					IProfile profile = profileRegistry.getProfile(ProvisioningUI.getDefaultUI().getProfileId());
					if (profile != null) {
						IQueryResult<IInstallableUnit> result = profile.available(QueryUtil.createIUGroupQuery(),
								monitor);
						for (IInstallableUnit unit : result) {
							iUs.put(unit.getId(), unit);
						}
					}
				}
			} finally {
				bundleContext.ungetService(serviceReference);
			}
		}
		return iUs;
	}

	public static Set<String> computeInstalledFeatures(IProgressMonitor monitor) {
		Map<String, IInstallableUnit> iusById = computeInstalledIUsById(monitor);
		Set<String> features = new HashSet<>(iusById.keySet());

		if (features.isEmpty()) {
			// probably a self-hosted environment
			IBundleGroupProvider[] bundleGroupProviders = Platform.getBundleGroupProviders();
			for (IBundleGroupProvider provider : bundleGroupProviders) {
				if (monitor.isCanceled()) {
					break;
				}
				IBundleGroup[] bundleGroups = provider.getBundleGroups();
				for (IBundleGroup group : bundleGroups) {
					String identifier = group.getIdentifier();
					if (!identifier.endsWith(DOT_FEATURE_DOT_GROUP)) {
						identifier += DOT_FEATURE_DOT_GROUP;
					}
					features.add(identifier);
				}
			}
		}
		return features;
	}

	public static void setDefaultHelp(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, "org.eclipse.epp.mpc.help.ui.userGuide"); //$NON-NLS-1$
	}

	public static void handle(Throwable t, final int style) {
		handle(Status.error(t.getLocalizedMessage(), t), style);
	}

	public static void handle(final IStatus status, final int style) {
		if (PlatformUI.isWorkbenchRunning()) {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (workbench != null) {
				final Display workbenchDisplay = workbench.getDisplay();
				if (!workbenchDisplay.isDisposed()) {
					Runnable logRunnable = () -> {
						if (!workbenchDisplay.isDisposed() && PlatformUI.isWorkbenchRunning()) {
							IWorkbench workbench1 = PlatformUI.getWorkbench();
							if (workbench1 != null) {
								try {
									StatusManager.getManager().handle(status, style);
									return;
								} catch (Exception ex) {
									// Display might get disposed during call to handle due to workspace shutdown or similar.
									// In that case, just log...
								}
							}
						}
						ILog.get().log(status);
					};
					if (runIn(workbenchDisplay, logRunnable)) {
						return;
					}
				}
			}
		}
		//else just log
		ILog.get().log(status);
	}

	private static boolean runIn(Display display, Runnable runnable) {
		if (display == null || display.isDisposed()) {
			return false;
		} else if (display == Display.getCurrent()) {
			if (display.isDisposed()) {
				return false;
			}
			runnable.run();
			return true;
		} else {
			try {
				display.asyncExec(runnable);
				return true;
			} catch (SWTException e) {
				if (e.code == SWT.ERROR_DEVICE_DISPOSED) {
					return false;
				}
				throw e;
			}
		}
	}

	public static boolean useNativeBorders() {
		IPreferencesService service = Platform.getPreferencesService();
		return service.getBoolean(BUNDLE_ID, "native-borders", true, //$NON-NLS-1$
				new IScopeContext[] { InstanceScope.INSTANCE });
	}
}
