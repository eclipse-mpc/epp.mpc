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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.operations.FeatureDescriptor;
import org.eclipse.epp.internal.mpc.ui.operations.ResolveFeatureNamesOperation;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.CatalogItemEntry;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.FeatureEntry;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

public class FeatureSelectionWizardPage extends WizardPage {

	private static class LabelProvider implements ILabelProvider, ITableLabelProvider {

		public void removeListener(ILabelProviderListener listener) {
			// nothing to do
		}

		public boolean isLabelProperty(Object element, String property) {
			return true;
		}

		public void dispose() {
			// nothing to do
		}

		public void addListener(ILabelProviderListener listener) {
			// nothing to do
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof CatalogItemEntry) {
				CatalogItemEntry entry = (CatalogItemEntry) element;
				if (columnIndex == 0) {
					return entry.getItem().getName();
				}
			} else if (element instanceof FeatureEntry) {
				FeatureEntry entry = (FeatureEntry) element;
				switch (columnIndex) {
				case 0:
					return entry.getFeatureDescriptor().getName();
				}
			}
			return "" + columnIndex; //$NON-NLS-1$
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0 && element instanceof FeatureEntry) {
				FeatureEntry entry = (FeatureEntry) element;
				switch (entry.getParent().getOperation()) {
				case CHECK_FOR_UPDATES:
					return MarketplaceClientUiPlugin.getInstance().getImageRegistry().get(
							MarketplaceClientUiPlugin.IU_ICON_UPDATE);
				case INSTALL:
					return MarketplaceClientUiPlugin.getInstance().getImageRegistry().get(
							MarketplaceClientUiPlugin.IU_ICON);
					// FIXME uninstall icon
				}
			}
			return null;
		}

		public Image getImage(Object element) {
			return getColumnImage(element, 0);
		}

		public String getText(Object element) {
			return getColumnText(element, 0);
		}
	}

	private static class ContentProvider implements ITreeContentProvider {

		private Object input;

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			input = newInput;
		}

		public void dispose() {
			// nothing to do
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement == input && input != null) {
				return ((SelectionModel) input).getCatalogItemEntries().toArray();
			} else if (inputElement instanceof CatalogItemEntry) {
				return ((CatalogItemEntry) inputElement).getChildren().toArray();
			}
			return new Object[0];
		}

		public boolean hasChildren(Object element) {
			return element == input || element instanceof CatalogItemEntry;
		}

		public Object getParent(Object element) {
			if (element instanceof FeatureEntry) {
				return ((FeatureEntry) element).getParent();
			}
			return input;
		}

		public Object[] getChildren(Object parentElement) {
			return getElements(parentElement);
		}
	}

	private CheckboxTreeViewer viewer;

	private ResolveFeatureNamesOperation operation;

	private List<CatalogItemEntry> model;

	protected FeatureSelectionWizardPage() {
		super(FeatureSelectionWizardPage.class.getName());
		setTitle(Messages.FeatureSelectionWizardPage_confirmSelectedFeatures);
		setDescription(Messages.FeatureSelectionWizardPage_confirmSelectedFeatures_description);
		setPageComplete(true);
	}

	@Override
	public MarketplaceWizard getWizard() {
		return (MarketplaceWizard) super.getWizard();
	}

	public void createControl(Composite parent) {
		viewer = new CheckboxTreeViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 == e2) {
					return 0;
				}
				String s1;
				String s2;
				if (e1 instanceof CatalogItemEntry) {
					s1 = ((CatalogItemEntry) e1).getItem().getName();
					s2 = ((CatalogItemEntry) e2).getItem().getName();
				} else {
					s1 = ((FeatureEntry) e1).getFeatureDescriptor().getName();
					s2 = ((FeatureEntry) e2).getFeatureDescriptor().getName();
				}
				int i = s1.compareToIgnoreCase(s2);
				if (i == 0) {
					i = s1.compareTo(s2);
					if (i == 0) {
						i = new Integer(System.identityHashCode(e1)).compareTo(System.identityHashCode(e2));
					}
				}
				return i;
			}
		});
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(new ContentProvider());
		viewer.setInput(getWizard().getSelectionModel());
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof CatalogItemEntry) {
					CatalogItemEntry entry = (CatalogItemEntry) event.getElement();
					for (FeatureEntry child : entry.getChildren()) {
						child.setChecked(event.getChecked());
					}
				} else if (event.getElement() instanceof FeatureEntry) {
					((FeatureEntry) event.getElement()).setChecked(event.getChecked());
				}
				computeCheckedViewerState();
				setPageComplete(getWizard().getSelectionModel().computeCanFinish());
			}
		});

		setControl(viewer.getControl());
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			updateFeatures();
		}
		super.setVisible(visible);
	}

	private void updateFeatures() {
		setPageComplete(false);
		viewer.setInput(getWizard().getSelectionModel());
		operation = new ResolveFeatureNamesOperation(new ArrayList<CatalogItem>(getWizard().getSelectionModel()
				.getItemToOperation()
				.keySet())) {

			Display display = getControl().getDisplay();

			@Override
			public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
				super.run(progressMonitor);
				display.asyncExec(new Runnable() {
					public void run() {
						if (!getControl().isDisposed()) {
							updateFeatureDescriptors(getFeatureDescriptors(), getUnresolvedFeatureDescriptors());
						}
					}
				});
			}
		};
		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException e) {
			IStatus status = MarketplaceClientUi.computeStatus(e, Messages.FeatureSelectionWizardPage_unexpectedException_verifyingFeatures);
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		} catch (InterruptedException e) {
			// canceled
		}
	}

	private void updateFeatureDescriptors(Set<FeatureDescriptor> featureDescriptors,
			Set<FeatureDescriptor> unresolvedFeatureDescriptors) {
		if (featureDescriptors != null) {
			updateSelectionModel(featureDescriptors);
			viewer.refresh();
			computeCheckedViewerState();
			viewer.expandAll();
		}
		if (unresolvedFeatureDescriptors != null && !unresolvedFeatureDescriptors.isEmpty()) {
			if (featureDescriptors == null || featureDescriptors.isEmpty()) {
				IStatus status = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
						Messages.FeatureSelectionWizardPage_noneAvailable);
				StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			} else {
				IStatus status = new Status(IStatus.WARNING, MarketplaceClientUi.BUNDLE_ID,
						Messages.FeatureSelectionWizardPage_oneOrMoreUnavailable);
				StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			}
		}
		setPageComplete(getWizard().getSelectionModel().computeCanFinish());
	}

	private void updateSelectionModel(Set<FeatureDescriptor> featureDescriptors) {
		Map<String, FeatureDescriptor> descriptorById = new HashMap<String, FeatureDescriptor>();
		for (FeatureDescriptor fd : featureDescriptors) {
			descriptorById.put(fd.getId(), fd);
		}
		SelectionModel selectionModel = getWizard().getSelectionModel();
		for (CatalogItemEntry entry : selectionModel.getCatalogItemEntries()) {
			for (FeatureEntry child : entry.getChildren()) {
				FeatureDescriptor descriptor = descriptorById.get(child.getFeatureDescriptor().getId());
				if (descriptor != null) {
					child.setFeatureDescriptor(descriptor);
				}
			}
		}
	}

	public void computeCheckedViewerState() {
		// compute which ones should be checked. (update scenario where only part of a feature is installed)
		List<Object> checkedElements = new ArrayList<Object>();
		List<Object> grayCheckedElements = new ArrayList<Object>();
		for (CatalogItemEntry entry : getWizard().getSelectionModel().getCatalogItemEntries()) {
			int childCheckCount = 0;
			for (FeatureEntry child : entry.getChildren()) {
				if (child.isChecked()) {
					checkedElements.add(child);
					++childCheckCount;
				}
			}
			if (childCheckCount == entry.getChildren().size()) {
				checkedElements.add(entry);
			} else if (childCheckCount > 0) {
				grayCheckedElements.add(entry);
				checkedElements.add(entry);
			}
		}
		viewer.setCheckedElements(checkedElements.toArray());
		viewer.setGrayedElements(grayCheckedElements.toArray());
	}

}
