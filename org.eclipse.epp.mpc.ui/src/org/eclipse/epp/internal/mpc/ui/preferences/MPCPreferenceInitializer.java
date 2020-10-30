/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiResources;
import org.eclipse.epp.internal.mpc.ui.discovery.MissingNatureDetector;

public class MPCPreferenceInitializer extends AbstractPreferenceInitializer {

	public MPCPreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		MarketplaceClientUiResources.getInstance().getPreferenceStore().setDefault(
				MissingNatureDetector.ENABLEMENT_PROPERTY, true);
	}

}
