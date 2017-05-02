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
package org.eclipse.epp.internal.mpc.core.transport.httpclient;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public interface HttpClientCustomizer {
	HttpClientBuilder customizeBuilder(HttpClientBuilder builder);

	CredentialsProvider customizeCredentialsProvider(CredentialsProvider credentialsProvider);
}