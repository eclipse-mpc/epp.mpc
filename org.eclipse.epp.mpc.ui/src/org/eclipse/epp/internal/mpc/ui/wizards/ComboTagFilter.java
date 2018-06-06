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
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 461603: featured market
 *******************************************************************************/

package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A tag filter that presents choices as a combo box. This filter can accomodate changes to the {@link #getChoices()
 * choices} after initialization.
 *
 * @author David Green
 */
public class ComboTagFilter extends AbstractTagFilter {

	private ComboWrapper combo;

	private String noSelectionLabel;

	private SelectionListener listener;

	@Override
	public void createControl(Composite parent) {
		if (getChoices() == null) {
			throw new IllegalStateException();
		}
		combo = useCCombo() ? new CComboComboWrapper(parent, SWT.READ_ONLY | SWT.DROP_DOWN)
				: new ComboComboWrapper(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		combo.setData(this);
		listener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectionIndex = combo.getSelectionIndex();
				if (selectionIndex > 0) {
					Tag tag = getChoices().get(selectionIndex - 1);
					setSelected(Collections.singleton(tag));
				} else {
					setSelected(Collections.<Tag> emptySet());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};
		combo.addSelectionListener(listener);
		rebuildChoicesUi();
	}

	/**
	 * Workaround for MPC bug 535037 / SWT bug 517590: Plain Combo has wrong width in GTK3 if one of its entries is
	 * wider than the combo itself. We could just replace the Combo with a CCombo globally, but until we can also get
	 * more control about the rendering of the search text field, this looks strange on the other platforms. So we just
	 * exchange it on Linux/GTK3 for now.
	 */
	private static boolean useCCombo() {
		return Platform.WS_GTK.equals(Platform.getWS());
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
		if (combo.isDisposed()) {
			return;
		}
		int index = -1;
		if (!getSelected().isEmpty()) {
			Tag selected = getSelected().iterator().next();
			index = getChoices().indexOf(selected);
		}
		combo.select(index + 1);//offset+1 for "All Markets" entry
		super.updateUi();
	}

	private static abstract class ComboWrapper {
		private final Control combo;

		public ComboWrapper(Composite parent, int style) {
			combo = createCombo(parent, style);
		}

		abstract Control createCombo(Composite parent, int style);

		public Control getCombo() {
			return combo;
		}

		public void setData(Object data) {
			combo.setData(data);
		}

		public boolean isDisposed() {
			return combo.isDisposed();
		}

		public abstract void select(int i);

		public abstract void add(String element);

		public abstract void removeAll();

		public abstract void removeSelectionListener(SelectionListener listener);

		public abstract void addSelectionListener(SelectionListener listener);

		public abstract int getSelectionIndex();

	}

	private static class ComboComboWrapper extends ComboWrapper {

		public ComboComboWrapper(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		Control createCombo(Composite parent, int style) {
			return new Combo(parent, style);
		}

		@Override
		public Combo getCombo() {
			return (Combo) super.getCombo();
		}

		@Override
		public void select(int i) {
			getCombo().select(i);
		}

		@Override
		public void add(String element) {
			getCombo().add(element);
		}

		@Override
		public void removeAll() {
			getCombo().removeAll();
		}

		@Override
		public void removeSelectionListener(SelectionListener listener) {
			getCombo().removeSelectionListener(listener);
		}

		@Override
		public void addSelectionListener(SelectionListener listener) {
			getCombo().addSelectionListener(listener);
		}

		@Override
		public int getSelectionIndex() {
			return getCombo().getSelectionIndex();
		}
	}

	private static class CComboComboWrapper extends ComboWrapper {

		public CComboComboWrapper(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		Control createCombo(Composite parent, int style) {
			return new CCombo(parent, style);
		}

		@Override
		public CCombo getCombo() {
			return (CCombo) super.getCombo();
		}

		@Override
		public void select(int i) {
			getCombo().select(i);
		}

		@Override
		public void add(String element) {
			getCombo().add(element);
		}

		@Override
		public void removeAll() {
			getCombo().removeAll();
		}

		@Override
		public void removeSelectionListener(SelectionListener listener) {
			getCombo().removeSelectionListener(listener);
		}

		@Override
		public void addSelectionListener(SelectionListener listener) {
			getCombo().addSelectionListener(listener);
		}

		@Override
		public int getSelectionIndex() {
			return getCombo().getSelectionIndex();
		}

	}
}
