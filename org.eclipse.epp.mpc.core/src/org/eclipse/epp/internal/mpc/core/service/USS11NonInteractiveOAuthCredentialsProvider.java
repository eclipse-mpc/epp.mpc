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

import java.net.URI;

import org.apache.http.client.fluent.Request;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.oauth.OAuthCredentialsProvider;
import org.eclipse.userstorage.spi.Credentials;

class USS11NonInteractiveOAuthCredentialsProvider extends OAuthCredentialsProvider
implements USS11OAuthStorageConfigurer.ToggleInteractive {

	private final OAuthCredentialsProvider delegate;

	private boolean interactive;

	USS11NonInteractiveOAuthCredentialsProvider(OAuthCredentialsProvider delegate, URI authService, String clientId,
			String clientSecret, String[] scopes, URI expectedCallback) {
		super(authService, clientId, clientSecret, scopes, expectedCallback);
		this.delegate = delegate;
	}

	protected boolean canProvideCredentialsNonInteractively() {
		return false;
	}

	public Credentials provideCredentials(IStorageService service, boolean reauthentication) {
		if (!isInteractive() && !canProvideCredentialsNonInteractively()) {
			return null;
		}
		return delegate.provideCredentials(service, reauthentication);
	}

	@Override
	public Credentials getCredentials(IStorageService service) {
		return delegate.getCredentials(service);
	}

	@Override
	public boolean hasCredentials(IStorageService service) {
		return delegate.hasCredentials(service);
	}

	@Override
	public boolean isValid(Credentials credentials) {
		return delegate.isValid(credentials);
	}

	@Override
	public boolean updateCredentials(IStorageService service, Credentials credential) {
		return delegate.updateCredentials(service, credential);
	}

	@Override
	public Request configureRequest(Request request, URI uri, Credentials credentials) {
		return delegate.configureRequest(request, uri, credentials);
	}

	@Override
	public void setStateCode(String stateCode) {
		super.setStateCode(stateCode);
		delegate.setStateCode(stateCode);
	}

	public OAuthCredentialsProvider getDelegate() {
		return delegate;
	}

	public void setInteractive(boolean interactive) {
		this.interactive = interactive;
	}

	public boolean isInteractive() {
		return interactive;
	}
}
