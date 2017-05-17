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
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Shell;

public class ImportFavoritesWizardDialog extends AbstractMarketplaceWizardDialog {

	public ImportFavoritesWizardDialog(Shell parentShell, ImportFavoritesWizard newWizard) {
		super(parentShell, newWizard);
	}

	@Override
	protected ImportFavoritesWizard getWizard() {
		return (ImportFavoritesWizard) super.getWizard();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setData(this);//make jface dialog accessible for swtbot
		new MarketplaceDropAdapter() {
			@Override
			protected void proceedFavorites(String url) {
				ImportFavoritesWizard wizard = getWizard();
				wizard.setInitialFavoritesUrl(url);
				ImportFavoritesPage importPage = wizard.getImportFavoritesPage();
				importPage.setFavoritesUrl(url);
			}
		}.installDropTarget(newShell);
	}

	@Override
	public String getFinishButtonLabel(IWizardPage page) {
		return Messages.ImportFavoritesWizardDialog_FinishButtonLabel;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		boolean relative = getParentShell() != null;
		IDialogSettings settings = getDialogBoundsSettings(getClass(), relative, false);
		if (settings == null) {
			//This tries to be smart in intializing its defaults:
			//- if there are settings for the opposite mode (!relative), take the size from there
			//- initialize the relative position to be slightly to the bottom left
			//- if there are settings for the main Marketplace wizard dialog, take the initial position
			//  from there for absolute position
			//- if we don't have an initial size yet, take the Marketplace wizard size, which should be
			//  a good match
			settings = getDialogBoundsSettings(getClass(), relative, true);
			if (relative) {
				setInitialLocation(80, 80, settings);
			}
			IDialogSettings marketplaceWizardSettings = getDialogBoundsSettings(MarketplaceWizardDialog.class, false,
					false);
			if (marketplaceWizardSettings != null) {
				if (!relative) {
					copyInitialLocation(marketplaceWizardSettings, settings);
				}
				IDialogSettings companionSettings = getDialogBoundsSettings(getClass(), !relative, false);
				//If we had companion settings, the call to getDialogBoundsSettings(..., true) would have
				//already initialized them
				if (companionSettings == null) {
					copyInitialSize(marketplaceWizardSettings, settings);
				}
			}
		}
		return settings;
	}
}
