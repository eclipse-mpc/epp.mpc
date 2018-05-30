/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *  Yatta Solutions - bug 341014
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.epp.internal.mpc.ui.css.StyleHelper;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CategoryItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public class CatalogSwitcher extends Composite implements ISelectionProvider {

	private static final int ITEM_MARGIN = 5;

	private static final int MIN_SCROLL_HEIGHT = 32 + (2 * ITEM_MARGIN);

	private final MarketplaceCatalogConfiguration configuration;

	private final ImageRegistry imageRegistry = new ImageRegistry();

	private final List<ISelectionChangedListener> listeners = new LinkedList<>();

	private final List<CatalogSwitcherItem> items = new LinkedList<>();

	private CatalogDescriptor selection;

	private Composite marketplaceArea;

	public CatalogSwitcher(Composite parent, int style, MarketplaceCatalogConfiguration configuration) {
		super(parent, style);
		this.configuration = configuration;

		GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(this);
		this.setBackgroundMode(SWT.INHERIT_FORCE);
		Color listBackground = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		setBackground(listBackground);

		StyleHelper styleHelper = new StyleHelper().on(this);
		styleHelper.setClass("CatalogSwitcher");

		createContents(this);
	}

	private void createContents(final Composite parent) {
		createHeader(parent);
		final ScrolledComposite scrollArea = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrollArea.setBackgroundMode(SWT.INHERIT_DEFAULT);
		scrollArea.setLayout(new FillLayout());
		marketplaceArea = new Composite(scrollArea, SWT.NONE);
		marketplaceArea.setBackgroundMode(SWT.INHERIT_DEFAULT);
		scrollArea.setContent(marketplaceArea);

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.marginLeft = layout.marginRight = layout.marginTop = layout.marginBottom = layout.marginHeight = layout.marginWidth = 0;
		marketplaceArea.setLayout(layout);

		SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(c -> {
			Object data = c.data;
			if (data instanceof CatalogDescriptor) {
				CatalogDescriptor catalogDescriptor = (CatalogDescriptor) data;
				this.selection = catalogDescriptor;
				refreshSelection();
				fireSelectionChanged();
			}
		});
		items.clear();
		List<CatalogDescriptor> catalogDescriptors = configuration.getCatalogDescriptors();
		for (CatalogDescriptor catalogDescriptor : catalogDescriptors) {
			CatalogSwitcherItem item = createMarketplace(marketplaceArea, catalogDescriptor);
			item.addSelectionListener(selectionListener);
			items.add(item);
		}

		scrollArea.setExpandVertical(true);
		scrollArea.setExpandHorizontal(true);
		scrollArea.setMinHeight(MIN_SCROLL_HEIGHT);
		scrollArea.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Rectangle r = parent.getClientArea();
				int scrollBarWidth = scrollArea.getVerticalBar().getSize().x;
				scrollArea.setMinSize(marketplaceArea.computeSize(r.width - scrollBarWidth, SWT.DEFAULT));
			}
		});
	}

	private void createHeader(Composite parent) {
		CatalogCategory fakeCategory = new CatalogCategory();
		fakeCategory.setName(Messages.CatalogSwitcher_Header);
		CategoryItem<CatalogCategory> header = new CategoryItem<>(parent, SWT.NONE,
				new DiscoveryResources(parent.getDisplay()), fakeCategory);
		MarketplaceViewer.setSeparatorVisible(header, false);
		MarketplaceViewer.fixLayout(header);
		new StyleHelper().on(header).setClass("CatalogSwitcherHeader");
	}

	private CatalogSwitcherItem createMarketplace(Composite composite, final CatalogDescriptor catalogDescriptor) {
		CatalogSwitcherItem marketplaceItem = new CatalogSwitcherItem(composite, imageRegistry, catalogDescriptor);

		StyleHelper styleHelper = new StyleHelper().on(marketplaceItem);
		styleHelper.setId("catalog-" + composite.getChildren().length);

		return marketplaceItem;
	}

	private void fireSelectionChanged() {
		for (ISelectionChangedListener listener : listeners) {
			SelectionChangedEvent event = new SelectionChangedEvent(this, new StructuredSelection(selection));
			listener.selectionChanged(event);
		}
	}

	@Override
	public void dispose() {
		imageRegistry.dispose();
		super.dispose();
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public ISelection getSelection() {
		return new StructuredSelection(selection);
	}

	@Override
	public void setSelection(ISelection newSelection) {
		if (newSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) newSelection;
			CatalogDescriptor oldDescriptor = this.selection;
			CatalogDescriptor newDescriptor = (CatalogDescriptor) structuredSelection.getFirstElement();
			this.selection = newDescriptor;
			refreshSelection();
			if (newDescriptor != oldDescriptor) {
				fireSelectionChanged();
			}
		}
	}

	private void refreshSelection() {
		for (CatalogSwitcherItem item : items) {
			item.setSelected(this.selection != null && this.selection == item.getCatalogDescriptor());
		}
		new StyleHelper().on(this).applyStyles();
	}

	public int getPreferredHeight() {
		return MIN_SCROLL_HEIGHT + (2 * getBorderWidth()) + 6;
	}

	public List<CatalogSwitcherItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public CatalogSwitcherItem getSelectedItem() {
		if (this.selection == null) {
			return null;
		}
		for (CatalogSwitcherItem item : items) {
			if (this.selection == item.getCatalogDescriptor()) {
				return item;
			}
		}
		return null;
	}
}
