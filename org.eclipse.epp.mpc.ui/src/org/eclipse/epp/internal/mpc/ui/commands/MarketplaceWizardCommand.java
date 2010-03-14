/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.epp.internal.mpc.core.service.Category;
import org.eclipse.epp.internal.mpc.core.service.Market;
import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCategory;
import org.eclipse.epp.internal.mpc.ui.wizards.ComboTagFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceCatalogConfiguration;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceFilter;
import org.eclipse.epp.internal.mpc.ui.wizards.MarketplaceWizard;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogFilter;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * @author David Green
 */
@SuppressWarnings("restriction")
public class MarketplaceWizardCommand extends AbstractHandler implements IHandler {

	private List<CatalogDescriptor> catalogDescriptors;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final MarketplaceCatalog catalog = new MarketplaceCatalog();

		catalog.setEnvironment(DiscoveryCore.createEnvironment());
		catalog.setVerifyUpdateSiteAvailability(false);

		MarketplaceCatalogConfiguration configuration = new MarketplaceCatalogConfiguration();

		if (catalogDescriptors == null || catalogDescriptors.isEmpty()) {
			configuration.getCatalogDescriptors().addAll(CatalogRegistry.getInstance().getCatalogDescriptors());
		} else {
			configuration.getCatalogDescriptors().addAll(catalogDescriptors);
		}

		configuration.getFilters().clear();

		final ComboTagFilter marketFilter = new ComboTagFilter() {
			@Override
			public void catalogUpdated(boolean wasCancelled) {
				List<Tag> choices = new ArrayList<Tag>();
				for (CatalogCategory category : catalog.getCategories()) {
					if (category instanceof MarketplaceCategory) {
						MarketplaceCategory marketplaceCategory = (MarketplaceCategory) category;
						for (Market market : marketplaceCategory.getMarkets()) {
							Tag marketTag = new Tag(Market.class, market.getId(), market.getName());
							marketTag.setData(market);
							choices.add(marketTag);
						}
					}
				}
				setChoices(choices);
			}
		};
		marketFilter.setSelectAllOnNoSelection(true);
		marketFilter.setNoSelectionLabel(Messages.MarketplaceWizardCommand_allMarkets);
		marketFilter.setTagClassification(Category.class);
		marketFilter.setChoices(new ArrayList<Tag>());

		final ComboTagFilter marketCategoryTagFilter = new ComboTagFilter();
		marketCategoryTagFilter.setSelectAllOnNoSelection(true);
		marketCategoryTagFilter.setNoSelectionLabel(Messages.MarketplaceWizardCommand_allCategories);
		marketCategoryTagFilter.setTagClassification(Category.class);
		marketCategoryTagFilter.setChoices(new ArrayList<Tag>());
		marketFilter.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				List<Tag> choices = new ArrayList<Tag>();

				Set<Tag> categoryTags = marketFilter.getSelected();
				Set<Tag> newChoices = new HashSet<Tag>();
				for (Tag tag : categoryTags) {
					Market market = (Market) tag.getData();
					for (Category marketCategory : market.getCategory()) {
						Tag categoryTag = new Tag(Category.class, marketCategory.getId(), marketCategory.getName());
						categoryTag.setData(marketCategory);
						if (newChoices.add(categoryTag)) {
							choices.add(categoryTag);
						}
					}
				}

				marketCategoryTagFilter.setChoices(choices);
			}
		});

		configuration.getFilters().add(marketFilter);
		configuration.getFilters().add(marketCategoryTagFilter);

		for (CatalogFilter filter : configuration.getFilters()) {
			((MarketplaceFilter) filter).setCatalog(catalog);
		}

		DiscoveryWizard wizard = new MarketplaceWizard(catalog, configuration);

		wizard.setWindowTitle(Messages.MarketplaceWizardCommand_eclipseMarketplace);

		WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);
		dialog.open();
		return null;
	}

	public void setCatalogDescriptors(List<CatalogDescriptor> catalogDescriptors) {
		this.catalogDescriptors = catalogDescriptors;
	}
}
