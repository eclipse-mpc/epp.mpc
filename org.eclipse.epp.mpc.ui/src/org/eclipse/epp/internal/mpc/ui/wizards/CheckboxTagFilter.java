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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A tag filter that presents choices as checkboxes.
 * 
 * @author David Green
 */
public class CheckboxTagFilter extends AbstractTagFilter {

	private Composite buttonContainer;

	@Override
	public void createControl(Composite parent) {
		if (getChoices() == null) {
			throw new IllegalStateException();
		}
		buttonContainer = new Composite(parent, SWT.NULL);
		rebuildChoicesUi();
	}

	protected void rebuildChoicesUi() {
		if (buttonContainer != null) {
			for (Control control : buttonContainer.getChildren()) {
				control.dispose();
			}
			for (final Tag choice : getChoices()) {
				final Button checkbox = new Button(buttonContainer, SWT.CHECK);
				checkbox.setSelection(getSelected().contains(choice));
				checkbox.setText(choice.getLabel());
				checkbox.addSelectionListener(new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent e) {
						widgetSelected(e);
					}

					public void widgetSelected(SelectionEvent e) {
						boolean selection = checkbox.getSelection();
						if (selection) {
							getSelected().add(choice);
						} else {
							getSelected().remove(choice);
						}
						selectionUpdated();
					}
				});
			}
			GridLayoutFactory.fillDefaults().numColumns(buttonContainer.getChildren().length).applyTo(buttonContainer);
		}
	}

	@Override
	protected void choicesChanged(List<Tag> choices, List<Tag> previousChoices) {
		rebuildChoicesUi();
		super.choicesChanged(choices, previousChoices);
	}
}
