/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service.xml;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class StripInvalidXMLCharsReader extends FilterReader {

	public StripInvalidXMLCharsReader(Reader in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		for (int r = -1; (r = super.read()) != -1;) {
			if (isValidXMLChar((char) r)) {
				return r;
			}
		}
		return -1;
	}

	private boolean isValidXMLChar(char c) {
		// https://marketplace.eclipse.org/content/surfexample/api/p
		// Strip ASCII control characters disallowed in general by XML 1.0.
		return (c >= 0x20) || (c == 0x9) || (c == 0xA) || (c == 0xD);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int read = super.read(cbuf, off, len);
		int remaining = read;
		for (int i = off; i < off + remaining;) {
			char c = cbuf[i];
			if (!isValidXMLChar(c)) {
				remaining--;
				int after = off + remaining - i;
				if (after > 0) {
					System.arraycopy(cbuf, i + 1, cbuf, i, after);
					cbuf[remaining] = 0;
				}
			} else {
				++i;
			}
		}
		return remaining;
	}
}
