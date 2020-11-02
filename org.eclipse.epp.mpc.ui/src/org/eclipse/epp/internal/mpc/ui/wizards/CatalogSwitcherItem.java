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
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiResources;
import org.eclipse.epp.internal.mpc.ui.css.StyleHelper;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

public class CatalogSwitcherItem extends Composite {

	public static final int ITEM_MARGIN = 5;

	public static final String KEY_SELECTED = CatalogSwitcherItem.class.getName() + ":selected"; //$NON-NLS-1$

	private final ImageRegistry imageRegistry;

	private Label iconLabel;

	private ListenerList<SelectionListener> selectionListeners;

	private MouseListener selectionHandler;

	public CatalogSwitcherItem(Composite parent, ImageRegistry imageRegistry, CatalogDescriptor catalogDescriptor) {
		super(parent, SWT.NONE);
		this.setData(catalogDescriptor);
		this.imageRegistry = imageRegistry;

		createContent();
	}

	public CatalogDescriptor getCatalogDescriptor() {
		return (CatalogDescriptor) getData();
	}

	protected void createContent() {
		Composite container = this;
		container.setBackgroundMode(SWT.INHERIT_DEFAULT);

		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = ITEM_MARGIN;
		layout.marginWidth = ITEM_MARGIN;
		container.setLayout(layout);

		StyleHelper styleHelper = new StyleHelper().on(container);
		styleHelper.setClass("Catalog");

		iconLabel = new Label(container, SWT.NONE);
		//label.setBackground(container.getBackground());
		iconLabel.setImage(getDefaultCatalogImage());
		styleHelper.on(iconLabel).setClass("CatalogImage");

		CatalogDescriptor catalogDescriptor = getCatalogDescriptor();
		retrieveCatalogImage(catalogDescriptor, iconLabel);
		CatalogToolTip.attachCatalogToolTip(iconLabel, catalogDescriptor);
	}

	private void retrieveCatalogImage(final CatalogDescriptor catalogDescriptor, final Label label) {
		//TODO we could simplify all this using the ResourceManager and/or MarketplaceDiscoveryResources,
		// if the CatalogDescriptor had the image URL instead of an ImageDescriptor
		Job job = new Job(Messages.CatalogSwitcher_retrieveMetaData) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (label.isDisposed()) {
					return Status.OK_STATUS;
				}
				monitor.beginTask(NLS.bind(Messages.CatalogSwitcher_downloadCatalogImage, catalogDescriptor.getLabel()),
						1);
				final Image image = getCatalogIcon(catalogDescriptor);
				monitor.worked(1);
				if (image != null && !label.isDisposed()) { // recheck - getCatalogIcon can take a bit if it needs to download the image...
					label.getDisplay().asyncExec(() -> {
						if (!label.isDisposed() && !image.isDisposed()) {
							label.setImage(image);
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
			if (image == null) {
				return getDefaultCatalogImage();
			}
		}
		return image;
	}

	private Image getDefaultCatalogImage() {
		return MarketplaceClientUiResources.getInstance().getImageRegistry().get(
				MarketplaceClientUiResources.NO_ICON_PROVIDED_CATALOG);
	}

	public void setSelected(boolean selected) {
		setData(KEY_SELECTED, selected);
		setSelectedBackground(selected);
	}

	protected void setSelectedBackground(boolean selected) {
		Color color;
		if (selected) {
			color = getSelectedBackground();
		} else {
			color = getParent().getBackground();
		}
		setBackground(color);
		iconLabel.setBackground(color);
	}

	protected Color getSelectedBackground() {
		return getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
	}

	public boolean isSelected() {
		return Boolean.TRUE.equals(getData(KEY_SELECTED));
	}

	public void addSelectionListener(SelectionListener listener) {
		if (selectionListeners == null) {
			final ListenerList<SelectionListener> selectionListeners = new ListenerList<>();
			this.selectionListeners = selectionListeners;
			this.selectionHandler = MouseListener.mouseUpAdapter(e -> {
				Event untyped = new Event();
				untyped.widget = this;
				untyped.display = e.display;
				untyped.widget = e.widget;
				untyped.time = e.time;
				untyped.data = this.getData();
				untyped.item = e.widget;
				untyped.x = e.x;
				untyped.y = e.y;
				untyped.stateMask = e.stateMask;
				untyped.doit = true;
				SelectionEvent selectionEvent = new SelectionEvent(untyped);
				fireSelectionEvent(selectionEvent);
			});
			iconLabel.addMouseListener(selectionHandler);
		}
		selectionListeners.add(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		if (selectionListeners == null) {
			return;
		}
		selectionListeners.remove(listener);
		if (selectionListeners.isEmpty()) {
			if (selectionHandler != null) {
				iconLabel.removeMouseListener(selectionHandler);
				selectionHandler = null;
			}
			selectionListeners = null;
		}
	}

	protected void fireSelectionEvent(SelectionEvent selectionEvent) {
		ListenerList<SelectionListener> currentListeners = this.selectionListeners;
		if (currentListeners == null) {
			return;
		}
		for (SelectionListener listener : currentListeners) {
			listener.widgetSelected(selectionEvent);
		}
	}
}
