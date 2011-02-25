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
