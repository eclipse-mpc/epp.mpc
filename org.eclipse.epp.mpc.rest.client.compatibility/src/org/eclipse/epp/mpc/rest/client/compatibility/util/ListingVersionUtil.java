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
package org.eclipse.epp.mpc.rest.client.compatibility.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.epp.mpc.rest.model.ListingVersion;

public class ListingVersionUtil {

	private static final String ZEROES = "00000000000000000000"; //$NON-NLS-1$

	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+"); //$NON-NLS-1$

	public static Optional<ListingVersion> newestApplicableVersion(List<ListingVersion> versions) {
		//TODO filter by applicable ListingVersion according to current platform etc
		return versions.stream().sorted(Comparator.<ListingVersion, String> comparing(v -> {
			Matcher paddingMatcher = NUMBER_PATTERN.matcher(v.getVersion());
			StringBuffer sb = new StringBuffer();
			while (paddingMatcher.find()) {
				String pad = paddingMatcher.group().length() > ZEROES.length() ? "" //$NON-NLS-1$
						: ZEROES.substring(paddingMatcher.group().length());
				paddingMatcher.appendReplacement(sb, pad + paddingMatcher.group());
			}
			paddingMatcher.appendTail(sb);
			return sb.toString();
		}).reversed()).findFirst();
	}

}
