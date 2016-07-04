/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.epp.internal.mpc.core.util.DebugTraceUtil;
import org.eclipse.epp.internal.mpc.core.util.ProxyHelper;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;

public class MarketplaceClientCorePlugin implements BundleActivator {

	public static final String DEBUG_OPTION = "/debug"; //$NON-NLS-1$

	public static final String DEBUG_FAKE_CLIENT_OPTION = "/client/fakeVersion"; //$NON-NLS-1$

	public static final String DEBUG_CLIENT_OPTIONS_PATH = MarketplaceClientCore.BUNDLE_ID + "/client/"; //$NON-NLS-1$

	public static final String DEBUG_CLIENT_REMOVE_OPTION = "xxx"; //$NON-NLS-1$

	public static boolean DEBUG = false;

	public static boolean DEBUG_FAKE_CLIENT = false;

	private static DebugTrace debugTrace;

	private static MarketplaceClientCorePlugin instance;

	private Bundle bundle;

	private List<ServiceRegistration<?>> serviceRegistrations;

	private ServiceHelperImpl serviceHelper;

	public void start(BundleContext context) throws Exception {
		bundle = context.getBundle();
		instance = this;
		ProxyHelper.acquireProxyService();
		registerServices(context);
		serviceHelper = new ServiceHelperImpl();
		serviceHelper.startTracking(context);

		Hashtable<String, String> props = new Hashtable<String, String>(2);
		props.put(org.eclipse.osgi.service.debug.DebugOptions.LISTENER_SYMBOLICNAME, MarketplaceClientCore.BUNDLE_ID);
		context.registerService(DebugOptionsListener.class.getName(), new DebugOptionsListener() {
			public void optionsChanged(DebugOptions options) {
				DebugTrace debugTrace = null;
				boolean debug = options.getBooleanOption(MarketplaceClientCore.BUNDLE_ID + DEBUG_OPTION, false);
				boolean fakeClient = false;
				if (debug) {
					debugTrace = options.newDebugTrace(MarketplaceClientCore.BUNDLE_ID);
					fakeClient = options.getBooleanOption(MarketplaceClientCore.BUNDLE_ID + DEBUG_FAKE_CLIENT_OPTION,
							false);
				}
				DEBUG = debug;
				DEBUG_FAKE_CLIENT = fakeClient;
				MarketplaceClientCorePlugin.debugTrace = debugTrace;
			}
		}, props);
	}

	public void stop(BundleContext context) throws Exception {
		serviceHelper.stopTracking(context);
		serviceHelper = null;
		unregisterServices();
		ProxyHelper.releaseProxyService();
		debugTrace = null;
		instance = null;
	}

	private void registerServices(BundleContext context) throws InvalidSyntaxException {
		List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();
		this.serviceRegistrations = serviceRegistrations;

		List<ITransportFactory> factories = TransportFactory.listAvailableFactories();//highest-prio factory comes first
		if (factories.isEmpty()) {
			return;
		}

		Collection<ServiceReference<ITransportFactory>> serviceReferences = context
				.getServiceReferences(ITransportFactory.class, null);
		int lowestPriority = Integer.MAX_VALUE;
		for (ServiceReference<ITransportFactory> serviceReference : serviceReferences) {
			Object legacyProperty = serviceReference.getProperty(TransportFactory.LEGACY_TRANSPORT_KEY);
			if (legacyProperty != null) {
				continue;
			}
			Integer ranking = (Integer) serviceReference.getProperty(Constants.SERVICE_RANKING);
			lowestPriority = Math.min(lowestPriority, ranking == null ? 0 : ranking.intValue());
		}

		int maxLegacyPriority, step;
		if (lowestPriority >= 0) {
			maxLegacyPriority = -100;
			step = 100;
		} else {
			int available = lowestPriority - Integer.MIN_VALUE;
			step = Math.min(100, available / factories.size());
			if (step == 0) {
				step = 1;
				maxLegacyPriority = Integer.MIN_VALUE + factories.size();
			} else {
				maxLegacyPriority = lowestPriority - step;
			}
		}
		int prio = maxLegacyPriority;//prio counts down from 0 in steps of 100
		for (ITransportFactory factory : factories) {
			Hashtable<String, Object> properties = new Hashtable<String, Object>();
			properties.put(Constants.SERVICE_RANKING, prio);
			properties.put(ComponentConstants.COMPONENT_NAME, "legacy:" + factory.getClass().getName());
			properties.put(TransportFactory.LEGACY_TRANSPORT_KEY, true);
			ServiceRegistration<ITransportFactory> registration = context.registerService(ITransportFactory.class,
					factory, properties);
			serviceRegistrations.add(registration);
			prio -= step;
		}
	}

	private void unregisterServices() {
		List<ServiceRegistration<?>> serviceRegistrations = this.serviceRegistrations;
		this.serviceRegistrations = null;
		if (serviceRegistrations != null) {
			for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
				serviceRegistration.unregister();
			}
		}
	}

	public ServiceHelperImpl getServiceHelper() {
		return serviceHelper;
	}

	public static MarketplaceClientCorePlugin getDefault() {
		return instance;
	}

	public static Bundle getBundle() {
		return instance == null ? null : instance.bundle;
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
