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
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.operations.FeatureDescriptor;
import org.eclipse.epp.internal.mpc.ui.operations.ResolveFeatureNamesOperation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
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
			FeatureDescriptor descriptor = (FeatureDescriptor) element;
			switch (columnIndex) {
			case 0:
				return descriptor.getName();
			}
			return "" + columnIndex;
		}

		public Image getColumnImage(Object element, int columnIndex) {
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
				return ((List<FeatureDescriptor>) input).toArray();
			}
			return new Object[0];
		}

		public boolean hasChildren(Object element) {
			return !(element instanceof FeatureDescriptor);
		}

		public Object getParent(Object element) {
			return input;
		}

		public Object[] getChildren(Object parentElement) {
			return getElements(parentElement);
		}
	}

	private CheckboxTreeViewer viewer;

	private ResolveFeatureNamesOperation operation;

	private List<FeatureDescriptor> featureDescriptors;

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
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 == e2) {
					return 0;
				}
				FeatureDescriptor d1 = (FeatureDescriptor) e1;
				FeatureDescriptor d2 = (FeatureDescriptor) e2;
				int i = d1.getId().compareTo(d2.getId());
				return i;
			}
		});
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(new ContentProvider());
		viewer.setInput(featureDescriptors);

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
			IStatus status = MarketplaceClientUi.computeStatus(e, "Unexpected exception while verifying features");
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
		} catch (InterruptedException e) {
			// canceled
		}
	}

	private void updateFeatureDescriptors(Set<FeatureDescriptor> featureDescriptors,
			Set<FeatureDescriptor> unresolvedFeatureDescriptors) {
		if (featureDescriptors != null) {
			this.featureDescriptors = new ArrayList<FeatureDescriptor>(featureDescriptors);
			viewer.setInput(this.featureDescriptors);
			// FIXME: compute which ones should be checked. (update scenario where only part of a feature is installed)
			viewer.setCheckedElements(this.featureDescriptors.toArray());
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
}
