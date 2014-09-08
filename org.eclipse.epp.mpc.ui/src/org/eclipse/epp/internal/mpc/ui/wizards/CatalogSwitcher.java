/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CategoryItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public class CatalogSwitcher extends Composite implements ISelectionProvider {

	private static final int ITEM_MARGIN = 5;

	private static final int MIN_SCROLL_HEIGHT = 32 + (2 * ITEM_MARGIN);

	private final MarketplaceCatalogConfiguration configuration;

	private final ImageRegistry imageRegistry = new ImageRegistry();

	private final List<ISelectionChangedListener> listeners = new LinkedList<ISelectionChangedListener>();

	private CatalogDescriptor selection;

	private Composite marketplaceArea;

	public CatalogSwitcher(Composite parent, int style, MarketplaceCatalogConfiguration configuration) {
		super(parent, style);
		this.configuration = configuration;
		GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(this);
		createContents(this);
	}

	private void createContents(final Composite parent) {
		createHeader(parent);
		final ScrolledComposite scrollArea = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrollArea.setLayout(new FillLayout());

		marketplaceArea = new Composite(scrollArea, SWT.NONE);

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.marginLeft = layout.marginRight = layout.marginTop = layout.marginBottom = layout.marginHeight = layout.marginWidth = 0;
		marketplaceArea.setLayout(layout);

		Color listBackground = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		setBackground(listBackground);
		marketplaceArea.setBackground(listBackground);
		scrollArea.setBackground(listBackground);

		List<CatalogDescriptor> catalogDescriptors = configuration.getCatalogDescriptors();
		for (CatalogDescriptor catalogDescriptor : catalogDescriptors) {
			createMarketplace(marketplaceArea, catalogDescriptor);
		}

		scrollArea.setContent(marketplaceArea);
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
		CategoryItem<CatalogCategory> header = new CategoryItem<CatalogCategory>(parent, SWT.NONE,
				new DiscoveryResources(parent.getDisplay()), fakeCategory);
		MarketplaceViewer.setSeparatorVisible(header, false);
		MarketplaceViewer.fixLayout(header);
	}

	private void createMarketplace(Composite composite, final CatalogDescriptor catalogDescriptor) {
		Composite container = new Composite(composite, SWT.NONE);
		Color listBackground = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		container.setBackground(listBackground);
		container.setData(catalogDescriptor);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = ITEM_MARGIN;
		layout.marginWidth = ITEM_MARGIN;
		container.setLayout(layout);

		final Label label = new Label(container, SWT.NONE);
		label.setBackground(listBackground);
		retrieveCatalogImage(catalogDescriptor, label);
		label.setImage(getDefaultCatalogImage());
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				selection = catalogDescriptor;
				refreshSelection();
				fireSelectionChanged();
			}
		});
		CatalogToolTip.attachCatalogToolTip(label, catalogDescriptor);
	}

	private void retrieveCatalogImage(final CatalogDescriptor catalogDescriptor, final Label label) {
		Job job = new Job(Messages.CatalogSwitcher_retrieveMetaData) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (label.isDisposed()) {
					return Status.OK_STATUS;
				}
				monitor.beginTask(
						NLS.bind(Messages.CatalogSwitcher_downloadCatalogImage, catalogDescriptor.getLabel()), 1);
				final Image image = getCatalogIcon(catalogDescriptor);
				monitor.worked(1);
				if (!label.isDisposed()) { // recheck - getCatalogIcon can take a bit if it needs to download the image...
					label.getDisplay().asyncExec(new Runnable() {

						public void run() {
							if (!label.isDisposed() && !image.isDisposed()) {
								label.setImage(image);
							}
						}
					});
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.DECORATE);
		job.schedule();
	}

	private void fireSelectionChanged() {
		for (ISelectionChangedListener listener : listeners) {
			SelectionChangedEvent event = new SelectionChangedEvent(this, new StructuredSelection(selection));
			listener.selectionChanged(event);
		}
	}

	private Image getCatalogIcon(final CatalogDescriptor catalogDescriptor) {
		String key = catalogDescriptor.getUrl().toExternalForm();
		Image image = imageRegistry.get(key);
		if (image == null) {
			ImageDescriptor catalogIcon = catalogDescriptor.getIcon();
			if (catalogIcon == null) {
				return getDefaultCatalogImage();
			}
			imageRegistry.put(key, catalogIcon);
			image = imageRegistry.get(key);
		}
		return image;
	}

	private Image getDefaultCatalogImage() {
		return MarketplaceClientUiPlugin.getInstance()
				.getImageRegistry()
				.get(MarketplaceClientUiPlugin.NO_ICON_PROVIDED_CATALOG);
	}

	@Override
	public void dispose() {
		imageRegistry.dispose();
		super.dispose();
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	public ISelection getSelection() {
		return new StructuredSelection(selection);
	}

	public void setSelection(ISelection newSelection) {
		if (newSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) newSelection;
			this.selection = (CatalogDescriptor) structuredSelection.getFirstElement();
			refreshSelection();
		}
	}

	private void refreshSelection() {
		Control[] children = marketplaceArea.getChildren();
		for (Control control : children) {
			int color;
			if (this.selection == control.getData()) {
				color = SWT.COLOR_LIST_SELECTION;
			} else {
				color = SWT.COLOR_WHITE;
			}
			control.setBackground(getDisplay().getSystemColor(color));
			((Composite) control).getChildren()[0].setBackground(getDisplay().getSystemColor(color));
		}
	}

	public int getPreferredHeight() {
		return MIN_SCROLL_HEIGHT + (2 * getBorderWidth()) + 6;
	}
}
