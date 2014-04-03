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
import java.util.Hashtable;
import java.util.List;

import org.eclipse.epp.internal.mpc.core.util.ProxyHelper;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.mpc.core.service.ITransportFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class MarketplaceClientCorePlugin implements BundleActivator {

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
	}

	public void stop(BundleContext context) throws Exception {
		serviceHelper.stopTracking(context);
		serviceHelper = null;
		unregisterServices();
		ProxyHelper.releaseProxyService();
		instance = null;
	}

	private void registerServices(BundleContext context) {
		List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();
		this.serviceRegistrations = serviceRegistrations;

		List<ITransportFactory> factories = TransportFactory.listAvailableFactories();//highest-prio factory comes first
		int prio = 100 * factories.size();//prio counts down from highest value to 0 in steps of 100
		for (ITransportFactory factory : factories) {
			prio -= 100;
			Hashtable<String, Object> properties = new Hashtable<String, Object>();
			properties.put(Constants.SERVICE_RANKING, prio);
			ServiceRegistration<ITransportFactory> registration = context.registerService(ITransportFactory.class,
					factory, properties);
			serviceRegistrations.add(registration);
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
}
