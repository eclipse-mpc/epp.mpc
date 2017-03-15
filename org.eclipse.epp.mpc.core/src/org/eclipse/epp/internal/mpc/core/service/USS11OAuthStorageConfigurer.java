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
package org.eclipse.epp.internal.mpc.core.service;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.oauth.EclipseOAuthCredentialsProvider;
import org.eclipse.userstorage.oauth.OAuthCredentialsProvider;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

class USS11OAuthStorageConfigurer extends StorageConfigurer {

	private static final String CLIENT_ID = "1e8b68d6e5015c2bcf8e03d44dd97fc431f17e394860f08f1a05978c"; //$NON-NLS-1$

	private static final String CLIENT_SECRET = "26e9f81e5d02d4a019e042d012a861f34a446c35cb84070e80278d8e"; //$NON-NLS-1$

	private static final String CLIENT_KEY = "9d08c11f742f53a2cd6348d373fd1fa0b079694199f760b315a12a"; //$NON-NLS-1$

	// FIXME: these should be the uss_project_* alternatives
	//private static final String[] DEFAULT_MPC_SCOPES = { "profile", "uss_project_retrieve", "uss_project_update", "uss_project_delete" };
	private static final String[] DEFAULT_MPC_SCOPES = { "profile", "uss_retrieve", "uss_update" }; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

	static class Factory extends StorageConfigurer.Factory {

		@Override
		boolean isApplicable() {
			Version ussVersion = getUSSVersion();
			if (ussVersion.getMajor() > 1 || (ussVersion.getMajor() == 1 && ussVersion.getMinor() >= 1)) {
				Bundle oauthBundle = Platform.getBundle("org.eclipse.userstorage.oauth"); //$NON-NLS-1$
				if (oauthBundle == null) {
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		StorageConfigurer doCreate() {
			return new USS11OAuthStorageConfigurer();
		}

	}

	@Override
	public void configure(IStorage storage) throws CoreException {
		ICredentialsProvider provider = createCredentialsProvider();
		storage.setCredentialsProvider(provider);
	}

	ICredentialsProvider createCredentialsProvider() {
		return new USS11NonInteractiveOAuthCredentialsProvider(createOAuthCredentialsProvider(),
				URI.create("https://accounts.eclipse.org/"), //$NON-NLS-1$
				decrypt(CLIENT_ID, CLIENT_KEY), decrypt(CLIENT_SECRET, CLIENT_KEY), DEFAULT_MPC_SCOPES,
				URI.create("http://localhost/")); //$NON-NLS-1$
	}

	OAuthCredentialsProvider createOAuthCredentialsProvider() {
		EclipseOAuthCredentialsProvider oauthProvider = new EclipseOAuthCredentialsProvider(URI.create("https://accounts.eclipse.org/"), //$NON-NLS-1$
				decrypt(CLIENT_ID, CLIENT_KEY), decrypt(CLIENT_SECRET, CLIENT_KEY), DEFAULT_MPC_SCOPES,
				URI.create("http://localhost/")); //$NON-NLS-1$
		//oauthProvider.setShell(null);
		return oauthProvider;
	}

	@Override
	public Object setInteractive(IStorage storage, boolean interactive) throws CoreException {
		ICredentialsProvider credentialsProvider = storage.getCredentialsProvider();
		boolean oldInteractive = setInteractive(credentialsProvider, interactive);
		return oldInteractive;
	}

	boolean setInteractive(ICredentialsProvider credentialsProvider, boolean interactive) {
		ToggleInteractive uss11Provider = (ToggleInteractive) credentialsProvider;
		boolean oldInteractive = uss11Provider.isInteractive();
		uss11Provider.setInteractive(interactive);
		return oldInteractive;
	}

	@Override
	void restoreInteractive(IStorage storage, Object restoreValue) throws CoreException {
		setInteractive(storage, Boolean.TRUE.equals(restoreValue));
	}

	private static String decrypt(String str, String key) {
		byte[] keyBytes = hexToBytes(key);
		byte[] bytes = hexToBytes(str);
		byte[] result = new byte[bytes.length - 1];

		int j = bytes[result.length] - Byte.MIN_VALUE;
		crypt(bytes, result, keyBytes, result.length, j);
		try {
			return new String(result, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static byte[] hexToBytes(String hexStr) {
		int hexStrLen = hexStr.length();
		if ((hexStrLen & 1) == 1) {
			hexStr = '0' + hexStr;
			hexStrLen++;
		}
		byte[] out = new byte[hexStrLen / 2];

		// Safe to assume the string is even length
		byte b1, b2;
		for (int i = 0; i < hexStrLen; i += 2) {
			b1 = (byte) Character.digit(hexStr.charAt(i), 16);
			b2 = (byte) Character.digit(hexStr.charAt(i + 1), 16);
			if (b1 < 0 || b2 < 0) {
				throw new NumberFormatException(hexStr);
			}

			out[i / 2] = (byte) (b1 << 4 | b2 & 0xff);
		}
		return out;
	}

	private static void crypt(byte[] bytes, byte[] result, byte[] key, int length, int j) {
		for (int i = 0; i < length; i++) {
			result[i] = (byte) (bytes[i] ^ key[j++ % key.length]);
		}
	}

	static interface ToggleInteractive {
		void setInteractive(boolean interactive);

		boolean isInteractive();
	}
}
