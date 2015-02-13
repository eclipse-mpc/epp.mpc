/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for manipulating text.
 *
 * @author David Green
 */
public class TextUtil {
	private static final Pattern STRIP_TAG_PATTERN = Pattern.compile(
			"</?[a-zA-Z]+[0-6]?(\\s+[a-zA-Z]+\\s*=\\s*((('|\")[^>]*?\\4)|(\\S+)))*\\s*/?>", Pattern.MULTILINE); //$NON-NLS-1$

	/**
	 * Strip HTML tags such that the returned text is suitable for display.
	 *
	 * @param text
	 *            the text to adjust
	 * @return the text, possibly altered
	 */
	public static String stripHtmlMarkup(String text) {
		if (text == null) {
			return null;
		}
		String result = ""; //$NON-NLS-1$
		int lastOffset = 0;
		Matcher matcher = STRIP_TAG_PATTERN.matcher(text);
		while (matcher.find()) {
			int start = matcher.start();
			if (start > lastOffset) {
				result += text.substring(lastOffset, start);
			}
			lastOffset = matcher.end();
		}
		if (lastOffset < text.length()) {
			result += text.substring(lastOffset);
		}
		return result;
	}

	/**
	 * Given text that may include HTML tags but may also include whitespace intended to imply formatting, return
	 * representative HTML markup
	 *
	 * @param text
	 *            the text to be marked up
	 * @return HTML markup
	 */
	public static String cleanInformalHtmlMarkup(String text) {
		if (text == null) {
			return null;
		}
		// replace dual newlines with paragraph tags, but not if between tags
		text = Pattern.compile("(?<!>)\\s*?((\\r\\n)|\\n|\\r){2,}\\s*(?!<)").matcher(text).replaceAll("<p>"); //$NON-NLS-1$ //$NON-NLS-2$

		return text;
	}

	public static String escapeText(String text) {
		if (text == null) {
			return null;
		}
		text = text.replace("&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$
		return text;
	}

	public static String join(String delim, String... parts) {
		if (parts == null) {
			return null;
		}
		if (parts.length == 0) {
			return ""; //$NON-NLS-1$
		}
		if ("".equals(delim)) { //$NON-NLS-1$
			delim = null;
		}
		StringBuilder joined = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0 && delim != null) {
				joined.append(delim);
			}
			joined.append(parts[i]);
		}
		return joined.toString();
	}
}
