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

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.core5.http.protocol.HttpContext;

public class SystemCredentialsProvider extends SystemDefaultCredentialsProvider {
	@Override
	public Credentials getCredentials(AuthScope authscope, HttpContext context) {
		Credentials credentials = super.getCredentials(authscope, context);
		if (credentials instanceof NTCredentials) {
			NTCredentials ntCredentials = (NTCredentials) credentials;
			if (ntCredentials.getDomain() == null || ntCredentials.getWorkstation() == null) {
				String domain = ntCredentials.getDomain();
				String userName = ntCredentials.getUserName();
				String workstation = ntCredentials.getWorkstation();
				String strippedUserName = NTLMDomainUtil.getNTLMUserName(userName);
				if (domain == null || !strippedUserName.equals(userName)) {
					domain = NTLMDomainUtil.getNTLMUserDomain(userName);
					if (domain != null) {
						userName = strippedUserName;
					}
				}
				if (workstation == null) {
					workstation = NTLMDomainUtil.getNTLMWorkstation();
				}
				credentials = new NTCredentials(userName, ntCredentials.getPassword(), workstation, domain);
			}
		}
		return credentials;
	}
}
