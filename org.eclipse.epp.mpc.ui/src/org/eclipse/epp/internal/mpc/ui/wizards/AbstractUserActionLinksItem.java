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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public abstract class AbstractUserActionLinksItem extends UserActionViewerItem<UserActionCatalogItem> {

	private final Map<String, ActionLink> actions = new HashMap<String, ActionLink>();

	public AbstractUserActionLinksItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider,
			UserActionCatalogItem element, MarketplaceViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
	}

	protected void createContent(ActionLink... actionLinks) {
		Composite parent = this;
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(parent);

		int vAlignLinks = SWT.CENTER;
		String descriptionText = getDescriptionText();
		if (descriptionText != null) {
			Label descriptionLabel = new Label(parent, SWT.CENTER);
			descriptionLabel.setText(descriptionText);
			GridDataFactory.swtDefaults()
			.grab(true, false)
			.align(SWT.CENTER, SWT.END)
			.applyTo(descriptionLabel);
			vAlignLinks = SWT.BEGINNING;
		}
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				Object data = event.data;
				if (data == null) {
					data = event.text;
					if (data == null) {
						data = event.widget.getData();
					}
				}
				actionPerformed(data);
			}
		};
		Composite linkParent = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.CENTER, vAlignLinks).applyTo(linkParent);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(linkParent);

		boolean first = true;
		for (ActionLink actionLink : actionLinks) {
			actions.put(actionLink.getId(), actionLink);
			if (first) {
				first = false;
			} else {
				Label separator = new Label(linkParent, SWT.CENTER);
				separator.setText(" | "); //$NON-NLS-1$
				GridDataFactory.swtDefaults().align(SWT.CENTER, vAlignLinks).applyTo(separator);
			}
			String linkText = getLinkText(actionLink);
			String tooltip = actionLink.getTooltip();
			if (tooltip == null) {
				tooltip = getLinkToolTipText();
			}
			Control link = createActionLink(linkParent, linkText, tooltip);
			link.setData(actionLink.getId());
			link.addListener(SWT.Selection, listener);
			GridDataFactory.swtDefaults().align(SWT.END, vAlignLinks).applyTo(link);
		}
	}

	private String getLinkText(ActionLink actionLink) {
		return MessageFormat.format("<a href=\"{0}\">{1}</a>", actionLink.getId(), actionLink.getLabel()); //$NON-NLS-1$
	}

	protected String getDescriptionText() {
		return null;
	}

	@Override
	protected String getLinkText() {
		return null;
	}

	@Override
	protected void actionPerformed(Object data) {
		ActionLink actionLink = actions.get(data);
		if (actionLink != null) {
			actionLink.selected();
		}
	}
}
