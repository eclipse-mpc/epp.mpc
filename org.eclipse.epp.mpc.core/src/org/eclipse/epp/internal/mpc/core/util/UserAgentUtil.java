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
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientTransport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class UserAgentUtil {

	public static String computeUserAgent() {
		Bundle mpcCoreBundle = FrameworkUtil.getBundle(HttpClientTransport.class);
		BundleContext context = mpcCoreBundle.getBundleContext();

		String version = getAgentVersion(mpcCoreBundle);
		String java = getAgentJava(context);
		String os = getAgentOS(context);
		String language = getProperty(context, "osgi.nl", "en");//$NON-NLS-1$//$NON-NLS-2$
		String agentDetail = getAgentDetail(context);

		String userAgent = MessageFormat.format("mpc/{0} ({1}; {2}; {3}) {4}", //$NON-NLS-1$
				/*{0}*/version, /*{1}*/java, /*{2}*/os, /*{3}*/language, /*{4}*/agentDetail);
		return userAgent;
	}

	private static String getAgentVersion(Bundle bundle) {
		return formatVersion(bundle.getVersion());
	}

	private static String formatVersion(String version) {
		try {
			return formatVersion(Version.parseVersion(version));
		} catch (RuntimeException ex) {
			String[] parts = version.split("[\\._\\-]", 4); //$NON-NLS-1$
			String shortVersion = Arrays.stream(parts)
					.limit(3)
					.map(part -> part == null || part.isBlank() ? "0" : part) //$NON-NLS-1$
					.collect(Collectors.joining(".")); //$NON-NLS-1$
			return shortVersion;
		}
	}

	private static String formatVersion(Version version) {
		return MessageFormat.format("{0}.{1}.{2}", version.getMajor(), version.getMinor(), //$NON-NLS-1$
				version.getMicro());
	}

	private static String getAgentJava(BundleContext context) {
		String javaName = getProperty(context, "java.runtime.name", //$NON-NLS-1$
				getProperty(context, "java.vendor", "Unknown Java")); //$NON-NLS-1$ //$NON-NLS-2$
		if (javaName.endsWith("Runtime Environment")) { //$NON-NLS-1$
			javaName = javaName.substring(0, javaName.length() - "Runtime Environment".length()); //$NON-NLS-1$
		}
		String javaVersion = getProperty(context, "java.runtime.version", //$NON-NLS-1$
				getProperty(context, "java.version", "0.0.0")); //$NON-NLS-1$ //$NON-NLS-2$
		String java = MessageFormat.format("{0} {1}", javaName, javaVersion); //$NON-NLS-1$
		return java;
	}

	private static String getAgentOS(BundleContext context) {
		String os;
		String osName = getProperty(context, "org.osgi.framework.os.name", null); //$NON-NLS-1$
		String osVersion = getProperty(context, "org.osgi.framework.os.version", "Unknown Version"); //$NON-NLS-1$ //$NON-NLS-2$
		String osArch = getProperty(context, "org.osgi.framework.processor", "Unknown Arch");//$NON-NLS-1$//$NON-NLS-2$
		os = osName == null ? "Unknown OS" : MessageFormat.format("{0} {1} {2}", osName, osVersion, osArch); //$NON-NLS-1$ //$NON-NLS-2$
		return os;
	}

	private static String getAgentDetail(BundleContext context) {
		String agentDetail;
		agentDetail = getProperty(context, HttpClientTransport.USER_AGENT_PROPERTY, null);
		if (agentDetail == null) {
			String productId = getProperty(context, "eclipse.product", null); //$NON-NLS-1$
			String productVersion = getProperty(context, "eclipse.buildId", null); //$NON-NLS-1$
			if (productId == null || productVersion == null) {
				Map<String, String> defaultRequestMetaParameters = ServiceLocator.computeProductInfo();
				productId = getProperty(defaultRequestMetaParameters, DefaultMarketplaceService.META_PARAM_PRODUCT,
						null);
				productVersion = getProperty(defaultRequestMetaParameters,
						DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION, "Unknown Build"); //$NON-NLS-1$
			}
			if (productId == null) {
				agentDetail = "Unknown Product"; //$NON-NLS-1$
			} else if (productVersion == null) {
				agentDetail = productId;
			} else {
				productVersion = formatVersion(productVersion);
				agentDetail = MessageFormat.format("{0}/{1}", productId, productVersion); //$NON-NLS-1$
			}

			String appId = getProperty(context, "eclipse.application", null); //$NON-NLS-1$
			if (appId == null) {
				IProduct product = Platform.getProduct();
				if (product != null) {
					appId = product.getApplication();
				}
			}
			if (appId != null) {
				agentDetail = MessageFormat.format("{0} ({1})", agentDetail, appId); //$NON-NLS-1$
			}
		}
		return agentDetail;
	}

	private static String getProperty(BundleContext context, String key, String defaultValue) {
		String value = context.getProperty(key);
		if (value != null) {
			return value;
		}
		return defaultValue;
	}

	private static String getProperty(Map<?, String> properties, Object key, String defaultValue) {
		String value = properties.get(key);
		if (value != null) {
			return value;
		}
		return defaultValue;
	}

}
