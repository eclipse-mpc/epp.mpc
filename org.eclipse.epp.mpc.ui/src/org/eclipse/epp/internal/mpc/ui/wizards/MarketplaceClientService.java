/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, bug 432803: public API,
 *                       bug 461603: featured market
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.commands.MarketplaceWizardCommand;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceViewer.ContentType;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard.WizardState;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientService;
import org.eclipse.ui.statushandlers.StatusManager;

public class MarketplaceClientService implements IMarketplaceClientService {

	public IMarketplaceClientConfiguration newConfiguration() {
		return new MarketplaceCatalogConfiguration();
	}

	public void open(IMarketplaceClientConfiguration configuration) {
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setConfiguration(configuration);
		WizardState wizardState = new WizardState();
		setInitialContentType(configuration, wizardState);
		wizardState.setProceedWithInstallation(false);
		command.setWizardDialogState(wizardState);
		execute(command);
	}

	private void setInitialContentType(IMarketplaceClientConfiguration configuration, WizardState wizardState) {
		if (configuration instanceof MarketplaceCatalogConfiguration) {
			MarketplaceCatalogConfiguration catalogConfiguration = (MarketplaceCatalogConfiguration) configuration;
			ContentType initialContentType = catalogConfiguration.getInitialContentType();
			if (initialContentType != null) {
				wizardState.setContentType(initialContentType);
			}
		}
	}

	public void openSelected(IMarketplaceClientConfiguration configuration) {
		checkInitialState(configuration);
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setConfiguration(configuration);
		WizardState wizardState = new WizardState();
		wizardState.setContentType(ContentType.SELECTION);
		wizardState.setProceedWithInstallation(false);
		command.setWizardDialogState(wizardState);
		execute(command);
	}

	public void openInstalled(IMarketplaceClientConfiguration configuration) {
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setConfiguration(configuration);
		WizardState wizardState = new WizardState();
		wizardState.setContentType(ContentType.INSTALLED);
		wizardState.setProceedWithInstallation(false);
		command.setWizardDialogState(wizardState);
		execute(command);
	}

	public void openSearch(IMarketplaceClientConfiguration configuration, IMarket market, ICategory category,
			String query) {
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setConfiguration(configuration);
		WizardState wizardState = new WizardState();
		wizardState.setContentType(ContentType.SEARCH);
		wizardState.setFilterMarket(market);
		wizardState.setFilterCategory(category);
		wizardState.setFilterQuery(query);
		wizardState.setProceedWithInstallation(false);
		command.setWizardDialogState(wizardState);
		execute(command);
	}

	public void open(IMarketplaceClientConfiguration configuration, Set<INode> nodes) {
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setConfiguration(configuration);
		WizardState wizardState = new WizardState();
		setInitialContentType(configuration, wizardState);
		wizardState.setContent(nodes);
		wizardState.setProceedWithInstallation(false);
		command.setWizardDialogState(wizardState);
		execute(command);
	}

	public void openProvisioning(IMarketplaceClientConfiguration configuration) {
		checkInitialState(configuration);
		MarketplaceWizardCommand command = new MarketplaceWizardCommand();
		command.setConfiguration(configuration);
		WizardState wizardState = new WizardState();
		wizardState.setProceedWithInstallation(true);
		command.setWizardDialogState(wizardState);
		execute(command);
	}

	private void checkInitialState(IMarketplaceClientConfiguration configuration) {
		if (configuration.getInitialState() == null
				&& (configuration.getInitialOperations() == null || configuration.getInitialOperations().isEmpty())) {
			throw new IllegalArgumentException(Messages.MarketplaceClientService_noProvisioningOperation);
		}
	}

	private void execute(MarketplaceWizardCommand command) {
		try {
			command.execute(new ExecutionEvent());
		} catch (ExecutionException e) {
			MarketplaceClientUi.handle(
					MarketplaceClientUi.computeStatus(e, Messages.MarketplaceClientService_ExecuteError),
					StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		}
	}

}
