/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions - initial API and implementation, public API (bug 432803)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.service.News;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUiPlugin;
import org.eclipse.epp.mpc.core.model.INews;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;

/**
 * @author Carsten Reckord
 */
public class NewsViewer {

	private Control browser;

	private Control control;

	private final MarketplaceWizard wizard;

	public NewsViewer(MarketplaceWizard marketplaceWizard) {
		this.wizard = marketplaceWizard;
	}

	public Control createControl(Composite parent) {
		control = createBrowser(parent);
		if (control == null) {
			control = createNoBrowserPart(parent);
		}
		return control;
	}

	protected Control createNoBrowserPart(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).margins(20, 0).applyTo(container);

		Label noEmbedBrowserLabel = new Label(container, SWT.WRAP);
		GridDataFactory.fillDefaults()
		.align(SWT.BEGINNING, SWT.END)
		.grab(true, true)
		.hint(450, SWT.DEFAULT)
		.applyTo(noEmbedBrowserLabel);
		noEmbedBrowserLabel.setText(Messages.NewsViewer_No_embeddable_browser);

		final Link link = new Link(container, SWT.NONE);
		GridDataFactory.fillDefaults()
		.align(SWT.BEGINNING, SWT.BEGINNING)
		.indent(IDialogConstants.SMALL_INDENT, 0)
		.grab(true, true)
		.hint(450 - IDialogConstants.SMALL_INDENT, SWT.DEFAULT)
		.applyTo(link);
		link.setText(Messages.NewsViewer_No_news);
		link.setEnabled(false);
		link.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				WorkbenchUtil.openUrl(e.text, IWorkbenchBrowserSupport.AS_EXTERNAL);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		Menu popup = new Menu(parent.getShell(), SWT.POP_UP);
		MenuItem copyMenuItem = new MenuItem(popup, SWT.PUSH);
		copyMenuItem.setText(Messages.NewsViewer_Copy_Link_Address);
		copyMenuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(link.getDisplay());
				String data = (String) link.getData("href"); //$NON-NLS-1$
				clipboard.setContents(new Object[] { data }, new Transfer[] { TextTransfer.getInstance() });
				clipboard.dispose();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		link.setMenu(popup);

		browser = link;
		return container;
	}

	protected Control createBrowser(Composite parent) {
		if (!WorkbenchBrowserSupport.getInstance().isInternalWebBrowserAvailable()) {
			return null;
		}
		final Browser browser;
		try {
			browser = new Browser(parent, SWT.NONE);
		} catch (Throwable t) {
			// embedded browser not available
			return null;
		}

		browser.addProgressListener(new ProgressRunnable());
		browser.addLocationListener(new NewsUrlHandler(this));

		this.browser = browser;
		return browser;
	}

	public Control getControl() {
		return control;
	}

	public MarketplaceWizard getWizard() {
		return this.wizard;
	}

	public Browser getBrowser() {
		if (browser instanceof Browser) {
			return (Browser) browser;
		} else {
			return null;
		}
	}

	public void showNews(INews news) {
		final String url = news.getUrl();
		if (url != null && url.length() > 0) {
			showUrl(url);

			String key = computeNewsPreferenceKey();
			MarketplaceClientUiPlugin.getInstance().getPreferenceStore().putValue(key, computeNewsStamp(news));
		}
	}

	protected void showUrl(String url) {
		if (browser instanceof Browser) {
			((Browser) browser).setUrl(url);
		} else {
			Link link = (Link) browser;
			link.setData("href", url); //$NON-NLS-1$
			link.setText(NLS.bind("<a href=\"{0}\">{1}</a>", url, url)); //$NON-NLS-1$
			link.setEnabled(true);
			link.getParent().layout();
		}
	}

	public void refresh() {
		if (browser instanceof Browser) {
			((Browser) browser).refresh();
		}
	}

	public void stop() {
		if (browser instanceof Browser) {
			((Browser) browser).stop();
		}
	}

	public void dispose() {
		if (browser != null && browser.getMenu() != null) {
			browser.getMenu().dispose();
		}
		if (control != null) {
			control.dispose();
		}
	}

	public boolean isUpdated(INews news) {
		String url = news.getUrl();
		if (url == null || url.length() == 0) {
			return false;
		}
		String key = computeNewsPreferenceKey();
		String previous = MarketplaceClientUiPlugin.getInstance().getPreferenceStore().getString(key);
		if (previous != null && previous.length() > 0) {
			String current = computeNewsStamp(news);
			return !previous.equals(current);
		}
		return true;
	}

	private String computeNewsStamp(INews news) {
		return NLS.bind("[{0}]{1}", news.getTimestamp(), news.getUrl()); //$NON-NLS-1$
	}

	private String computeNewsPreferenceKey() {
		CatalogDescriptor catalogDescriptor = wizard.getConfiguration().getCatalogDescriptor();
		URL catalogUrl = catalogDescriptor.getUrl();
		URI catalogUri;
		try {
			catalogUri = catalogUrl.toURI();
		} catch (URISyntaxException e) {
			// should never happen
			throw new IllegalStateException(e);
		}
		String marketplaceId = catalogUri.toString().replaceAll("[^a-zA-Z0-9_-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		return News.class.getSimpleName() + "/" + marketplaceId; //$NON-NLS-1$
	}

	private class ProgressRunnable implements IRunnableWithProgress, ProgressListener {
		private int current;

		private int total;

		private int lastCurrent;

		private int lastTotal;

		private boolean done;

		private boolean running = false;

		public void completed(ProgressEvent event) {
			if (running) {
				running = false;
				done();
			}
		}

		public void changed(ProgressEvent event) {
			if (event.total == 0) {
				return;
			}
			if (!running) {
				running = true;
				current = event.current;
				total = event.total;
				Display.getCurrent().asyncExec(new Runnable() {

					public void run() {
						try {
							wizard.getContainer().run(true, true, ProgressRunnable.this);
						} catch (InvocationTargetException e) {

						} catch (InterruptedException e) {
							// cancelled by the user
							completed(null);
							getBrowser().stop();
						}
					}

				});
			} else {
				progress(event.current, event.total);
			}
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			final SubMonitor progress = SubMonitor.convert(monitor, Messages.NewsViewer_Loading, total);
			if (current > 0) {
				progress.worked(current);
			}
			long lastUpdate = System.currentTimeMillis();
			while (!monitor.isCanceled() && !done) {
				int newCurrent, newTotal, newRemaining, worked;
				synchronized (this) {
					wait(200);

					newCurrent = current;
					newTotal = total;

					int oldRemaining = lastTotal - lastCurrent;
					newRemaining = newTotal - newCurrent;

					long now = System.currentTimeMillis();
					long timeSinceLastUpdate = now - lastUpdate;

					if (newCurrent != lastCurrent || newTotal != lastTotal) {
						lastUpdate = now;
					} else if ((newRemaining == 0 && oldRemaining == 0 && timeSinceLastUpdate >= 900)
							|| timeSinceLastUpdate > 10000) {
						//FIXME hack because completed event is not always coming
						break;
					}

					worked = oldRemaining - newRemaining;
					if (worked <= 0) {
						worked = 1;
					}
					lastTotal = newTotal;
					lastCurrent = newCurrent;
				}
				progress.setWorkRemaining(newRemaining + worked);
				progress.worked(worked);
			}
			if (monitor.isCanceled()) {
				getBrowser().stop();
				return;
			}
		}

		public synchronized void progress(int current, int total) {
			this.current = current;
			this.total = total;
			notify();
		}

		public synchronized void done() {
			done = true;
			notify();
		}
	}
}
