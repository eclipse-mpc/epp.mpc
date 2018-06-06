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
package org.eclipse.epp.internal.mpc.core.transport.httpclient;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

public class SystemCredentialsProvider extends SystemDefaultCredentialsProvider {
	@Override
	public Credentials getCredentials(AuthScope authscope) {
		Credentials credentials = super.getCredentials(authscope);
		if (credentials instanceof NTCredentials) {
			NTCredentials ntCredentials = (NTCredentials) credentials;
			if (ntCredentials.getDomain() == null || ntCredentials.getWorkstation() == null) {
				String domain = ntCredentials.getDomain();
				String userName = ntCredentials.getUserName();
				String workstation = ntCredentials.getWorkstation();
				String strippedUserName = HttpClientProxyUtil.getNTLMUserName(userName);
				if (domain == null || !strippedUserName.equals(userName)) {
					domain = HttpClientProxyUtil.getNTLMUserDomain(userName);
					if (domain != null) {
						userName = strippedUserName;
					}
				}
				if (workstation == null) {
					workstation = HttpClientProxyUtil.getNTLMWorkstation();
				}
				credentials = new NTCredentials(userName, ntCredentials.getPassword(), workstation, domain);
			}
		}
		return credentials;
	}
}
