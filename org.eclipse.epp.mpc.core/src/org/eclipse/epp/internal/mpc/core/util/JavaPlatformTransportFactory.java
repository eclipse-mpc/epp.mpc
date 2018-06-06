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

import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;

public class JavaPlatformTransportFactory extends TransportFactory {

	@Override
	protected boolean isAvailable() {
		return true;
	}

	@Override
	protected InputStream invokeStream(URI location, IProgressMonitor monitor) throws Exception {
		return location.toURL().openStream();
	}

}
