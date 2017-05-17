/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - category filtering (bug 314936), error handling (bug 374105),
 *                      multiselect hints (bug 337774), public API (bug 432803),
 *                      performance (bug 413871), featured market (bug 461603)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.wizards.AbstractTagFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.ComboTagFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceCatalogConfiguration;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard.WizardState;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizardDialog;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.IMarketplaceClientConfiguration;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogFilter;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class MarketplaceWizardCommand extends AbstractMarketplaceWizardCommand implements IHandler {

	private String wizardState;

	private Map<String, Operation> operations;

	private WizardState wizardDialogState;

	@Override
	protected MarketplaceCatalogConfiguration createConfiguration(final MarketplaceCatalog catalog,
			ExecutionEvent event) {
		MarketplaceCatalogConfiguration configuration = super.createConfiguration(catalog, event);
		configuration.getFilters().clear();

		final ComboTagFilter marketFilter = new ComboTagFilter() {
			@Override
			public void catalogUpdated(boolean wasCancelled) {
				List<Tag> choices = new ArrayList<Tag>();
				List<IMarket> markets = catalog.getMarkets();
				for (IMarket market : markets) {
					Tag marketTag = new Tag(IMarket.class, market.getId(), market.getName());
					marketTag.setData(market);
					choices.add(marketTag);
				}
				setChoices(choices);
			}
		};
		marketFilter.setSelectAllOnNoSelection(true);
		marketFilter.setNoSelectionLabel(Messages.MarketplaceWizardCommand_allMarkets);
		marketFilter.setTagClassification(ICategory.class);
		marketFilter.setChoices(new ArrayList<Tag>());

		final ComboTagFilter marketCategoryTagFilter = new ComboTagFilter() {
			@Override
			public void catalogUpdated(boolean wasCancelled) {
				updateCategoryChoices(this, marketFilter);
			}
		};
		marketCategoryTagFilter.setSelectAllOnNoSelection(true);
		marketCategoryTagFilter.setNoSelectionLabel(Messages.MarketplaceWizardCommand_allCategories);
		marketCategoryTagFilter.setTagClassification(ICategory.class);
		marketCategoryTagFilter.setChoices(new ArrayList<Tag>());

		final IPropertyChangeListener marketListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				final String property = event.getProperty();
				if (AbstractTagFilter.PROP_SELECTED.equals(property)) {
					updateCategoryChoices(marketCategoryTagFilter, marketFilter);
				}
			}
		};
		marketFilter.addPropertyChangeListener(marketListener);

		configuration.getFilters().add(marketFilter);
		configuration.getFilters().add(marketCategoryTagFilter);
		configuration.setInitialState(wizardState);
		if (operations != null && !operations.isEmpty()) {
			configuration.setInitialOperations(operations);
		}

		for (CatalogFilter filter : configuration.getFilters()) {
			((MarketplaceFilter) filter).setCatalog(catalog);
		}
		return configuration;
	}

	@Override
	protected MarketplaceWizardDialog createWizardDialog(DiscoveryWizard wizard, ExecutionEvent event) {
		Shell activeShell = HandlerUtil.getActiveShell(event);
		if (activeShell == null) {
			activeShell = WorkbenchUtil.getShell();
		}
		return new MarketplaceWizardDialog(activeShell, (MarketplaceWizard) wizard);
	}

	@Override
	protected MarketplaceWizard createWizard(final MarketplaceCatalog catalog,
			MarketplaceCatalogConfiguration configuration, ExecutionEvent event) {
		MarketplaceWizard wizard = new MarketplaceWizard(catalog, configuration);
		wizard.setInitialState(wizardDialogState);
		wizard.setWindowTitle(Messages.MarketplaceWizardCommand_eclipseMarketplace);
		return wizard;
	}

	private void updateCategoryChoices(final ComboTagFilter marketCategoryTagFilter, final ComboTagFilter marketFilter) {
		Set<Tag> newChoices = new HashSet<Tag>();
		List<Tag> choices = new ArrayList<Tag>();

		Set<IMarket> selectedMarkets = new HashSet<IMarket>();
		for (Tag marketTag : marketFilter.getSelected()) {
			selectedMarkets.add((IMarket) marketTag.getData());
		}

		final MarketplaceCatalog catalog = (MarketplaceCatalog) marketCategoryTagFilter.getCatalog();
		List<IMarket> markets = catalog.getMarkets();
		for (IMarket market : markets) {
			if (selectedMarkets.isEmpty() || selectedMarkets.contains(market)) {
				for (ICategory marketCategory : market.getCategory()) {
					Tag categoryTag = new Tag(ICategory.class, marketCategory.getId(), marketCategory.getName());
					categoryTag.setData(marketCategory);
					if (newChoices.add(categoryTag)) {
						choices.add(categoryTag);
					}
				}
			}
		}
		Collections.sort(choices, new Comparator<Tag>() {
			public int compare(Tag o1, Tag o2) {
				return o1.getLabel().compareTo(o2.getLabel());
			}
		});
		marketCategoryTagFilter.setChoices(choices);
	}

	public void setWizardState(String wizardState) {
		this.wizardState = wizardState;
	}

	public void setWizardDialogState(WizardState wizardState) {
		this.wizardDialogState = wizardState;
	}

	/**
	 * @deprecated use {@link #setOperations(Map)} instead
	 */
	@Deprecated
	public void setOperationByNodeId(Map<String, org.eclipse.epp.internal.mpc.ui.wizards.Operation> operationByNodeId) {
		this.operations = org.eclipse.epp.internal.mpc.ui.wizards.Operation.mapAllBack(operationByNodeId);
	}

	public void setOperations(Map<String, Operation> operationByNodeId) {
		this.operations = operationByNodeId;
	}

	public void setConfiguration(IMarketplaceClientConfiguration configuration) {
	   super.setConfiguration(configuration);
		setOperations(configuration.getInitialOperations());
		setWizardState((String) configuration.getInitialState());
	}

}
