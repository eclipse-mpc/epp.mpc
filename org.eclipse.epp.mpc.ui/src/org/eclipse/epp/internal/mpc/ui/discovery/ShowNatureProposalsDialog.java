/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.discovery;

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.epp.internal.mpc.ui.Messages;
import org.eclipse.epp.internal.mpc.ui.preferences.ProjectNaturesPreferencePage;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

final class ShowNatureProposalsDialog extends TitleAreaDialog {
	private Image wizban;

	private final Map<String, Collection<INode>> candidates;

	ShowNatureProposalsDialog(Shell parentShell, Map<String, Collection<INode>> candidates) {
		super(parentShell);
		this.candidates = candidates;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.MissingNatureDetector_Title);
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public Control createDialogArea(Composite parent) {
		setTitle(Messages.MissingNatureDetector_Title);
		setMessage(Messages.MissingNatureDetector_Desc);
		wizban = DiscoveryImages.BANNER_DISOVERY.createImage();
		setTitleImage(wizban);
		Composite res = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
		.grab(true, true)
		.hint(SWT.DEFAULT, SWT.DEFAULT)
		.applyTo(res);
		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).equalWidth(false).applyTo(
				res);
		Label label = new Label(res, SWT.WRAP);
		StringBuilder message = new StringBuilder(Messages.MissingNatureDetector_Message);
		message.append("\n\n"); //$NON-NLS-1$
		SortedSet<String> relevantNatures = new TreeSet<String>(candidates.keySet());
		for (String natureId : relevantNatures) {
			message.append("- "); //$NON-NLS-1$
			message.append(natureId);
			message.append('\n');
		}
		label.setText(message.toString());
		Link preferencesLink = new Link(res, SWT.NONE);
		preferencesLink.setText(Messages.MissingNatureDetector_linkToPreferences);
		preferencesLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(getShell(),
						ProjectNaturesPreferencePage.ID, null, null);
				pref.setBlockOnOpen(false);
				pref.open();
			}
		});
		return res;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(Messages.MissingNatureDetector_ShowSolutions);
	}

	@Override
	public boolean close() {
		if (super.close()) {
			wizban.dispose();
		}
		return false;
	}
}