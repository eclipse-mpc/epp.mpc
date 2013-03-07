/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/

package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.List;

import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * A tag filter that presents choices as a combo box. This filter can accomodate changes to the {@link #getChoices()
 * choices} after initialization.
 * 
 * @author David Green
 */
public class ComboTagFilter extends AbstractTagFilter {

	private Combo combo;

	private String noSelectionLabel;

	private SelectionListener listener;

	@Override
	public void createControl(Composite parent) {
		if (getChoices() == null) {
			throw new IllegalStateException();
		}
		combo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		listener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int selectionIndex = combo.getSelectionIndex();
				getSelected().clear();
				if (selectionIndex > 0) {
					Tag tag = getChoices().get(selectionIndex - 1);
					getSelected().add(tag);
				}
				selectionUpdated();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};
		combo.addSelectionListener(listener);
		rebuildChoicesUi();
	}

	protected void rebuildChoicesUi() {
		if (combo != null) {
			combo.removeSelectionListener(listener);
			combo.removeAll();
			combo.add(noSelectionLabel == null ? "" : noSelectionLabel); //$NON-NLS-1$
			if (getChoices() != null) {
				for (Tag tag : getChoices()) {
					combo.add(tag.getLabel());
				}
			}
			combo.select(0);
			combo.addSelectionListener(listener);
		}
	}

	@Override
	protected void choicesChanged(List<Tag> choices, List<Tag> previousChoices) {
		rebuildChoicesUi();
		super.choicesChanged(choices, previousChoices);
	}

	/**
	 * The label for the element in the list that represents the empty selection
	 */
	public String getNoSelectionLabel() {
		return noSelectionLabel;
	}

	/**
	 * The label for the element in the list that represents the empty selection
	 */
	public void setNoSelectionLabel(String noSelectionLabel) {
		this.noSelectionLabel = noSelectionLabel;
	}

	@Override
	protected void updateUi() {
		int index = -1;
		if (!getSelected().isEmpty()) {
			Tag selected = getSelected().iterator().next();
			index = getChoices().indexOf(selected);
		}
		combo.select(index + 1);//offset+1 for "All Markets" entry
		super.updateUi();
	}
}
