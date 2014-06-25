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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;

/**
 * An abstract implementation of a filter that operates on {@link Tag tags}.
 * 
 * @author David Green
 * @see Tag
 */
public abstract class AbstractTagFilter extends MarketplaceFilter {

	public static final String PROP_SELECTED = "selected"; //$NON-NLS-1$

	public static final String PROP_CHOICES = "choices"; //$NON-NLS-1$

	private List<Tag> choices;

	private final Set<Tag> selected = new HashSet<Tag>();

	private boolean selectAllOnNoSelection;

	private Object tagClassification;

	@Override
	public final boolean select(CatalogItem item) {
		if (selectAllOnNoSelection && selected.isEmpty()) {
			return true;
		}
		return hasSelectedTag(item);
	}

	/**
	 * subclasses may override to determine how tagging is detected
	 * 
	 * @return true if and only if the given item has one of the selected tags.
	 */
	protected boolean hasSelectedTag(CatalogItem item) {
		Set<Tag> tags = item.getTags();
		if (tags != null) {
			for (Tag selectedTag : selected) {
				if (tags.contains(selectedTag)) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Tag> getChoices() {
		return choices;
	}

	public void setChoices(List<Tag> choices) {
		List<Tag> previousChoices = this.choices;
		this.choices = choices;
		if (previousChoices != choices && (choices == null || !choices.equals(previousChoices))) {
			choicesChanged(choices, previousChoices);
		}
	}

	protected void choicesChanged(List<Tag> choices, List<Tag> previousChoices) {
		firePropertyChange(PROP_CHOICES, previousChoices, choices);
	}

	public Set<Tag> getSelected() {
		return selected;
	}

	public void setSelected(Set<Tag> selected) {
		this.selected.clear();
		if (selected != null) {
			this.selected.addAll(selected);
		}
	}

	public boolean isSelectAllOnNoSelection() {
		return selectAllOnNoSelection;
	}

	public void setSelectAllOnNoSelection(boolean selectAllOnNoSelection) {
		this.selectAllOnNoSelection = selectAllOnNoSelection;
	}

	protected void selectionUpdated() {
		firePropertyChange(PROP_SELECTED, null, getSelected());
	}

	protected void updateUi() {
	}

	@Override
	public void catalogUpdated(boolean wasCancelled) {
		// nothing to do.
	}

	public Object getTagClassification() {
		return tagClassification;
	}

	public void setTagClassification(Object tagClassification) {
		this.tagClassification = tagClassification;
	}
}
