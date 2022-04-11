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
package org.eclipse.epp.internal.mpc.core.transport.httpclient.win32;

import java.util.Map;

import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.impl.auth.BasicSchemeFactory;
import org.apache.hc.client5.http.impl.auth.DigestSchemeFactory;
import org.apache.hc.client5.http.impl.auth.KerberosSchemeFactory;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.win.WindowsNTLMSchemeFactory;
import org.apache.hc.client5.http.impl.win.WindowsNegotiateSchemeFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientCustomizer;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.SynchronizedCredentialsProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.sun.jna.platform.win32.Sspi;

public class WinClientBuilderCustomizer implements HttpClientCustomizer {

	public static final String ID = "org.eclipse.epp.mpc.core.transport.http.win32"; //$NON-NLS-1$

	public static final String SERVICE_PRINCIPAL_NAME_ATTRIBUTE = "servicePrincipal"; //$NON-NLS-1$

	public static final String SERVICE_PRINCIPAL_NAME_PROPERTY = ID + "." + SERVICE_PRINCIPAL_NAME_ATTRIBUTE; //$NON-NLS-1$

	private static Boolean winAuthAvailable;

	private String servicePrincipalName;

	public static boolean isWinAuthAvailable() {
		if (winAuthAvailable == null) {
			//from org.apache.http.impl.client.WinHttpClients.isWinAuthAvailable()
			try {
				winAuthAvailable = Sspi.MAX_TOKEN_SIZE > 0;
			} catch (Exception ignore) { // Likely ClassNotFound
				winAuthAvailable = false;
			}
		}
		return winAuthAvailable;
	}

	public void setServicePrincipalName(String servicePrincipalName) {
		this.servicePrincipalName = servicePrincipalName;
	}

	public String getServicePrincipalName() {
		return servicePrincipalName;
	}

	public HttpClientBuilder customizeBuilder(HttpClientBuilder builder) {
		if (!isWinAuthAvailable()) {
			return builder;
		}
		HttpClientBuilder winBuilder = builder == null ? HttpClientBuilder.create() : builder;
		Registry<AuthSchemeFactory> authSchemeRegistry = createAuthSchemeRegistry();
		return winBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
	}

	private Registry<AuthSchemeFactory> createAuthSchemeRegistry() {
		@SuppressWarnings("restriction")
		Registry<AuthSchemeFactory> authSchemeRegistry = RegistryBuilder.<AuthSchemeFactory> create()
		.register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
		.register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
		.register(StandardAuthScheme.NTLM, new WindowsNTLMSchemeFactory(servicePrincipalName))
		.register(StandardAuthScheme.SPNEGO, new WindowsNegotiateSchemeFactory(servicePrincipalName))
		.register(StandardAuthScheme.KERBEROS, KerberosSchemeFactory.DEFAULT)
		.build();
		return authSchemeRegistry;
	}

	@Override
	public CredentialsStore customizeCredentialsProvider(CredentialsStore credentialsProvider) {
		if (credentialsProvider == null || !isWinAuthAvailable()) {
			return credentialsProvider;
		}

		@SuppressWarnings("restriction")
		CredentialsStore winCredentialsStore = new SynchronizedCredentialsProvider(credentialsProvider);
		return winCredentialsStore;
	}

	public synchronized void activate(BundleContext context, Map<?, ?> properties) {
		this.servicePrincipalName = getServicePrincipalName(properties);
	}

	private String getServicePrincipalName(Map<?, ?> properties) {
		Object servicePrincipalValue = properties.get(SERVICE_PRINCIPAL_NAME_ATTRIBUTE);
		if (servicePrincipalValue != null) {
			return servicePrincipalValue.toString();
		}
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		if (bundle != null) {
			return bundle.getBundleContext().getProperty(SERVICE_PRINCIPAL_NAME_PROPERTY);
		}
		return System.getProperty(SERVICE_PRINCIPAL_NAME_PROPERTY);
	}

}
