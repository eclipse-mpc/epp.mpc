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
 *     JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.Arrays;

import org.eclipse.epp.internal.mpc.ui.css.StyleHelper;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;

public class MarketplaceWizardDialog extends AbstractMarketplaceWizardDialog {
	public MarketplaceWizardDialog(Shell parentShell, MarketplaceWizard newWizard) {
		//bug 424729 - make the wizard dialog modal
		//don't pass on parentShell, so we get a new top-level shell with its own taskbar entry
		//TODO is there some way to still get centering on the parentShell?
		super(null, newWizard);
		int shellStyle = getShellStyle();
		int allModal = SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL;
		shellStyle &= ~allModal;
		shellStyle |= SWT.MODELESS;
		setShellStyle(shellStyle);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setData(this);//make jface dialog accessible for swtbot
		new StyleHelper().on(newShell).setClass("MarketplaceWizardDialog").setId("MarketplaceWizard");

		new MarketplaceDropAdapter() {
			@Override
			protected void proceedInstallation(String url) {
				SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(url);
				CatalogDescriptor catalogDescriptor = info.getCatalogDescriptor();
				String installItem = info.getInstallId();
				//we ignore previous wizard state here, since the wizard is still open...
				if (installItem != null && installItem.length() > 0) {
					info.setState(null);
					getWizard().handleInstallRequest(info, url);
				}
			}

			@Override
			protected void proceedFavorites(String url) {
				getWizard().importFavorites(url);
			}
		}.installDropTarget(newShell);
		final IWorkbenchListener workbenchListener = new IWorkbenchListener() {

			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				MarketplaceWizardDialog wizardDialog = MarketplaceWizardDialog.this;
				Shell wizardShell = wizardDialog.getShell();
				if (wizardShell != null && !wizardShell.isDisposed()) {
					if (!forced) {
						MarketplaceWizard wizard = wizardDialog.getWizard();
						boolean hasPendingActions = false;
						IWizardPage currentPage = wizardDialog.getCurrentPage();
						if (currentPage != null && wizard != null) {
							if (currentPage == wizard.getCatalogPage()) {
								hasPendingActions = !wizard.getSelectionModel().getSelectedCatalogItems().isEmpty();
							} else {
								hasPendingActions = true;
							}
						}
						if (hasPendingActions) {
							Shell parentShell = activate(wizardDialog.getShell());
							MessageDialog messageDialog = new MessageDialog(parentShell, Messages.MarketplaceWizardDialog_PromptPendingActionsTitle,
									null, Messages.MarketplaceWizardDialog_PromptPendingActionsMessage,
									MessageDialog.QUESTION_WITH_CANCEL, new String[] { IDialogConstants.YES_LABEL,
											IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL },
									SWT.NONE);
							int result = messageDialog.open();
							switch (result) {
							case 0: //yes
								finishWizard();
								return false;
							case 1: //no
								break;
							case 3: //cancel
							case SWT.DEFAULT: //[x]
							default:
								return false;
							}
						}
					}
					if (forced) {
						wizardShell.close();
					} else {
						boolean closed = wizardDialog.close();
						return closed;
					}
				}
				return true;
			}

			private void finishWizard() {
				MarketplaceWizardDialog wizardDialog = MarketplaceWizardDialog.this;
				MarketplaceWizard wizard = wizardDialog.getWizard();
				IWizardPage currentPage = wizardDialog.getCurrentPage();
				if (currentPage == wizard.getCatalogPage()) {
					((MarketplacePage) currentPage).showNextPage();
				}
			}

			private Shell activate(Shell shell) {
				Shell activeShell = shell.getDisplay().getActiveShell();
				if (activeShell != shell) {
					Shell[] childShells = shell.getShells();
					if (childShells.length == 0 || !Arrays.asList(childShells).contains(activeShell)) {
						shell.forceActive();
						shell.forceFocus();
					}
				}
				if (activeShell == null) {
					activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
					if (activeShell == null) {
						activeShell = shell;
					}
				}
				return activeShell;
			}

			@Override
			public void postShutdown(IWorkbench workbench) {
			}
		};
		PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);
		newShell.addDisposeListener(e -> PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener));

		if (newShell.getParent() == null) {
			//bug 500379 - root shells don't handle escape traversal by default
			newShell.addTraverseListener(e -> {
				if (e.keyCode == SWT.ESC) {
					Shell shell = (Shell) e.widget;
					if (shell != null && !shell.isDisposed() && shell.isVisible() && shell.isEnabled()) {
						shell.close();
					}
				}
			});
		}
	}

	@Override
	public MarketplaceWizard getWizard() {
		return (MarketplaceWizard) super.getWizard();
	}
}
