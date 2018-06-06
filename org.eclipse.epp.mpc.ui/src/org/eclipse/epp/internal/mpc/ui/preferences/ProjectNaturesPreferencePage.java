/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.preferences;

import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.internal.mpc.ui.discovery.MissingNatureDetector;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class ProjectNaturesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.eclipse.epp.mpc.projectnatures"; //$NON-NLS-1$

	public ProjectNaturesPreferencePage() {
		super(Messages.ProjectNatures, null, SWT.FLAT);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(MarketplaceClientUiPlugin.getInstance().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(MissingNatureDetector.ENABLEMENT_PROPERTY,
				Messages.MissingNatureDetector_enable, getFieldEditorParent()));
		if (getContainer() instanceof IWorkbenchPreferenceContainer) {
			new PreferenceLinkArea(getFieldEditorParent(), SWT.NONE, "org.eclipse.ui.preferencePages.FileEditors", //$NON-NLS-1$
					Messages.PreferencePage_linkToEditorSettings, (IWorkbenchPreferenceContainer) getContainer(), null);
		}
	}

}
