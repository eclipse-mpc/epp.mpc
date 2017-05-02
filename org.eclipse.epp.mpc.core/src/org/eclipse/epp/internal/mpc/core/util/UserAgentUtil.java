/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.text.MessageFormat;
import java.util.Map;

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
		String language = getProperty(context, "osgi.nl", "unknownLanguage");//$NON-NLS-1$//$NON-NLS-2$
		String agentDetail = getAgentDetail(context);

		String userAgent = MessageFormat.format("mpc/{0} (Java {1}; {2}; {3}) {4}", //$NON-NLS-1$
				/*{0}*/version, /*{1}*/java, /*{2}*/os, /*{3}*/language, /*{4}*/agentDetail);
		return userAgent;
	}

	private static String getAgentVersion(Bundle bundle) {
		String version;
		Version mpcCoreVersion = bundle.getVersion();
		version = MessageFormat.format("{0}.{1}.{2}", mpcCoreVersion.getMajor(), mpcCoreVersion.getMinor(), //$NON-NLS-1$
				mpcCoreVersion.getMicro());
		return version;
	}

	private static String getAgentJava(BundleContext context) {
		String java;
		String javaSpec = getProperty(context, "java.runtime.version", "unknownJava"); //$NON-NLS-1$ //$NON-NLS-2$
		String javaVendor = getProperty(context, "java.vendor", "unknownJavaVendor");//$NON-NLS-1$//$NON-NLS-2$
		java = MessageFormat.format("{0} {1}", javaSpec, javaVendor); //$NON-NLS-1$
		return java;
	}

	private static String getAgentOS(BundleContext context) {
		String os;
		String osName = getProperty(context, "org.osgi.framework.os.name", "unknownOS"); //$NON-NLS-1$ //$NON-NLS-2$
		String osVersion = getProperty(context, "org.osgi.framework.os.version", "unknownOSVersion"); //$NON-NLS-1$ //$NON-NLS-2$
		String osArch = getProperty(context, "org.osgi.framework.processor", "unknownArch");//$NON-NLS-1$//$NON-NLS-2$
		os = MessageFormat.format("{0} {1} {2}", osName, osVersion, osArch); //$NON-NLS-1$
		return os;
	}

	private static String getAgentDetail(BundleContext context) {
		String agentDetail;
		agentDetail = getProperty(context, HttpClientTransport.USER_AGENT_PROPERTY, null);
		if (agentDetail == null) {
			String productId = getProperty(context, "eclipse.product", null); //$NON-NLS-1$
			String productVersion = getProperty(context, "eclipse.buildId", null); //$NON-NLS-1$
			String appId = getProperty(context, "eclipse.application", null); //$NON-NLS-1$
			if (productId == null || productVersion == null) {
				Map<String, String> defaultRequestMetaParameters = ServiceLocator.computeDefaultRequestMetaParameters();
				productId = getProperty(defaultRequestMetaParameters, DefaultMarketplaceService.META_PARAM_PRODUCT,
						"unknownProduct"); //$NON-NLS-1$
				productVersion = getProperty(defaultRequestMetaParameters,
						DefaultMarketplaceService.META_PARAM_PRODUCT_VERSION, "unknownBuildId"); //$NON-NLS-1$
			}
			if (appId == null) {
				IProduct product = Platform.getProduct();
				if (product != null) {
					appId = product.getApplication();
				}
				if (appId == null) {
					appId = "unknownApp"; //$NON-NLS-1$
				}
			}
			agentDetail = MessageFormat.format("{0}/{1} ({2})", productId, productVersion, appId); //$NON-NLS-1$
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
