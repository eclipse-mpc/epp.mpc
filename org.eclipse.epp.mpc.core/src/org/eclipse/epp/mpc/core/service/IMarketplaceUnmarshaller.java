/*******************************************************************************
 * Copyright (c) 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This service provides unmarshalling support for the data returned by the <a
 * href="https://wiki.eclipse.org/Marketplace/REST">Marketplace REST API</a>.
 *
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMarketplaceUnmarshaller {
	/**
	 * Parse the input stream into an object of the given type.
	 *
	 * @throws IOException
	 *             if an error occurs while reading from the stream
	 * @throws UnmarshalException
	 *             if an error occurs while parsing the input, including unexpected content or content that doesn't
	 *             match the given type.
	 */
	public <T> T unmarshal(InputStream in, Class<T> type, IProgressMonitor monitor) throws UnmarshalException,
	IOException;
}
