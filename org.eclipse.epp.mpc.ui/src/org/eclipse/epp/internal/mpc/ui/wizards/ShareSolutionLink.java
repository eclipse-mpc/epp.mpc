/*******************************************************************************
 * Copyright (c) 2011 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.epp.internal.mpc.core.util.TextUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.internal.mpc.ui.util.Util;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * @author Benjamin Muskalla
 */
public class ShareSolutionLink {

	private final CatalogItem catalogItem;

	private final Button control;

	public ShareSolutionLink(Composite parent, CatalogItem item) {
		this.catalogItem = item;

		control = createShareLink(parent);
		Menu popupMenu = createMenu(control);
		attachMenu(control, popupMenu);
	}

	public Control getControl() {
		return control;
	}

	private static Button createShareLink(Composite parent) {
		final Button share = new Button(parent, SWT.PUSH);
		DiscoveryItem.setWidgetId(share, DiscoveryItem.WIDGET_ID_SHARE);
		share.setImage(MarketplaceClientUiPlugin.getInstance()
				.getImageRegistry()
				.get(MarketplaceClientUiPlugin.ITEM_ICON_SHARE));
		share.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = Messages.ShareSolutionLink_Share;
			}
		});
		return share;
	}

	private Menu createMenu(final Control control) {
		final Menu popupMenu = new Menu(control);
		createTweetMenu(popupMenu);
		createMailMenu(popupMenu);
		createOpenInBrowserMenu(popupMenu);
		createCopyLinkMenu(popupMenu);
		return popupMenu;
	}

	private void createTweetMenu(final Menu popupMenu) {
		MenuItem twitterItem = new MenuItem(popupMenu, SWT.POP_UP);
		twitterItem.setText(Messages.ShareSolutionLink_Twitter);
		twitterItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String tweet = NLS.bind(Messages.ShareSolutionLink_tweet, new Object[] { catalogItem.getName(),
						getUrl() });
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

	private void createCopyLinkMenu(final Menu popupMenu) {
		MenuItem copyItem = new MenuItem(popupMenu, SWT.POP_UP);
		copyItem.setText(Messages.ShareSolutionLink_CopyToClipboard);
		copyItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyLinkToClipboard();
			}
		});
	}

	private void createOpenInBrowserMenu(final Menu popupMenu) {
		MenuItem copyItem = new MenuItem(popupMenu, SWT.POP_UP);
		copyItem.setText(Messages.ShareSolutionLink_OpenInBrowser);
		copyItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyLinkToClipboard();
			}
		});
	}

	protected void openInBrowser() {
		WorkbenchUtil.openUrl(getUrl(), IWorkbenchBrowserSupport.AS_EXTERNAL);
	}

	protected void copyLinkToClipboard() {
		Clipboard clipboard = new Clipboard(control.getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		Transfer[] transfers = new Transfer[] { textTransfer };
		Object[] data = new Object[] { getUrl() };
		clipboard.setContents(data, transfers);
		clipboard.dispose();
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
				Clipboard clipboard = new Clipboard(control.getDisplay());
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
		return catalogItem.getName() + "\n" + getUrl() + "\n\n" + description; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String getUrl() {
		return ((INode) catalogItem.getData()).getUrl();
	}

	private void openMail(URI uri) throws Exception {
		// Desktop.getDesktop().mail(uri);
		if (isAwtMailSupported()) {
			openMailAwt(uri);
		} else {
			openMailSwt(uri);
		}
	}

	private void openMailSwt(URI uri) {
		//TODO bug 384840 - On linux, use xdb-open through Runtime.exec and check return status before falling back to Program.launch

		//this works less reliably than the awt version. we also don't get any feedback if it worked. so only use as a last resort
		Program.launch(uri.toString());
	}

	protected void openMailAwt(URI uri) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
	InvocationTargetException {
		Class<?> desktopClazz = getDesktopClazz();
		Method getDesktopMethod = desktopClazz.getMethod("getDesktop"); //$NON-NLS-1$
		Object desktop = getDesktopMethod.invoke(null);

		Method mailMethod = desktopClazz.getMethod("mail", URI.class); //$NON-NLS-1$
		mailMethod.invoke(desktop, uri);
	}

	private boolean isAwtMailSupported() {
		if (Util.isGtk3()) {
			return false;
		}
		try {
			Class<?> desktopClazz = getDesktopClazz();
			Method isDesktopSupportedMethod = desktopClazz.getMethod("isDesktopSupported"); //$NON-NLS-1$
			boolean isDesktopSupported = (Boolean) isDesktopSupportedMethod.invoke(null);
			if (!isDesktopSupported) {
				return false;
			}
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

			return isMailSupported;
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

	public void setShowText(boolean showText) {
		if (showText) {
			control.setText(Messages.DiscoveryItem_Share);
		} else {
			control.setText(""); //$NON-NLS-1$
		}
	}
}
