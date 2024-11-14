/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public enum PKIContext {
	INSTANCE;

	boolean isEnabled = false;
	SSLContext sslContext = null;
	public SSLContext get() {
		if (sslContext == null) {
			enablePKI();
		}
		return sslContext;
	}

	public void enablePKI() {

		Optional<String> keystoreContainer = null;
		Optional<String> truststoreContainer = null;
		Optional<String> keystoreContainerPw = null;
		Optional<String> keystoreContainerType = null;
		Optional<String> truststoreContainerPw = null;
		Optional<String> truststoreContainerType = null;

		KeyStore keyStore = null;
		KeyStore trustStore = null;

		try {
			keystoreContainer = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStore"));//$NON-NLS-1$
			keystoreContainerType = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreType"));//$NON-NLS-1$
			keystoreContainerPw = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStorePassword"));//$NON-NLS-1$

			truststoreContainer = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore"));//$NON-NLS-1$
			truststoreContainerPw = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStorePassword"));//$NON-NLS-1$
			truststoreContainerType = Optional.ofNullable(System.getProperty("javax.net.ssl.trustStoreType"));//$NON-NLS-1$

			// Ensure all the properties are set for a complete PKI
			if ((keystoreContainer.isEmpty()) || (truststoreContainer.isEmpty()) || (keystoreContainerPw.isEmpty())
					|| (keystoreContainerType.isEmpty()) || (truststoreContainerPw.isEmpty())
					|| (truststoreContainerType.isEmpty())) {
				//System.out.println("PKIContext  get---NO PROPERTIES CONFIGURED");
				// Returns the context from the SSLContextHelper
				sslContext = null;
				return;
			}

			String keyStoreLocation = keystoreContainer.get();
			String keyStoreType = keystoreContainerType.get();
			String keyStorePassword = keystoreContainerPw.get();

			String trustStoreLocation = truststoreContainer.get();
			String trustStorePassword = truststoreContainerPw.get();
			String trustStoreType = truststoreContainerType.get();

			try {

				InputStream is = Files.newInputStream(Paths.get(keyStoreLocation));
				keyStore = KeyStore.getInstance(keyStoreType);
				keyStore.load(is, keyStorePassword.toCharArray());
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(keyStore, keyStorePassword.toCharArray());
				sslContext = SSLContext.getInstance("TLS");

				InputStream tis = Files.newInputStream(Paths.get(trustStoreLocation));
				trustStore = KeyStore.getInstance(trustStoreType);
				trustStore.load(tis, trustStorePassword.toCharArray());

				TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
				tmf.init(trustStore);

				sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				SSLContext.setDefault(sslContext);
				setEnabled(true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

}
