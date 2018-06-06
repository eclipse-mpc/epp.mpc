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

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.win.WindowsCredentialsProvider;
import org.apache.http.impl.auth.win.WindowsNTLMSchemeFactory;
import org.apache.http.impl.auth.win.WindowsNegotiateSchemeFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.HttpClientCustomizer;
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
		Registry<AuthSchemeProvider> authSchemeRegistry = createAuthSchemeRegistry();
		return winBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
	}

	private Registry<AuthSchemeProvider> createAuthSchemeRegistry() {
		@SuppressWarnings("restriction")
		Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider> create()
		.register(AuthSchemes.BASIC, new BasicSchemeFactory())
		.register(AuthSchemes.DIGEST, new DigestSchemeFactory())
		.register(AuthSchemes.NTLM, new WindowsNTLMSchemeFactory(servicePrincipalName))
		.register(AuthSchemes.SPNEGO, new WindowsNegotiateSchemeFactory(servicePrincipalName))
		.register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
		.build();
		return authSchemeRegistry;
	}

	public CredentialsProvider customizeCredentialsProvider(CredentialsProvider credentialsProvider) {
		if (credentialsProvider == null || !isWinAuthAvailable()) {
			return credentialsProvider;
		}

		@SuppressWarnings("restriction")
		CredentialsProvider winCredentialsProvider = new WindowsCredentialsProvider(credentialsProvider);
		return winCredentialsProvider;
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
