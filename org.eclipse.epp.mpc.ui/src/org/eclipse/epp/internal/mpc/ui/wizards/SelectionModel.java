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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
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
	public void select(CatalogItem item, Operation operation) {
		boolean changed = false;
		if (operation == null || Operation.NONE == operation) {
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
			Operation previous = itemToOperation.put(item, operation);
			if (previous != operation) {
				changed = true;
				if (entries != null) {
					Iterator<CatalogItemEntry> it = entries.iterator();
					while (it.hasNext()) {
						CatalogItemEntry entry = it.next();
						if (entry.getItem().equals(item)) {
							it.remove();
						}
					}
					CatalogItemEntry itemEntry = createItemEntry(item, operation);
					entries.add(itemEntry);
				}
			}
		}
		if (changed) {
			selectionChanged();
		}
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
		for (String iu : itemEntry.getItem().getInstallableUnits()) {
			FeatureEntry featureEntry = new FeatureEntry(itemEntry, new FeatureDescriptor(iu));
			featureEntry.setInstalled(computeInstalled(featureEntry));
			computeInitialChecked(featureEntry);
			children.add(featureEntry);
		}
		itemEntry.children = children;
	}

	private boolean computeInstalled(FeatureEntry entry) {
		Set<String> installedFeatures = installProfile.getInstalledFeatures();
		return installedFeatures.contains(entry.featureDescriptor.getId())
				|| installedFeatures.contains(entry.featureDescriptor.getSimpleId());
	}

	private void computeInitialChecked(FeatureEntry entry) {
		Operation operation = entry.parent.operation;
		if (operation == Operation.UPDATE) {
			if (entry.isInstalled()) {
				entry.checked = true;
			}
		} else {
			entry.checked = true;
		}
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

		private FeatureDescriptor featureDescriptor;

		private boolean checked;

		private boolean installed;

		private FeatureEntry(CatalogItemEntry parent, FeatureDescriptor featureDescriptor) {
			super();
			this.parent = parent;
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

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
			selectionChanged();
		}

		public boolean isInstalled() {
			return installed;
		}

		public void setInstalled(boolean installed) {
			this.installed = installed;
		}

		public CatalogItemEntry getParent() {
			return parent;
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

	public void selectionChanged() {
		// ignore

	}

	public Set<FeatureDescriptor> getSelectedFeatureDescriptors() {
		Set<FeatureDescriptor> featureDescriptors = new HashSet<FeatureDescriptor>();
		for (CatalogItemEntry entry : getCatalogItemEntries()) {
			for (FeatureEntry featureEntry : entry.getChildren()) {
				if (featureEntry.isChecked()) {
					featureDescriptors.add(featureEntry.getFeatureDescriptor());
				}
			}
		}
		return Collections.unmodifiableSet(featureDescriptors);
	}

	/**
	 * Get all catalog items that have at least one feature selected
	 */
	public Set<CatalogItem> getSelectedCatalogItems() {
		Set<CatalogItem> items = new HashSet<CatalogItem>();
		for (CatalogItemEntry entry : getCatalogItemEntries()) {
			for (FeatureEntry featureEntry : entry.getChildren()) {
				if (featureEntry.isChecked()) {
					items.add(entry.item);
				}
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

	public boolean computeProvisioningOperationViable() {
		if (getSelectedFeatureDescriptors().isEmpty()) {
			return false;
		}
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
		return true;
	}

	/**
	 * Determine what message related to finishing the wizard should correspond to the current selection.
	 *
	 * @return the message, or null if there should be no message.
	 */
	public IStatus computeProvisioningOperationViability() {
		Set<FeatureDescriptor> selectedFeatureDescriptors = getSelectedFeatureDescriptors();
		if (selectedFeatureDescriptors.isEmpty()) {
			return null;
		}
		Map<Operation, List<CatalogItem>> operationToItem = computeOperationToItem();

		if (operationToItem.size() == 1) {
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
		} else if (operationToItem.size() > 1) {
			if (!(operationToItem.size() == 2 && operationToItem.containsKey(Operation.INSTALL) && operationToItem.containsKey(Operation.UPDATE))) {
				return new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID,
						Messages.SelectionModel_cannotInstallRemoveConcurrently);
			}
		}
		return null;
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
