/*******************************************************************************
 * Copyright (c) 2011 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.epp.internal.mpc.core.util.TextUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * @author Benjamin Muskalla
 */
public class ShareSolutionLink extends Composite {

	private final CatalogItem catalogItem;

	public ShareSolutionLink(Composite parent, CatalogItem item) {
		super(parent, SWT.NONE);
		this.catalogItem = item;
		setLayout(new GridLayout());

		Control shareControl = createShareLink(this);
		Menu popupMenu = createMenu(shareControl);
		attachMenu(shareControl, popupMenu);

	}

	private static Control createShareLink(Composite parent) {
		Link link = new Link(parent, SWT.NONE);
		link.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		link.setText("<a>" + //$NON-NLS-1$
				Messages.ShareSolutionLink_Share + "</a>"); //$NON-NLS-1$

		return link;
	}

	private Menu createMenu(final Control control) {
		final Menu popupMenu = new Menu(control);
		createTweetMenu(popupMenu);
		if (isMailSupported()) {
			createMailMenu(popupMenu);
		}
		return popupMenu;
	}

	private void createTweetMenu(final Menu popupMenu) {
		MenuItem twitterItem = new MenuItem(popupMenu, SWT.POP_UP);
		twitterItem.setText(Messages.ShareSolutionLink_Twitter);
		twitterItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String tweet = NLS.bind(Messages.ShareSolutionLink_tweet, new Object[] {
						catalogItem.getName(), catalogItem.getOverview().getUrl() });
				WorkbenchUtil.openUrl("http://twitter.com/?status=" + tweet, IWorkbenchBrowserSupport.AS_EXTERNAL); //$NON-NLS-1$
			}
		});
	}

	private void createMailMenu(final Menu popupMenu) {
		MenuItem mailItem = new MenuItem(popupMenu, SWT.POP_UP);
		mailItem.setText(Messages.ShareSolutionLink_EMail);
		mailItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openNewMail();
			}
		});
	}

	protected void openNewMail() {
		String subject = NLS.bind(Messages.ShareSolutionLink_mailSubject, new Object[] { catalogItem.getName() });
		String body = computeMessage();
		String recipient = Messages.ShareSolutionLink_recipient;
		String mailToString = "mailto:" + recipient + "?subject=" + subject + "&body=" + body; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			URI uri = URIUtil.fromString(mailToString);
			openMail(uri);
		} catch (Exception e) {
			boolean copyToClipboard = MessageDialog.openQuestion(WorkbenchUtil.getShell(),
					Messages.ShareSolutionLink_share, Messages.ShareSolutionLink_failed_to_open_manually_share);
			if (copyToClipboard) {
				Clipboard clipboard = new Clipboard(getDisplay());
				TextTransfer textTransfer = TextTransfer.getInstance();
				Transfer[] transfers = new Transfer[] { textTransfer };
				Object[] data = new Object[] { body };
				clipboard.setContents(data, transfers);
				clipboard.dispose();
			}
			MarketplaceClientUi.error(e);
		}
	}

	private String computeMessage() {
		// NOTE: put URL before description since some mail clients have troubles with descriptions, especially if they're long.
		String description = catalogItem.getDescription() == null ? "" : catalogItem.getDescription(); //$NON-NLS-1$
		description = TextUtil.stripHtmlMarkup(catalogItem.getDescription()).trim();
		return catalogItem.getName()
		+ "\n" + catalogItem.getOverview().getUrl() + "\n\n" + description; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void openMail(URI uri) throws Exception {
		// Desktop.getDesktop().mail(uri);
		Class<?> desktopClazz = getDesktopClazz();
		Method getDesktopMethod = desktopClazz.getMethod("getDesktop"); //$NON-NLS-1$
		Object desktop = getDesktopMethod.invoke(null);

		Method mailMethod = desktopClazz.getMethod("mail", URI.class); //$NON-NLS-1$
		mailMethod.invoke(desktop, uri);
	}

	private boolean isMailSupported() {
		// return Desktop.isDesktopSupported() &&
		// Desktop.getDesktop().isSupported(Action.MAIL)
		try {
			Class<?> desktopClazz = getDesktopClazz();
			Method isDesktopSupportedMethod = desktopClazz.getMethod("isDesktopSupported"); //$NON-NLS-1$
			boolean isDesktopSupported = (Boolean) isDesktopSupportedMethod.invoke(null);

			Method getDesktopMethod = desktopClazz.getMethod("getDesktop"); //$NON-NLS-1$
			Object desktop = getDesktopMethod.invoke(null);

			Class<?>[] classes = desktopClazz.getClasses();
			Class<?> actionEnum = null;
			for (Class<?> innerClass : classes) {
				if (innerClass.getName().equals("java.awt.Desktop$Action")) { //$NON-NLS-1$
					actionEnum = innerClass;
				}
			}
			if (actionEnum == null) {
				return false;
			}
			Method isSupportedMethod = desktop.getClass().getMethod("isSupported", actionEnum); //$NON-NLS-1$

			Field mailEnumOption = actionEnum.getDeclaredField("MAIL"); //$NON-NLS-1$
			boolean isMailSupported = (Boolean) isSupportedMethod.invoke(desktop, mailEnumOption.get(null));

			return isDesktopSupported && isMailSupported;
		} catch (Exception e) {
			// ignore, we don't support mail in this case
		}
		return false;
	}

	private Class<?> getDesktopClazz() throws ClassNotFoundException {
		Class<?> desktopClazz = getClass().getClassLoader().loadClass("java.awt.Desktop"); //$NON-NLS-1$
		return desktopClazz;
	}

	private static void attachMenu(final Control shareControl, final Menu popupMenu) {
		shareControl.setMenu(popupMenu);
		shareControl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				popupMenu.setVisible(true);
			}
		});
	}

}
