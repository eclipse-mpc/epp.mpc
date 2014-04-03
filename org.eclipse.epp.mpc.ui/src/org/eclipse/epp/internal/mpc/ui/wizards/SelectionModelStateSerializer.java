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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceCatalog;
import org.eclipse.epp.internal.mpc.ui.catalog.MarketplaceNodeCatalogItem;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.Operation;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * A mechanism for serializing/deserializing state
 *
 * @author David Green
 */
public class SelectionModelStateSerializer {

	private final SelectionModel selectionModel;

	private final MarketplaceCatalog catalog;

	public SelectionModelStateSerializer(MarketplaceCatalog catalog, SelectionModel selectionModel) {
		this.catalog = catalog;
		this.selectionModel = selectionModel;
	}

	public String serialize() {
		StringBuilder state = new StringBuilder(1024);
		for (Map.Entry<CatalogItem, Operation> entry : selectionModel.getItemToSelectedOperation().entrySet()) {
			if (entry.getValue() != Operation.NONE) {
				if (state.length() > 0) {
					state.append(' ');
				}
				INode data = (INode) entry.getKey().getData();
				state.append(data.getId());
				state.append('=');
				state.append(entry.getValue().name());
			}
		}
		return state.toString();
	}

	/**
	 * @param monitor
	 * @param state
	 *            the state to restore
	 * @param operationByNodeIdExtras
	 *            additional operations to include
	 * @deprecated use {@link #deserialize(String, Map, IProgressMonitor)} instead
	 */
	@Deprecated
	public void deserialize(IProgressMonitor monitor, String state,
			Map<String, org.eclipse.epp.internal.mpc.ui.wizards.Operation> operationByNodeIdExtras) {
		Map<String, Operation> operationByNodeId = new HashMap<String, Operation>();
		for (Entry<String, org.eclipse.epp.internal.mpc.ui.wizards.Operation> entry : operationByNodeIdExtras.entrySet()) {
			org.eclipse.epp.internal.mpc.ui.wizards.Operation op = entry.getValue();
			operationByNodeId.put(entry.getKey(), op == null ? null : op.getOperation());
		}
		deserialize(state, operationByNodeId, monitor);
	}

	/**
	 * @param state
	 *            the state to restore
	 * @param operationByNodeExtras
	 *            additional operations to include
	 * @param monitor
	 */
	public void deserialize(String state, Map<String, Operation> operationByNodeExtras, IProgressMonitor monitor) {

		Map<String, Operation> operationByNodeId = new HashMap<String, Operation>();
		if (state != null && state.length() > 0) {
			Pattern pattern = Pattern.compile("([^\\s=]+)=(\\S+)"); //$NON-NLS-1$
			Matcher matcher = pattern.matcher(state);
			while (matcher.find()) {
				String nodeId = matcher.group(1);
				String operationName = matcher.group(2);

				Operation operation = Operation.valueOf(operationName);

				operationByNodeId.put(nodeId, operation);
			}
		}
		if (operationByNodeExtras != null) {
			operationByNodeId.putAll(operationByNodeExtras);
		}
		if (!operationByNodeId.isEmpty()) {
			catalog.performQuery(monitor, operationByNodeId.keySet());
			for (CatalogItem item : catalog.getItems()) {
				if (item instanceof MarketplaceNodeCatalogItem) {
					MarketplaceNodeCatalogItem nodeItem = (MarketplaceNodeCatalogItem) item;
					Operation operation = operationByNodeId.get(nodeItem.getData().getId());
					if (operation != null && operation != Operation.NONE) {
						if (nodeItem.isInstalled() && operation == Operation.INSTALL) {
							operation = Operation.UPDATE;
						}
						selectionModel.select(nodeItem, operation);
					}
				}
			}
		}
	}

	/**
	 * @deprecated use {@link #deserialize(String, IProgressMonitor)} instead.
	 */
	@Deprecated
	public void deserialize(IProgressMonitor monitor, String state) {
		deserialize(state, monitor);
	}

	public void deserialize(String state, IProgressMonitor monitor) {
		deserialize(state, null, monitor);
	}
}
