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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.operations.FeatureDescriptor;
import org.eclipse.epp.internal.mpc.ui.operations.ResolveFeatureNamesOperation;
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
					return entry.item.getName();
				}
			} else if (element instanceof FeatureDescriptorEntry) {
				FeatureDescriptorEntry entry = (FeatureDescriptorEntry) element;
				switch (columnIndex) {
				case 0:
					return entry.featureDescriptor.getName();
				}
			}
			return "" + columnIndex;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0 && element instanceof FeatureDescriptorEntry) {
				FeatureDescriptorEntry entry = (FeatureDescriptorEntry) element;
				switch (entry.parent.operation) {
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
				return ((List<CatalogItemEntry>) input).toArray();
			} else if (inputElement instanceof CatalogItemEntry) {
				return ((CatalogItemEntry) inputElement).children.toArray();
			}
			return new Object[0];
		}

		public boolean hasChildren(Object element) {
			return element == input || element instanceof CatalogItemEntry;
		}

		public Object getParent(Object element) {
			if (element instanceof FeatureDescriptorEntry) {
				return ((FeatureDescriptorEntry) element).parent;
			}
			return input;
		}

		public Object[] getChildren(Object parentElement) {
			return getElements(parentElement);
		}
	}

	private CheckboxTreeViewer viewer;

	private ResolveFeatureNamesOperation operation;

	private List<FeatureDescriptor> featureDescriptors;

	private Set<String> installedFeatures;

	private List<CatalogItemEntry> model;

	protected FeatureSelectionWizardPage() {
		super(FeatureSelectionWizardPage.class.getName());
		setTitle("Confirm Selected Features");
		setDescription("Confirm the features to include in this provisioning operation");
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
					s1 = ((CatalogItemEntry) e1).item.getName();
					s2 = ((CatalogItemEntry) e2).item.getName();
				} else {
					s1 = ((FeatureDescriptorEntry) e1).featureDescriptor.getName();
					s2 = ((FeatureDescriptorEntry) e2).featureDescriptor.getName();
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
		viewer.setInput(featureDescriptors);
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof CatalogItemEntry) {
					CatalogItemEntry entry = (CatalogItemEntry) event.getElement();
					for (FeatureDescriptorEntry child : entry.children) {
						child.checked = event.getChecked();
					}
				} else if (event.getElement() instanceof FeatureDescriptorEntry) {
					((FeatureDescriptorEntry) event.getElement()).checked = event.getChecked();
				}
				computeCheckedViewerState(model);
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
		featureDescriptors = null;
		viewer.setInput(null);
		operation = new ResolveFeatureNamesOperation(new ArrayList<CatalogItem>(getWizard().getItemToOperation()
				.keySet())) {

			Display display = getControl().getDisplay();

			@Override
			public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
				super.run(progressMonitor);
				final List<CatalogItemEntry> model = buildModel(getFeatureDescriptors());
				display.asyncExec(new Runnable() {
					public void run() {
						if (!getControl().isDisposed()) {
							updateFeatureDescriptors(getFeatureDescriptors(), getUnresolvedFeatureDescriptors(), model);
						}
					}
				});
			}
		};
		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException e) {
			IStatus status = MarketplaceClientUi.computeStatus(e, "Unexpected exception while verifying features");
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		} catch (InterruptedException e) {
			// canceled
		}
	}

	private void updateFeatureDescriptors(Set<FeatureDescriptor> featureDescriptors,
			Set<FeatureDescriptor> unresolvedFeatureDescriptors, List<CatalogItemEntry> model) {
		if (featureDescriptors != null) {
			this.model = model;
			viewer.setInput(model);
			computeCheckedViewerState(model);
			viewer.expandAll();
		}
		if (unresolvedFeatureDescriptors != null && !unresolvedFeatureDescriptors.isEmpty()) {
			if (featureDescriptors == null || featureDescriptors.isEmpty()) {
				IStatus status = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
						"None of the selected features are not available.  Please choose another solution to install.");
				StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			} else {
				IStatus status = new Status(IStatus.WARNING, MarketplaceClientUi.BUNDLE_ID,
						"One or more selected features are not available");
				StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			}
		}
	}

	public void computeCheckedViewerState(List<CatalogItemEntry> model) {
		// compute which ones should be checked. (update scenario where only part of a feature is installed)
		List<Object> checkedElements = new ArrayList<Object>();
		List<Object> grayCheckedElements = new ArrayList<Object>();
		for (CatalogItemEntry entry : model) {
			int childCheckCount = 0;
			for (FeatureDescriptorEntry child : entry.children) {
				if (child.checked) {
					checkedElements.add(child);
					++childCheckCount;
				}
			}
			if (childCheckCount == entry.children.size()) {
				checkedElements.add(entry);
			} else if (childCheckCount > 0) {
				grayCheckedElements.add(entry);
				checkedElements.add(entry);
			}
		}
		viewer.setCheckedElements(checkedElements.toArray());
		viewer.setGrayedElements(grayCheckedElements.toArray());
	}

	private static class CatalogItemEntry {
		CatalogItem item;

		Operation operation;

		List<FeatureDescriptorEntry> children;
	}

	private static class FeatureDescriptorEntry {

		CatalogItemEntry parent;

		FeatureDescriptor featureDescriptor;

		boolean checked;

		public FeatureDescriptorEntry(CatalogItemEntry entry, FeatureDescriptor descriptor) {
			parent = entry;
			featureDescriptor = descriptor;
		}
	}

	private List<CatalogItemEntry> buildModel(Set<FeatureDescriptor> featureDescriptors) {
		List<CatalogItemEntry> entries = new ArrayList<CatalogItemEntry>();

		for (Entry<CatalogItem, Operation> itemEntry : getWizard().getItemToOperation().entrySet()) {
			CatalogItemEntry entry = new CatalogItemEntry();
			entry.operation = itemEntry.getValue();
			entry.item = itemEntry.getKey();
			entry.children = new ArrayList<FeatureDescriptorEntry>();
			for (String featureId : entry.item.getInstallableUnits()) {
				for (FeatureDescriptor descriptor : featureDescriptors) {
					if (descriptor.getId().equals(featureId) || descriptor.getSimpleId().equals(featureId)) {
						FeatureDescriptorEntry featureEntry = new FeatureDescriptorEntry(entry, descriptor);
						entry.children.add(featureEntry);
						computeInitialChecked(featureEntry);
					}
				}
			}
			if (!entry.children.isEmpty()) {
				entries.add(entry);
			}
		}

		return entries;
	}

	private void computeInitialChecked(FeatureDescriptorEntry entry) {
		Operation operation = entry.parent.operation;
		if (operation == Operation.CHECK_FOR_UPDATES) {
			if (getInstalledFeatures().contains(entry.featureDescriptor.getId())
					|| getInstalledFeatures().contains(entry.featureDescriptor.getSimpleId())) {
				entry.checked = true;
			}
		} else {
			entry.checked = true;
		}
	}

	protected Set<String> getInstalledFeatures() {
		if (installedFeatures == null) {
			Set<String> features = new HashSet<String>();
			IBundleGroupProvider[] bundleGroupProviders = Platform.getBundleGroupProviders();
			for (IBundleGroupProvider provider : bundleGroupProviders) {
				IBundleGroup[] bundleGroups = provider.getBundleGroups();
				for (IBundleGroup group : bundleGroups) {
					features.add(group.getIdentifier());
				}
			}
			installedFeatures = features;
		}
		return installedFeatures;
	}

}
