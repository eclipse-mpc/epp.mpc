/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.epp.internal.mpc.ui.catalog.FavoriteListCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.FavoritesDiscoveryStrategy;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

public class FavoritesViewer extends CatalogViewer {

	private MarketplaceDiscoveryResources discoveryResources;

	private Button installSelectedCheckbox;

	private PixelConverter pixelConverter;

	private boolean installSelected;

	private Button selectAllButton;

	private Button deselectAllButton;

	public FavoritesViewer(Catalog catalog, ImportFavoritesPage page, CatalogConfiguration configuration) {
		super(catalog, page, page.getWizard().getContainer(), configuration);
		setAutomaticFind(false);
		setRefreshJobDelay(50L);
	}

	@Override
	public void setFilterText(String newFilter) {
		super.setFilterText(newFilter);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		createViewerButtonBar((Composite) getControl());
	}

	protected void createViewerButtonBar(Composite parent) {
		Composite buttonBar = new Composite(parent, SWT.NONE);
		buttonBar.setFont(parent.getFont());

		pixelConverter = new PixelConverter(parent);
		GridLayoutFactory layoutFactory = GridLayoutFactory.swtDefaults()
				.numColumns(2)
				.equalWidth(false)
				.margins(pixelConverter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), 0)
				.spacing(pixelConverter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING), 0);
		layoutFactory.applyTo(buttonBar);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(buttonBar);
		createInstallCheckbox(buttonBar);

		Composite buttonContainer = new Composite(buttonBar, SWT.NONE);
		buttonContainer.setFont(parent.getFont());
		layoutFactory.margins(0, 0).equalWidth(true).numColumns(0).applyTo(buttonContainer);
		GridDataFactory.swtDefaults().align(SWT.END, SWT.FILL).grab(true, false).applyTo(buttonContainer);
		createButtonsForViewerButtonBar(buttonContainer);

		updateButtonState(getSelection());
	}

	private void createButtonsForViewerButtonBar(Composite buttonContainer) {
		selectAllButton = createButton(buttonContainer, IDialogConstants.SELECT_ALL_ID, Messages.FavoritesViewer_SelectAll);
		deselectAllButton = createButton(buttonContainer, IDialogConstants.DESELECT_ALL_ID, Messages.FavoritesViewer_DeselectAll);
		addSelectionChangedListener(event -> {
			IStructuredSelection selection = event.getStructuredSelection();
			updateButtonState(selection);
		});
	}

	private void updateButtonState(IStructuredSelection selection) {
		List<MarketplaceNodeCatalogItem> items = filterSelectableItems(getCatalog().getItems().iterator());
		List<MarketplaceNodeCatalogItem> selectedItems = filterSelectableItems(
				selection == null ? null : selection.iterator());
		installSelectedCheckbox.setEnabled(!items.isEmpty());
		if (items.isEmpty()) {
			selectAllButton.setEnabled(false);
			deselectAllButton.setEnabled(false);
		} else if (selectedItems.isEmpty()) {
			selectAllButton.setEnabled(true);
			deselectAllButton.setEnabled(false);
		} else {
			deselectAllButton.setEnabled(true);
			if (selectedItems.size() == items.size()) {
				selectAllButton.setEnabled(false);
			} else {
				selectAllButton.setEnabled(true);
			}
		}
	}

	private static List<MarketplaceNodeCatalogItem> filterSelectableItems(Iterator<?> items) {
		if (items == null || !items.hasNext()) {
			return Collections.emptyList();
		}
		ArrayList<MarketplaceNodeCatalogItem> selectableItems = null;
		while (items.hasNext()) {
			Object element = items.next();
			if (element instanceof MarketplaceNodeCatalogItem) {
				if (selectableItems == null) {
					selectableItems = new ArrayList<MarketplaceNodeCatalogItem>();
				}
				selectableItems.add((MarketplaceNodeCatalogItem) element);
			}
		}
		return selectableItems == null ? Collections.<MarketplaceNodeCatalogItem> emptyList() : selectableItems;
	}

	protected Button createButton(Composite parent, int id, String label) {
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.setFont(JFaceResources.getDialogFont());
		button.setData(Integer.valueOf(id));
		button.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonPressed(((Integer) e.widget.getData()).intValue());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore

			}
		});
		setButtonLayoutData(button);
		return button;
	}

	private void setButtonLayoutData(Button button) {
		AbstractMarketplaceDiscoveryItem.createButtonLayoutData(button, pixelConverter).applyTo(button);
	}

	protected void buttonPressed(int id) {
		if (id == IDialogConstants.SELECT_ALL_ID) {
			selectAll();
		} else if (id == IDialogConstants.DESELECT_ALL_ID) {
			deselectAll();
		}
	}

	private void deselectAll() {
		setSelection(StructuredSelection.EMPTY);
	}

	private void selectAll() {
		StructuredSelection all = new StructuredSelection(getCatalog().getItems());
		setSelection(all);
	}

	protected void createInstallCheckbox(Composite buttonContainer) {
		installSelectedCheckbox = new Button(buttonContainer, SWT.CHECK);
		installSelectedCheckbox.setText(Messages.FavoritesViewer_SelectForInstallation);
		installSelectedCheckbox.setToolTipText(
				Messages.FavoritesViewer_SelectForInstallationTooltip);
		GridDataFactory.defaultsFor(installSelectedCheckbox).applyTo(installSelectedCheckbox);

		installSelectedCheckbox.setSelection(this.installSelected);
		installSelectedCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				installSelected = ((Button) e.widget).getSelection();
			}
		});
	}

	private void updateInstallSelectedCheckbox() {
		if (installSelectedCheckbox != null && !installSelectedCheckbox.isDisposed()) {
			installSelectedCheckbox.setSelection(this.installSelected);
		}
	}

	@Override
	protected StructuredViewer doCreateViewer(Composite container) {
		StructuredViewer viewer = super.doCreateViewer(container);
		discoveryResources = new MarketplaceDiscoveryResources(container.getDisplay());
		viewer.getControl().addDisposeListener(e -> {
			discoveryResources.dispose();
			discoveryResources = null;
		});
		super.getResources().dispose();
		viewer.setComparator(null);
		return viewer;
	}

	private static <T extends Widget> T findControl(Composite container, Class<T> type) {
		Control[] children = container.getChildren();
		for (Control control : children) {
			if (type.isInstance(control)) {
				return type.cast(control);
			}
			if (control instanceof Composite) {
				T childMatch = findControl((Composite) control, type);
				if (childMatch != null) {
					return childMatch;
				}
			}
		}
		return null;
	}

	@Override
	protected void doCreateHeaderControls(Composite parent) {
		//header will have two items: the label and the search field
		Label searchLabel = findControl(parent, Label.class);
		final Text searchField = findControl(parent, Text.class);
		if (searchLabel != null) {
			searchLabel.setText(Messages.FavoritesViewer_searchLabel);
		}
		if (searchField != null) {
			searchField.setMessage(Messages.FavoritesViewer_searchInputDescription);
			searchField.addVerifyListener(e -> {
				if (e.keyCode == 0 && e.start == 0 && e.end == searchField.getText().length() && e.text.length() > 0) {
					filterTextChanged();
				}
			});
		}
	}

	@Override
	protected void modifySelection(CatalogItem connector, boolean selected) {
		//bug 517559 - make sure we don't add elements to the selection twice
		if (selected && connector != null) {
			List<CatalogItem> checkedItems = getCheckedItems();
			boolean alreadyChecked = checkedItems.contains(connector);
			if (alreadyChecked && ((!connector.isSelected() || !getSelection().toList().contains(connector)))) {
				//something is off - we recreate the selection state for the element completely...
				super.modifySelection(connector, false);
				super.modifySelection(connector, true);
			} // else skip - this wouldn't change anything...
		} else {
			super.modifySelection(connector, selected);
		}
	}

	@Override
	public void setSelection(IStructuredSelection selection) {
		//bug 517559 - make sure we don't add elements to the selection twice
		if (selection.isEmpty() || (getSelection().isEmpty() && getCheckedItems().isEmpty())) {
			super.setSelection(selection);
		} else {
			super.setSelection(StructuredSelection.EMPTY);
			super.setSelection(selection);
		}
	}

	@Override
	public void updateCatalog() {
		//Clear selection both here and in catalogUpdated()...
		//If something goes wrong, we won't reach catalogUpdated(),
		//so clear early. But do it there as well, since it can
		//be triggered from other pathes...
		setSelection(StructuredSelection.EMPTY);
		super.updateCatalog();
		updateButtonState(getSelection());
	}

	@Override
	protected MarketplaceDiscoveryResources getResources() {
		return discoveryResources;
	}

	@Override
	protected CatalogContentProvider doCreateContentProvider() {
		return new MarketplaceViewer.MarketplaceCatalogContentProvider();
	}

	@Override
	protected ControlListItem<?> doCreateViewerItem(Composite parent, Object element) {
		if (element instanceof MarketplaceNodeCatalogItem) {
			//marketplace entry
			FavoritesDiscoveryItem discoveryItem = createDiscoveryItem(parent, (MarketplaceNodeCatalogItem) element);
			return discoveryItem;
		} else if (element instanceof UserActionCatalogItem) {
			return new DiscoverFavoritesUserActionItem(parent, getResources(), (UserActionCatalogItem) element, this);
		} else if (element instanceof FavoriteListCatalogItem) {
			return new FavoriteListDiscoveryItem(parent, discoveryResources, (FavoriteListCatalogItem) element, this);
		}
		return super.doCreateViewerItem(parent, element);
	}

	private FavoritesDiscoveryItem createDiscoveryItem(Composite parent, MarketplaceNodeCatalogItem catalogItem) {
		return new FavoritesDiscoveryItem(parent, SWT.NONE, getResources(), catalogItem, this);
	}

	@Override
	protected Set<String> getInstalledFeatures(org.eclipse.core.runtime.IProgressMonitor monitor) {
		return Collections.emptySet();
	}

	@Override
	protected void doFind(final String text) {
		FavoritesDiscoveryStrategy favoritesStrategy = findFavoritesStrategy();
		if (favoritesStrategy != null) {
			favoritesStrategy.setFavoritesReference(text);
		}
		updateCatalog();
	}

	private FavoritesDiscoveryStrategy findFavoritesStrategy() {
		FavoritesDiscoveryStrategy favoritesStrategy = null;
		List<AbstractDiscoveryStrategy> discoveryStrategies = getCatalog().getDiscoveryStrategies();
		for (AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			if (strategy instanceof FavoritesDiscoveryStrategy) {
				favoritesStrategy = (FavoritesDiscoveryStrategy) strategy;
				break;
			}
		}
		return favoritesStrategy;
	}

	@Override
	protected void catalogUpdated(boolean wasCancelled, boolean wasError) {
		List<CatalogItem> items = getCatalog().getItems();
		Set<CatalogItem> selectedItems = new LinkedHashSet<CatalogItem>();
		for (CatalogItem catalogItem : items) {
			if (catalogItem.isSelected()) {
				selectedItems.add(catalogItem);
			}
		}
		setSelection(StructuredSelection.EMPTY);
		super.catalogUpdated(wasCancelled, wasError);
		setSelection(new StructuredSelection(new ArrayList<CatalogItem>(selectedItems)));
	}

	public void setInstallSelected(boolean install) {
		this.installSelected = install;
		updateInstallSelectedCheckbox();
	}

	public boolean isInstallSelected() {
		return installSelected;
	}

	public void setFavoritesUrl(String url) {
		setFilterText(url);
		filterTextChanged();
	}
}
