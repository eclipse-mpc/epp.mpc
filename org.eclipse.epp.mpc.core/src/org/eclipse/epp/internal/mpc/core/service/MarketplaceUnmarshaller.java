/*******************************************************************************
 * Copyright (c) 2014, 2019 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.core.MarketplaceClientCore;
import org.eclipse.epp.internal.mpc.core.service.xml.StripInvalidXMLCharsReader;
import org.eclipse.epp.internal.mpc.core.service.xml.Unmarshaller;
import org.eclipse.epp.mpc.core.service.IMarketplaceUnmarshaller;
import org.eclipse.epp.mpc.core.service.UnmarshalException;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.component.annotations.Component;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author Carsten Reckord
 */
@Component(name = "org.eclipse.epp.mpc.core.unmarshaller")
public class MarketplaceUnmarshaller implements IMarketplaceUnmarshaller {

	@Override
	public <T> T unmarshal(InputStream in, Class<T> type, IProgressMonitor monitor) throws IOException,
	UnmarshalException {
		if (in == null) {
			throw new IOException(Messages.MarketplaceUnmarshaller_errorNullStream);
		}
		final Unmarshaller unmarshaller = new Unmarshaller();
		final XMLReader xmlReader = Unmarshaller.createXMLReader(unmarshaller);

		BufferedInputStream bufferedInput = in instanceof BufferedInputStream ? (BufferedInputStream) in
				: new BufferedInputStream(in);
		ByteBuffer peekBuffer = peekResponseContent(bufferedInput);

		// FIXME how can the charset be determined?
		Reader reader = new InputStreamReader(bufferedInput, StandardCharsets.UTF_8);
		reader = new StripInvalidXMLCharsReader(reader);
		try {
			xmlReader.parse(new InputSource(reader));
		} catch (final SAXException e) {
			IStatus error = createContentError(peekBuffer,
					NLS.bind(Messages.MarketplaceUnmarshaller_invalidResponseContent, e.getMessage()), e);
			throw new UnmarshalException(error);
		}

		Object model = unmarshaller.getModel();
		if (model == null) {
			// if we reach here this should never happen
			IStatus error = createContentError(peekBuffer,
					Messages.MarketplaceUnmarshaller_unexpectedResponseContentNullResult, null);
			throw new UnmarshalException(error);
		} else {
			try {
				return type.cast(model);
			} catch (Exception e) {
				String message = NLS.bind(Messages.DefaultMarketplaceService_unexpectedResponseContent,
						model.getClass().getSimpleName());
				IStatus error = createContentError(peekBuffer, message, e);
				throw new UnmarshalException(error);
			}
		}
	}

	private ByteBuffer peekResponseContent(BufferedInputStream bufferedInput) throws IOException {
		bufferedInput.mark(2049);
		ReadableByteChannel inputChannel = Channels.newChannel(bufferedInput);
		ByteBuffer peekBuffer = ByteBuffer.allocate(2048);
		while (peekBuffer.hasRemaining()) {
			int read = inputChannel.read(peekBuffer);
			if (read == -1) {
				break;
			}
		}
		bufferedInput.reset();
		peekBuffer.flip();
		return peekBuffer;
	}

	private IStatus createContentError(ByteBuffer peekBuffer, String message, Throwable t) {
		IStatus status = createErrorStatus(message, t);
		if (peekBuffer != null && peekBuffer.hasRemaining()) {
			IStatus contentInfo = createContentInfo(peekBuffer);
			if (contentInfo != null) {
				MultiStatus multiStatus = new MultiStatus(status.getPlugin(), status.getCode(), status.getMessage(),
						status.getException());
				multiStatus.add(contentInfo);
				status = multiStatus;
			}
		}
		return status;
	}

	private IStatus createContentInfo(ByteBuffer peekBuffer) {
		try {
			StringBuilder message = new StringBuilder("Received response begins with:\n\n"); //$NON-NLS-1$
			CharsetDecoder decoder = StandardCharsets.US_ASCII.newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPLACE);
			decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
			decoder.replaceWith("?"); //$NON-NLS-1$
			CharBuffer charBuffer = decoder.decode(peekBuffer);

			BufferedReader reader = new BufferedReader(new CharArrayReader(charBuffer.array(), 0, charBuffer.limit()));
			for (int i = 0; i < 3; i++) {
				//include the first three lines
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				char[] safeChars = line.toCharArray();
				for (int j = 0; j < safeChars.length; j++) {
					char c = safeChars[j];
					if (c < 32 || c >= 127) {
						//replace potentially non-printable character
						safeChars[j] = '?';
					}
				}
				message.append(i + 1).append(": ").append(new String(safeChars)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return new Status(IStatus.INFO, MarketplaceClientCore.BUNDLE_ID, 0, message.toString(), null);
		} catch (Exception e) {
			// ignore - this is only additional diagnostic info, so don't bother anybody with errors assembling it
			return null;
		}
	}

	protected IStatus createErrorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, MarketplaceClientCore.BUNDLE_ID, 0, message, t);
	}

}
