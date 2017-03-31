/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeInstallableUnitItem;
import org.eclipse.epp.internal.mpc.ui.operations.FeatureDescriptor;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.osgi.util.NLS;

/**
 * A model of selected items in the catalog. Provides feature-level selection fidelity, and stores the selected
 * operation.
 *
 * @author David Green
 */
public class SelectionModel {

	private final Map<CatalogItem, Operation> itemToOperation = new HashMap<CatalogItem, Operation>();

	private List<CatalogItemEntry> entries;

	private final InstallProfile installProfile;

	public SelectionModel(InstallProfile installProfile) {
		this.installProfile = installProfile;
	}

	/**
	 * Select the given item with the given operation.
	 *
	 * @param item
	 *            the item to select
	 * @param operation
	 *            the operation to perform. Providing {@link Operation#NONE} removes the selection
	 * @deprecated use {@link #select(CatalogItem, Operation)} instead
	 */
	@Deprecated
	public void select(CatalogItem item, org.eclipse.epp.internal.mpc.ui.wizards.Operation operation) {
		select(item, operation == null ? null : operation.getOperation());
	}

	/**
	 * Select the given item with the given operation.
	 *
	 * @param item
	 *            the item to select
	 * @param operation
	 *            the operation to perform. Providing {@link Operation#NONE} removes the selection
	 */
	public boolean select(CatalogItem item, Operation operation) {
		boolean changed = false;
		Operation sanitizedOperation = operation;
		if (operation != null && Operation.NONE != operation && item instanceof MarketplaceNodeCatalogItem) {
			MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) item;
			List<Operation> availableOperations = nodeItem.getAvailableOperations();
			if (!availableOperations.contains(operation)) {
				sanitizedOperation = null;
				switch (operation) {
				case INSTALL:
					if (availableOperations.contains(Operation.UPDATE)) {
						sanitizedOperation = Operation.UPDATE;
					}
					break;
				case UPDATE:
					if (availableOperations.contains(Operation.INSTALL)) {
						sanitizedOperation = Operation.UPDATE;
					}
					break;
				}
			}
		}
		if (sanitizedOperation == null || Operation.NONE == sanitizedOperation) {
			if (itemToOperation.remove(item) != Operation.NONE) {
				changed = true;
			}
			if (entries != null) {
				Iterator<CatalogItemEntry> it = entries.iterator();
				while (it.hasNext()) {
					CatalogItemEntry entry = it.next();
					if (entry.getItem().equals(item)) {
						it.remove();
					}
				}
			}
		} else {
			Operation previous = itemToOperation.put(item, sanitizedOperation);
			if (previous != sanitizedOperation) {
				changed = true;
				if (entries != null) {
					Iterator<CatalogItemEntry> it = entries.iterator();
					while (it.hasNext()) {
						CatalogItemEntry entry = it.next();
						if (entry.getItem().equals(item)) {
							it.remove();
						}
					}
					CatalogItemEntry itemEntry = createItemEntry(item, sanitizedOperation);
					entries.add(itemEntry);
				}
			}
		}
		if (changed) {
			selectionChanged();
		}
		return changed;
	}

	public List<CatalogItemEntry> getCatalogItemEntries() {
		if (entries == null) {
			List<CatalogItemEntry> entries = new ArrayList<CatalogItemEntry>();

			for (Entry<CatalogItem, Operation> entry : itemToOperation.entrySet()) {
				CatalogItem item = entry.getKey();
				Operation operation = entry.getValue();
				CatalogItemEntry itemEntry = createItemEntry(item, operation);
				entries.add(itemEntry);
			}

			this.entries = entries;
		}
		return entries;
	}

	/**
	 * @deprecated use {@link #createItemEntry(CatalogItem, Operation)} instead
	 */
	@Deprecated
	public CatalogItemEntry createItemEntry(CatalogItem item,
			org.eclipse.epp.internal.mpc.ui.wizards.Operation operation) {
		return createItemEntry(item, operation == null ? null : operation.getOperation());
	}

	public CatalogItemEntry createItemEntry(CatalogItem item, Operation operation) {
		CatalogItemEntry itemEntry = new CatalogItemEntry(item, operation);
		computeChildren(itemEntry);
		return itemEntry;
	}

	private void computeChildren(CatalogItemEntry itemEntry) {
		List<FeatureEntry> children = new ArrayList<FeatureEntry>();
		List<MarketplaceNodeInstallableUnitItem> iuItems = ((MarketplaceNodeCatalogItem) itemEntry.getItem()).getInstallableUnitItems();
		if (iuItems != null) {
			for (MarketplaceNodeInstallableUnitItem iuItem : iuItems) {
				FeatureEntry featureEntry = new FeatureEntry(itemEntry, iuItem);
				featureEntry.setInstalled(computeInstalled(featureEntry));
				featureEntry.setChecked(computeInitiallyChecked(featureEntry));
				children.add(featureEntry);
			}
		}
		itemEntry.children = children;
	}

	private Boolean computeInitiallyChecked(FeatureEntry featureEntry) {
		CatalogItemEntry parent = featureEntry.getParent();
		Operation selectedOperation = parent.getSelectedOperation();
		switch (selectedOperation) {
		case INSTALL:
			if (!featureEntry.isInstalled()) {
				return featureEntry.isRequiredInstall() || featureEntry.getInstallableUnitItem().isDefaultSelected();
			}
			return featureEntry.hasUpdateAvailable();
		case UNINSTALL:
			return featureEntry.isInstalled();
		case UPDATE:
			return featureEntry.hasUpdateAvailable() || featureEntry.isRequiredInstall();
		case CHANGE:
			return featureEntry.isInstalled();
		}
		return false;
	}

	private boolean computeInstalled(FeatureEntry entry) {
		Set<String> installedFeatures = installProfile.getInstalledFeatures();
		return installedFeatures.contains(entry.featureDescriptor.getId())
				|| installedFeatures.contains(entry.featureDescriptor.getSimpleId());
	}

	public static class CatalogItemEntry {
		private final CatalogItem item;

		private final Operation operation;

		private List<FeatureEntry> children;

		private CatalogItemEntry(CatalogItem item, Operation operation) {
			this.item = item;
			this.operation = operation;
		}

		public CatalogItem getItem() {
			return item;
		}

		/**
		 * @deprecated use {@link #getSelectedOperation()}
		 */
		@Deprecated
		public org.eclipse.epp.internal.mpc.ui.wizards.Operation getOperation() {
			return org.eclipse.epp.internal.mpc.ui.wizards.Operation.map(operation);
		}

		public Operation getSelectedOperation() {
			return operation;
		}

		public List<FeatureEntry> getChildren() {
			return children;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((item == null) ? 0 : item.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CatalogItemEntry other = (CatalogItemEntry) obj;
			if (item == null) {
				if (other.item != null) {
					return false;
				}
			} else if (!item.equals(other.item)) {
				return false;
			}
			return true;
		}

	}

	public class FeatureEntry {

		private final CatalogItemEntry parent;

		private final MarketplaceNodeInstallableUnitItem installableUnitItem;

		private FeatureDescriptor featureDescriptor;

		private Boolean checked;

		private FeatureEntry(CatalogItemEntry parent, MarketplaceNodeInstallableUnitItem installableUnitItem) {
			this(parent, installableUnitItem, new FeatureDescriptor(installableUnitItem.getId()));
		}

		private FeatureEntry(CatalogItemEntry parent, MarketplaceNodeInstallableUnitItem installableUnitItem,
				FeatureDescriptor featureDescriptor) {
			super();
			this.parent = parent;
			this.installableUnitItem = installableUnitItem;
			this.featureDescriptor = featureDescriptor;
		}

		public FeatureDescriptor getFeatureDescriptor() {
			return featureDescriptor;
		}

		public void setFeatureDescriptor(FeatureDescriptor featureDescriptor) {
			if (featureDescriptor != null && this.featureDescriptor != null
					&& !this.featureDescriptor.getId().equals(featureDescriptor.getId())) {
				throw new IllegalStateException();
			}
			this.featureDescriptor = featureDescriptor;
		}

		public MarketplaceNodeInstallableUnitItem getInstallableUnitItem() {
			return installableUnitItem;
		}

		public boolean isOptional() {
			return getInstallableUnitItem().isOptional();
		}

		public boolean isInstalled() {
			return Boolean.TRUE.equals(getInstallableUnitItem().getInstalled());
		}

		public void setInstalled(boolean installed) {
			getInstallableUnitItem().setInstalled(installed);
		}

		public boolean hasUpdateAvailable() {
			return isInstalled() && Boolean.TRUE.equals(getInstallableUnitItem().getUpdateAvailable());
		}

		public boolean isRequiredInstall() {
			return !isInstalled() && !getInstallableUnitItem().isOptional();
		}

		public CatalogItemEntry getParent() {
			return parent;
		}

		public void setChecked(Boolean checked) {
			this.checked = checked;
		}

		public boolean isChecked() {
			return Boolean.TRUE.equals(this.checked);
		}

		public boolean isGrayed() {
			return this.checked == null;
		}

		public void setGrayed() {
			setChecked(null);
		}

		public Operation computeChangeOperation() {
			return isGrayed() ? Operation.NONE : computeChangeOperation(isChecked());
		}

		public Operation computeChangeOperation(boolean checked) {
			CatalogItemEntry parent = getParent();
			switch (parent.getSelectedOperation()) {
			case INSTALL:
			case UPDATE:
				if (checked) {
					if (hasUpdateAvailable()) {
						return Operation.UPDATE;
					} else if (isRequiredInstall() && !isInstalled()) {
						return Operation.INSTALL;
					}
					if (!isInstalled()) {
						return Operation.INSTALL;
					}
				}
				return Operation.NONE;
			case UNINSTALL:
				if (checked && isInstalled()) {
					return Operation.UNINSTALL;
				}
				return Operation.NONE;
			case CHANGE:
				if (checked) {
					if (isInstalled()) {
						if (hasUpdateAvailable()) {
							return Operation.UPDATE;
						}
						return Operation.NONE;
					} else {
						return Operation.INSTALL;
					}
				} else {
					if (isInstalled()) {
						return Operation.UNINSTALL;
					} else {
						return Operation.NONE;
					}
				}
			default:
				return Operation.NONE;
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((featureDescriptor == null) ? 0 : featureDescriptor.hashCode());
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			FeatureEntry other = (FeatureEntry) obj;
			if (featureDescriptor == null) {
				if (other.featureDescriptor != null) {
					return false;
				}
			} else if (!featureDescriptor.equals(other.featureDescriptor)) {
				return false;
			}
			if (parent == null) {
				if (other.parent != null) {
					return false;
				}
			} else if (!parent.equals(other.parent)) {
				return false;
			}
			return true;
		}

	}

	/**
	 * @deprecated use {@link #getItemToSelectedOperation()} instead
	 */
	@Deprecated
	public Map<CatalogItem, org.eclipse.epp.internal.mpc.ui.wizards.Operation> getItemToOperation() {
		Map<CatalogItem, org.eclipse.epp.internal.mpc.ui.wizards.Operation> itemToOperation = new HashMap<CatalogItem, org.eclipse.epp.internal.mpc.ui.wizards.Operation>();
		Set<Entry<CatalogItem, Operation>> entrySet = this.itemToOperation.entrySet();
		for (Entry<CatalogItem, Operation> entry : entrySet) {
			itemToOperation.put(entry.getKey(), org.eclipse.epp.internal.mpc.ui.wizards.Operation.map(entry.getValue()));
		}
		return itemToOperation;
	}

	public Map<CatalogItem, Operation> getItemToSelectedOperation() {
		return Collections.unmodifiableMap(itemToOperation);
	}

	public Map<String, Operation> getItemIdToSelectedOperation() {
		Map<CatalogItem, Operation> itemToSelectedOperation = getItemToSelectedOperation();
		Map<String, Operation> itemIdToOperation = new HashMap<String, Operation>(itemToSelectedOperation.size());
		for (Entry<CatalogItem, Operation> entry : itemToSelectedOperation.entrySet()) {
			itemIdToOperation.put(entry.getKey().getId(), entry.getValue());
		}
		return itemIdToOperation;
	}

	public void selectionChanged() {
		// ignore

	}

	public Map<FeatureEntry, Operation> getFeatureEntryToOperation(boolean includeNone, boolean verify) {
		Map<FeatureEntry, Operation> featureEntries = new HashMap<FeatureEntry, Operation>();
		for (CatalogItemEntry entry : getCatalogItemEntries()) {
			for (FeatureEntry featureEntry : entry.getChildren()) {
				Operation operation = featureEntry.computeChangeOperation();
				if (operation != null && (includeNone || operation != Operation.NONE)) {
					Operation old = featureEntries.put(featureEntry, operation);
					if (old != null && old != Operation.NONE) {
						if (operation == Operation.NONE) {
							featureEntries.put(featureEntry, old);
							old = null;
						} else if (verify && !old.equals(operation)) {
							IStatus error = new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, NLS.bind(
									Messages.SelectionModel_Inconsistent_Actions, new Object[] {
											featureEntry.getFeatureDescriptor().getName(), old, operation }));
							throw new IllegalStateException(new CoreException(error));
						}
					}
				}
			}
		}
		return Collections.unmodifiableMap(featureEntries);
	}

	public Set<FeatureEntry> getSelectedFeatureEntries() {
		return getFeatureEntryToOperation(false, false).keySet();
	}

	/**
	 * @deprecated use {@link #getSelectedFeatureEntries()} instead
	 * @return the descriptors for all selected features
	 */
	@Deprecated
	public Set<FeatureDescriptor> getSelectedFeatureDescriptors() {
		Set<FeatureDescriptor> featureDescriptors = new HashSet<FeatureDescriptor>();
		Set<FeatureEntry> selectedFeatureEntries = getSelectedFeatureEntries();
		for (FeatureEntry featureEntry : selectedFeatureEntries) {
			featureDescriptors.add(featureEntry.getFeatureDescriptor());
		}
		return Collections.unmodifiableSet(featureDescriptors);
	}

	/**
	 * Get all catalog items that have at least one feature selected
	 */
	public Set<CatalogItem> getSelectedCatalogItems() {
		Set<CatalogItem> items = new HashSet<CatalogItem>();
		for (CatalogItemEntry entry : getCatalogItemEntries()) {
			if (entry.getSelectedOperation() == Operation.NONE) {
				continue;
			}
			for (FeatureEntry featureEntry : entry.getChildren()) {
				Operation operation = featureEntry.computeChangeOperation();
				if (operation != null && operation != Operation.NONE) {
					items.add(entry.item);
					break;
				}
			}
			if (entry.getSelectedOperation() == Operation.CHANGE) {
				items.add(entry.item);
			}
		}
		return Collections.unmodifiableSet(items);
	}

	/**
	 * @deprecated use {@link #getSelectedOperation(CatalogItem)} instead
	 */
	@Deprecated
	public org.eclipse.epp.internal.mpc.ui.wizards.Operation getOperation(CatalogItem item) {
		Operation operation = getSelectedOperation(item);
		return org.eclipse.epp.internal.mpc.ui.wizards.Operation.map(operation);
	}

	public Operation getSelectedOperation(CatalogItem item) {
		Operation operation = itemToOperation.get(item);
		return operation == null ? Operation.NONE : operation;
	}

	public boolean computeProvisioningOperationViableForFeatureSelection() {
		IStatus status = computeFeatureOperationViability();
		if (status == null) {
			//no operation
			//this is okay for a CHANGE
			Map<Operation, List<CatalogItem>> operationToItem = computeOperationToItem();
			if (operationToItem.size() == 1 && operationToItem.containsKey(Operation.CHANGE)) {
				return true;
			}
			return false;
		}
		return computeProvisioningOperationViable();
	}

	public boolean computeProvisioningOperationViable() {
		IStatus status = computeProvisioningOperationViability();
		if (status != null) {
			switch (status.getSeverity()) {
			case IStatus.INFO:
			case IStatus.OK:
			case IStatus.WARNING:
				return true;
			}
			return false;
		}
		return false;//no operations
	}

	/**
	 * Determine what message related to finishing the wizard should correspond to the current selection.
	 *
	 * @return the message, or null if there should be no message.
	 */
	public IStatus computeProvisioningOperationViability() {
		IStatus featureStatus = computeFeatureOperationViability();
		if (featureStatus == null || !featureStatus.isOK()) {
			return featureStatus;
		}

		Map<Operation, List<CatalogItem>> operationToItem = computeOperationToItem();

		if (operationToItem.size() == 0) {
			return new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, Messages.SelectionModel_Nothing_Selected);
		} else if (operationToItem.size() == 1) {
			Entry<Operation, List<CatalogItem>> entry = operationToItem.entrySet().iterator().next();
			return new Status(IStatus.INFO, MarketplaceClientUi.BUNDLE_ID,
					NLS.bind(
							Messages.SelectionModel_count_selectedFor_operation,
							entry.getValue().size() == 1 ? Messages.SelectionModel_oneSolution : NLS.bind(
									Messages.SelectionModel_countSolutions, entry.getValue().size()), entry.getKey()
									.getLabel()));
		} else if (operationToItem.size() == 2 && operationToItem.containsKey(Operation.INSTALL)
				&& operationToItem.containsKey(Operation.UPDATE)) {
			int count = 0;
			for (List<CatalogItem> items : operationToItem.values()) {
				count += items.size();
			}
			return new Status(IStatus.INFO, MarketplaceClientUi.BUNDLE_ID, NLS.bind(
					Messages.SelectionModel_countSolutionsSelectedForInstallUpdate, count));
		} else {
			return new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
					Messages.SelectionModel_cannotInstallRemoveConcurrently);
		}
	}

	private IStatus computeFeatureOperationViability() {
		Map<FeatureEntry, Operation> selectedFeatureEntries;
		try {
			selectedFeatureEntries = getFeatureEntryToOperation(false, true);
		} catch (IllegalStateException ex) {
			CoreException cause = (CoreException) ex.getCause();
			return cause.getStatus();
		}
		if (selectedFeatureEntries.isEmpty()) {
			return null;
		}
		return Status.OK_STATUS;
	}

	private Map<Operation, List<CatalogItem>> computeOperationToItem() {
		Map<CatalogItem, Operation> itemToOperation = getItemToSelectedOperation();
		Map<Operation, List<CatalogItem>> catalogItemByOperation = new HashMap<Operation, List<CatalogItem>>();
		for (Map.Entry<CatalogItem, Operation> entry : itemToOperation.entrySet()) {
			if (entry.getValue() == Operation.NONE) {
				continue;
			}
			List<CatalogItem> list = catalogItemByOperation.get(entry.getValue());
			if (list == null) {
				list = new ArrayList<CatalogItem>();
				catalogItemByOperation.put(entry.getValue(), list);
			}
			list.add(entry.getKey());
		}
		return catalogItemByOperation;
	}

	public void clear() {
		itemToOperation.clear();
		entries = null;
	}

}
