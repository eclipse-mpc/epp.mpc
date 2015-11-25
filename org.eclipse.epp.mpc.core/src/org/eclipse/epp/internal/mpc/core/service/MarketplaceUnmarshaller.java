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
package org.eclipse.epp.internal.mpc.core.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.xml.Unmarshaller;
import org.eclipse.epp.mpc.core.service.IMarketplaceUnmarshaller;
import org.eclipse.epp.mpc.core.service.UnmarshalException;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author Carsten Reckord
 */
public class MarketplaceUnmarshaller implements IMarketplaceUnmarshaller {

	public <T> T unmarshal(InputStream in, Class<T> type, IProgressMonitor monitor) throws IOException,
	UnmarshalException {
		final Unmarshaller unmarshaller = new Unmarshaller();
		final XMLReader xmlReader = Unmarshaller.createXMLReader(unmarshaller);

		// FIXME how can the charset be determined?
		Reader reader = new InputStreamReader(new BufferedInputStream(in), RemoteMarketplaceService.UTF_8);
		try {
			xmlReader.parse(new InputSource(reader));
		} catch (final SAXException e) {
			throw new UnmarshalException(createErrorStatus(e.getMessage(), null));
		}

		Object model = unmarshaller.getModel();
		if (model == null) {
			// if we reach here this should never happen
			throw new IllegalStateException();
		} else {
			try {
				return type.cast(model);
			} catch (Exception e) {
				String message = NLS.bind(Messages.DefaultMarketplaceService_unexpectedResponseContent,
						model.getClass().getSimpleName());
				throw new UnmarshalException(createErrorStatus(message, null));
			}
		}
	}

	protected IStatus createErrorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, 0, message, t);
	}

}
