/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.MissingNatureDetector;

public class MPCPreferenceInitializer extends AbstractPreferenceInitializer {

	public MPCPreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		MarketplaceClientUiPlugin.getInstance().getPreferenceStore().setDefault(
				MissingNatureDetector.ENABLEMENT_PROPERTY, true);
	}

}
