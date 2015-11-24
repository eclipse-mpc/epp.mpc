/*******************************************************************************
 * Copyright (c) 2010, 2013 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.operations.FeatureDescriptor;
import org.eclipse.epp.internal.mpc.ui.operations.ResolveFeatureNamesOperation;
import org.eclipse.epp.internal.mpc.ui.util.Util;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.CatalogItemEntry;
import org.eclipse.epp.internal.mpc.ui.wizards.SelectionModel.FeatureEntry;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.dialogs.RemediationGroup;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

public class FeatureSelectionWizardPage extends WizardPage {

	private static class LabelProvider extends ColumnLabelProvider implements IStyledLabelProvider {

		public StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			String text = getText(element);
			if (element instanceof CatalogItemEntry) {
				styledString.append(text);
				CatalogItemEntry entry = (CatalogItemEntry) element;
				styledString.append("  " + entry.getItem().getSiteUrl(), StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			} else if (element instanceof FeatureEntry) {
				FeatureEntry entry = (FeatureEntry) element;
				if (entry.isOptional()) {
					styledString.append(text);
				} else {
					styledString.append(text, StyledString.QUALIFIER_STYLER);//TODO real "disabled" color
					styledString.append(Messages.FeatureSelectionWizardPage_required, StyledString.DECORATIONS_STYLER);
				}
				Operation operation = entry.computeChangeOperation();
				if (entry.getParent().getSelectedOperation() == Operation.CHANGE && operation != null) {
					switch (operation) {
					case UPDATE:
						styledString.append(Messages.FeatureSelectionWizardPage_Update_Pending, StyledString.COUNTER_STYLER);//TODO better highlight color
						break;
					case INSTALL:
						styledString.append(Messages.FeatureSelectionWizardPage_Install_Pending, StyledString.COUNTER_STYLER);
						break;
					case UNINSTALL:
						styledString.append(Messages.FeatureSelectionWizardPage_Uninstall_Pending, StyledString.COUNTER_STYLER);
						break;
					}
				}
			}
			return styledString;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof FeatureEntry) {
				FeatureEntry entry = (FeatureEntry) element;
				switch (entry.computeChangeOperation()) {
				case UPDATE:
					return MarketplaceClientUiPlugin.getInstance()
							.getImageRegistry()
							.get(MarketplaceClientUiPlugin.IU_ICON_UPDATE);
				case INSTALL:
					return MarketplaceClientUiPlugin.getInstance()
							.getImageRegistry()
							.get(MarketplaceClientUiPlugin.IU_ICON_INSTALL);
				case UNINSTALL:
					return MarketplaceClientUiPlugin.getInstance()
							.getImageRegistry()
							.get(MarketplaceClientUiPlugin.IU_ICON_UNINSTALL);
				case NONE:
					return MarketplaceClientUiPlugin.getInstance()
							.getImageRegistry()
							.get(MarketplaceClientUiPlugin.IU_ICON_DISABLED);
				default:
					return MarketplaceClientUiPlugin.getInstance()
							.getImageRegistry()
							.get(MarketplaceClientUiPlugin.IU_ICON);
				}
			} else if (element instanceof CatalogItemEntry) {
				return MarketplaceClientUiPlugin.getInstance()
						.getImageRegistry()
						.get(MarketplaceClientUiPlugin.IU_ICON);
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof CatalogItemEntry) {
				CatalogItemEntry entry = (CatalogItemEntry) element;
				return entry.getItem().getName();
			} else if (element instanceof FeatureEntry) {
				FeatureEntry entry = (FeatureEntry) element;
				return entry.getFeatureDescriptor().getName();
			}
			return element.toString();
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
				CatalogItemEntry itemEntry = (CatalogItemEntry) inputElement;
				List<FeatureEntry> children = itemEntry.getChildren();
				children = filterChildren(itemEntry.getSelectedOperation(), children);
				return children.toArray();
			}
			return new Object[0];
		}

		private List<FeatureEntry> filterChildren(Operation selectedOperation, List<FeatureEntry> children) {
			List<FeatureEntry> filtered;
			switch (selectedOperation) {
			case INSTALL:
				filtered = new ArrayList<SelectionModel.FeatureEntry>();
				for (FeatureEntry featureEntry : children) {
					if (!featureEntry.isInstalled() || featureEntry.hasUpdateAvailable()) {//FIXME show installed features as disabled
						filtered.add(featureEntry);
					}
				}
				return filtered;
			case UPDATE:
				filtered = new ArrayList<SelectionModel.FeatureEntry>();
				for (FeatureEntry featureEntry : children) {
					if (featureEntry.hasUpdateAvailable()) {
						filtered.add(featureEntry);
					}
				}
				return filtered;
			case UNINSTALL:
				filtered = new ArrayList<SelectionModel.FeatureEntry>();
				for (FeatureEntry featureEntry : children) {
					if (featureEntry.isInstalled()) {
						filtered.add(featureEntry);
					}
				}
				return filtered;
			case CHANGE:
				//show everything
				//$fall-through$
			default:
				return children;
			}
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

	private Text detailStatusText;

	private Group detailsControl;

	private TreeViewerColumn column;

	private Composite container;

	private RemediationGroup remediationGroup;

	private Composite defaultComposite;

	private StackLayout switchResultLayout;

	protected FeatureSelectionWizardPage() {
		super(FeatureSelectionWizardPage.class.getName());
		setTitle(Messages.FeatureSelectionWizardPage_confirmSelectedFeatures);
		setDescription(Messages.FeatureSelectionWizardPage_confirmSelectedFeatures_description);
	}

	@Override
	public MarketplaceWizard getWizard() {
		return (MarketplaceWizard) super.getWizard();
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		switchResultLayout = new StackLayout();
		container.setLayout(switchResultLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(data);
		defaultComposite = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(defaultComposite);

		Composite treeContainer = new Composite(defaultComposite, SWT.NULL);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(treeContainer);

		TreeColumnLayout columnLayout = new TreeColumnLayout();
		treeContainer.setLayout(columnLayout);

		viewer = new CheckboxTreeViewer(treeContainer, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL
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
					FeatureEntry fe1 = (FeatureEntry) e1;
					FeatureEntry fe2 = (FeatureEntry) e2;
					if (fe1.getInstallableUnitItem().isOptional() != fe2.getInstallableUnitItem().isOptional()) {
						return fe1.getInstallableUnitItem().isOptional() ? 1 : -1; //required items first
					}
					s1 = fe1.getFeatureDescriptor().getName();
					s2 = fe2.getFeatureDescriptor().getName();
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
		viewer.setContentProvider(new ContentProvider());
		viewer.setInput(getWizard().getSelectionModel());
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean checked = event.getChecked();
				if (event.getElement() instanceof CatalogItemEntry) {
					CatalogItemEntry entry = (CatalogItemEntry) event.getElement();
					for (FeatureEntry child : entry.getChildren()) {
						child.setChecked(checked);
					}
				} else if (event.getElement() instanceof FeatureEntry) {
					FeatureEntry featureEntry = (FeatureEntry) event.getElement();
					featureEntry.setChecked(checked);
				}
				refreshState();
			}
		});

		column = new TreeViewerColumn(viewer, SWT.NONE);
		column.setLabelProvider(new DelegatingStyledCellLabelProvider(new LabelProvider()));
		columnLayout.setColumnData(column.getColumn(), new ColumnWeightData(100, 100, true));

		detailsControl = new Group(defaultComposite, SWT.SHADOW_IN);
		detailsControl.setText(Messages.FeatureSelectionWizardPage_details);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 300).exclude(true).applyTo(detailsControl);
		GridLayoutFactory.fillDefaults().applyTo(detailsControl);
		detailStatusText = new Text(detailsControl, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(detailStatusText);

		setControl(container);
		Dialog.applyDialogFont(container);
		MarketplaceClientUi.setDefaultHelp(getControl());
		flipToDefaultComposite();
	}

	public RemediationGroup getRemediationGroup() {
		return remediationGroup;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			refreshState();
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {
					if (getControl() == null || getControl().isDisposed()
							|| getWizard().getContainer().getCurrentPage() != FeatureSelectionWizardPage.this) {
						return;
					}
					if (getWizard().wantInitializeInitialSelection()) {
						try {
							getWizard().initializeInitialSelection();
						} catch (CoreException e) {
							boolean wasCancelled = e.getStatus().getSeverity() == IStatus.CANCEL;
							if (!wasCancelled) {
								StatusManager.getManager().handle(e.getStatus(),
										StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
							}
						}
					}
					updateFeatures();
				}
			});
		}
	}

	private void refreshState() {
		computeCheckedViewerState();
		updateMessage();
		setPageComplete(computePageComplete());
	}

	private void updateFeatures() {
		SelectionModel selectionModel = getWizard().getSelectionModel();
		Set<CatalogItem> allSelectionCatalogItems = selectionModel.getItemToSelectedOperation().keySet();
		Set<CatalogItem> selectedCatalogItems = selectionModel.getSelectedCatalogItems();
		if (allSelectionCatalogItems.isEmpty() || selectedCatalogItems.isEmpty()) {
			showPreviousPage();
			return;
		}
		viewer.setInput(selectionModel);
		ResolveFeatureNamesOperation operation = new ResolveFeatureNamesOperation(new ArrayList<CatalogItem>(
allSelectionCatalogItems)) {

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
			// we only log here since any error will also be displayed when resolving the provisioning operation.
			int statusFlags = StatusManager.LOG;
			IStatus status;
			if (e.getCause() instanceof ProvisionException) {
				status = ((ProvisionException) e.getCause()).getStatus();
			} else {
				status = MarketplaceClientUi.computeStatus(e,
						Messages.FeatureSelectionWizardPage_unexpectedException_verifyingFeatures);
				statusFlags |= StatusManager.BLOCK | StatusManager.SHOW;
			}
			StatusManager.getManager().handle(status, statusFlags);
		} catch (InterruptedException e) {
			// canceled
		} finally {
			refresh();
			//bug 470485: need to recompute button state, because run() resets that to previous state
			refreshState();
		}
		//maybeUpdateProfileChangeOperation();
	}

	protected void showPreviousPage() {
		((MarketplaceWizardDialog) getWizard().getContainer()).backPressed();
	}

	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete() && getNextPage(false) != null;
	}

	public IWizardPage getNextPage(boolean computeChanges) {
		if (getWizard() == null) {
			return null;
		}
		return getWizard().getNextPage(this, computeChanges);
	}

	@Override
	public IWizardPage getNextPage() {
		return getNextPage(true);
	}

	void updateMessage() {
		ProfileChangeOperation profileChangeOperation = getWizard().getProfileChangeOperation();
		if (profileChangeOperation != null) {
			if (profileChangeOperation instanceof RemediationOperation
					&& ((RemediationOperation) profileChangeOperation).getResolutionResult() == Status.OK_STATUS) {
				if (remediationGroup == null) {
					remediationGroup = new RemediationGroup(this);
					remediationGroup.createRemediationControl(container);
				}
				setMessage(remediationGroup.getMessage(), IStatus.WARNING);
				remediationGroup.getDetailsGroup().setDetailText(getWizard().getErrorMessage());
				remediationGroup.update((RemediationOperation) profileChangeOperation);
				flipToRemediationComposite();
			} else {
				IStatus resolutionResult = profileChangeOperation.getResolutionResult();
				if (!resolutionResult.isOK()) {
					String message = resolutionResult.getMessage();
					if (resolutionResult.getSeverity() == IStatus.ERROR) {
						message = Messages.FeatureSelectionWizardPage_provisioningErrorAdvisory;
					} else if (resolutionResult.getSeverity() == IStatus.WARNING) {
						message = Messages.FeatureSelectionWizardPage_provisioningWarningAdvisory;
					}
					setMessage(message, Util.computeMessageType(resolutionResult));

					// avoid gratuitous scrolling
					String originalText = detailStatusText.getText();
					String newText;
					try {
						newText = profileChangeOperation.getResolutionDetails();
					} catch (Exception e) {
						// sometimes p2 might throw an exception
						MarketplaceClientUi.error(e);
						newText = Messages.FeatureSelectionWizardPage_detailsUnavailable;
					}
					if (newText != originalText || (newText != null && !newText.equals(originalText))) {
						detailStatusText.setText(newText);
					}
					((GridData) detailsControl.getLayoutData()).exclude = false;
				} else {
					setMessage(null, IMessageProvider.NONE);
					((GridData) detailsControl.getLayoutData()).exclude = true;
				}
			}
		} else {
			setMessage(null, IMessageProvider.NONE);
			((GridData) detailsControl.getLayoutData()).exclude = true;
		}

		((Composite) getControl()).layout(true);
		defaultComposite.layout(true);
	}

	@Override
	public IWizardPage getPreviousPage() {
		if (switchResultLayout.topControl != defaultComposite) {
			return this;
		}
		return super.getPreviousPage();
	}

	private void updateFeatureDescriptors(Set<FeatureDescriptor> featureDescriptors,
			Set<FeatureDescriptor> unresolvedFeatureDescriptors) {
		if (featureDescriptors != null) {
			updateSelectionModel(featureDescriptors);
			refresh();
		}
		// we don't warn on unresolved feature descriptors since it'll come up when we
		// resolve the provisioning operation.
		boolean pageComplete = computePageComplete();
		if (pageComplete != isPageComplete()) {
			setPageComplete(pageComplete);
		}
	}

	private void refresh() {
		viewer.refresh();
		computeCheckedViewerState();
		viewer.expandAll();
	}

	private boolean computePageComplete() {
		return getWizard().getSelectionModel().computeProvisioningOperationViable();
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
			boolean childGrayed = false;
			for (FeatureEntry child : entry.getChildren()) {
				if (child.isChecked()) {
					checkedElements.add(child);
					++childCheckCount;
				}
			}
			if (childCheckCount > 0) {
				for (FeatureEntry child : entry.getChildren()) {
					if (!child.isChecked() && !child.getInstallableUnitItem().isOptional()) {
						child.setChecked(true);
						checkedElements.add(child);
						++childCheckCount;
					}
				}
			}
			for (FeatureEntry child : entry.getChildren()) {
				if (child.isGrayed()) {
					checkedElements.add(child);
					grayCheckedElements.add(child);
					childGrayed = true;
				}
			}
			if (childCheckCount == entry.getChildren().size()) {
				checkedElements.add(entry);
			} else if (childCheckCount > 0 || childGrayed) {
				grayCheckedElements.add(entry);
				checkedElements.add(entry);
			}
		}
		viewer.setCheckedElements(checkedElements.toArray());
		viewer.setGrayedElements(grayCheckedElements.toArray());
		viewer.refresh(true);
	}

//	private boolean isChecked(FeatureEntry featureEntry) {
//		Operation featureOperation = featureEntry.getOperation();
//		CatalogItemEntry parent = featureEntry.getParent();
//		if (parent.getSelectedOperation() == Operation.CHANGE) {
//			//For CHANGE, unchecked means UNINSTALL, checked means INSTALL
//			if (featureOperation == Operation.NONE) {
//				return featureEntry.isInstalled();
//			}
//			return featureOperation == Operation.INSTALL || featureOperation == Operation.UPDATE;
//		} else if (featureOperation != null && featureOperation != Operation.NONE) {
//			return true;
//		}
//		return false;
//	}

	@Override
	public void performHelp() {
		getControl().notifyListeners(SWT.Help, new Event());
	}

	public void flipToRemediationComposite() {
		switchResultLayout.topControl = remediationGroup.getComposite();
		container.layout();
	}

	public void flipToDefaultComposite() {
		switchResultLayout.topControl = defaultComposite;
		container.layout();
	}

	public boolean isInRemediationMode() {
		return switchResultLayout.topControl != defaultComposite;
	}
}
