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
			if (isValidXMLCodePoint(r)) {
				return r;
			}
		}
		return -1;
	}

	private boolean isValidXMLCodePoint(int cp) {
		return (cp == 0x9) || (cp == 0xA) || (cp == 0xD) || ((cp >= 0x20) && (cp <= 0xD7FF))
				|| ((cp >= 0xE000) && (cp <= 0xFFFD)) || ((cp >= 0x10000) && (cp <= 0x10FFFF));
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int read = super.read(cbuf, off, len);
		int remaining = read;
		for (int i = off; i < off + read; i++) {
			char c = cbuf[i];
			if (!isValidXMLCodePoint(c)) {
				remaining--;
				int after = off + read - i - 1;
				if (after > 0) {
					System.arraycopy(cbuf, i + 1, cbuf, i, after);
					cbuf[remaining] = 0;
				}
			}
		}
		return remaining;
	}
}
