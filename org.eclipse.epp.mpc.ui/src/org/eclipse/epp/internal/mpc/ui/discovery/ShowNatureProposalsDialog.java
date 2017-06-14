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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

final class ShowNatureProposalsDialog extends TitleAreaDialog {
	private Image wizban;

	private final Map<String, Collection<INode>> candidates;

	private CheckboxTableViewer naturesCheckList;

	private final Set<String> selectedNatures;

	ShowNatureProposalsDialog(Shell parentShell, Map<String, Collection<INode>> candidates) {
		super(parentShell);
		this.candidates = candidates;
		this.selectedNatures = new LinkedHashSet<String>(candidates.keySet());
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
		label.setText(Messages.MissingNatureDetector_Message);

		naturesCheckList = CheckboxTableViewer.newCheckList(res,
				SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		naturesCheckList.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return ((Set<?>) inputElement).toArray();
			}
		});
		naturesCheckList.setComparator(new ViewerComparator());
		naturesCheckList.setInput(candidates.keySet());
		naturesCheckList.setAllChecked(true);
		GridDataFactory.fillDefaults().applyTo(naturesCheckList.getControl());
		naturesCheckList.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				Button okButton = getButton(IDialogConstants.OK_ID);
				if (okButton != null) {
					okButton.setEnabled(canComplete());
				}
				updateSelectedNatures();
			}
		});

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

	private void updateSelectedNatures() {
		selectedNatures.clear();
		Object[] checkedElements = naturesCheckList.getCheckedElements();
		if (checkedElements == null) {
			return;
		}
		for (Object selected : checkedElements) {
			selectedNatures.add(selected.toString());
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.setText(Messages.MissingNatureDetector_ShowSolutions);
		okButton.setEnabled(canComplete());
	}

	protected boolean canComplete() {
		Object[] checkedElements = naturesCheckList.getCheckedElements();
		return checkedElements != null && checkedElements.length > 0;
	}

	@Override
	public boolean close() {
		updateSelectedNatures();
		if (super.close()) {
			wizban.dispose();
		}
		return false;
	}

	public Set<String> getSelectedNatures() {
		return selectedNatures;
	}
}