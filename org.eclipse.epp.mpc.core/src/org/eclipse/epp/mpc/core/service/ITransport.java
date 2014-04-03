/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An abstraction for a URI-based download channel. The actual download strategy depends on the implementation.
 *
 * @author Carsten Reckord
 */
public interface ITransport {
	public InputStream stream(URI location, IProgressMonitor monitor) throws FileNotFoundException,
	ServiceUnavailableException, CoreException;
}
