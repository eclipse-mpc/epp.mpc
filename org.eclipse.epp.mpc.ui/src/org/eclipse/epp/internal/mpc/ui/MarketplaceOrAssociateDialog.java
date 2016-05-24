/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui;

import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

public class MarketplaceOrAssociateDialog extends TitleAreaDialog {

	private final String fileExtensionLabel;

	private final IEditorDescriptor currentEditor;

	private Button showProposalsRadio;

	private Button associateRadio;

	private boolean showProposals;

	private boolean associate;

	private Image wizban;

	protected MarketplaceOrAssociateDialog(Shell shell, String fileExtensionLabel, IEditorDescriptor currentEditor) {
		super(shell);
		this.fileExtensionLabel = fileExtensionLabel;
		this.currentEditor = currentEditor;
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.MarketplaceOrAssociateDialog_title);
	}

	@Override
	public Control createDialogArea(Composite parent) {
		setTitle(Messages.MarketplaceOrAssociateDialog_title);
		setMessage(NLS.bind(Messages.MarketplaceOrAssociateDialog_message, fileExtensionLabel));
		wizban = DiscoveryImages.BANNER_DISOVERY.createImage();
		setTitleImage(wizban);

		Composite res = new Composite(parent, SWT.NONE);
		GridData resGridData = GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).create();
		res.setLayoutData(resGridData);
		GridLayoutFactory.swtDefaults().equalWidth(false).applyTo(res);

		Label label = new Label(res, SWT.WRAP);
		label.setText(createDescription());
		GridData labelGridData = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).create();
		label.setLayoutData(labelGridData);

		showProposalsRadio = new Button(res, SWT.RADIO);
		GridDataFactory.swtDefaults()
				.align(SWT.FILL, SWT.TOP)
				.grab(true, false)
				.indent(0, 10)
				.applyTo(showProposalsRadio);
		showProposalsRadio.setText(Messages.MarketplaceOrAssociateDialog_showProposals);
		showProposalsRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelection();
			}
		});

		associateRadio = new Button(res, SWT.RADIO);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(associateRadio);
		associateRadio
		.setText(NLS.bind(Messages.MarketplaceOrAssociateDialog_associate, fileExtensionLabel,
				currentEditor.getLabel()));
		associateRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelection();
			}
		});

		Link linkToPreferences = new Link(res, SWT.NONE);
		GridDataFactory.swtDefaults()
				.align(SWT.FILL, SWT.TOP)
				.grab(true, true)
				.indent(0, 20)
				.applyTo(linkToPreferences);
		linkToPreferences.setText(Messages.MarketplaceOrAssociateDialog_linkToPreferences);
		linkToPreferences.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(getShell(),
						"org.eclipse.ui.preferencePages.FileEditors", null, //$NON-NLS-1$
						null);
				pref.setBlockOnOpen(false);
				if (pref != null) {
					pref.open();
				}
			}
		});

		Point hint = res.computeSize(SWT.NONE, SWT.DEFAULT);
		labelGridData.widthHint = hint.x + 20;
		labelGridData.heightHint = SWT.DEFAULT;
		hint = res.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		resGridData.widthHint = hint.x;
		resGridData.heightHint = SWT.DEFAULT;

		showProposalsRadio.setSelection(true);
		updateSelection();
		return res;
	}

	private String createDescription() {
		String editorId = currentEditor == null ? null : currentEditor.getId();
		if (IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID.equals(editorId)) {
			return NLS.bind(
					Messages.MarketplaceOrAssociateDialog_descriptionEmbeddedSystemEditor,
					fileExtensionLabel);
		} else if (IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID.equals(editorId)) {
			return NLS.bind(
					Messages.MarketplaceOrAssociateDialog_descriptionExternalSystemEditor,
					fileExtensionLabel);
		} else if (IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID.equals(editorId)) {
			return NLS.bind(
					Messages.MarketplaceOrAssociateDialog_descriptionSimpleTextEditor,
					fileExtensionLabel);
		} else {
			return NLS.bind(Messages.MarketplaceOrAssociateDialog_message, fileExtensionLabel);
		}
	}

	private void updateSelection() {
		showProposals = showProposalsRadio.getSelection();
		associate = associateRadio.getSelection();
	}

	@Override
	public boolean close() {
		if (super.close()) {
			wizban.dispose();
		}
		return false;
	}

	public boolean isShowProposals() {
		return this.showProposals;
	}

	public boolean isAssociateToExtension() {
		return this.associate;
	}
}
