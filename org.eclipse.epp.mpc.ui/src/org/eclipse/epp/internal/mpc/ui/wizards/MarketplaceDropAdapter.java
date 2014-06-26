/*******************************************************************************
 * Copyright (c) 2011, 2014 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - public API (bug 432803), drag&drop (bug 433333)
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * @author Benjamin Muskalla
 * @author Carsten Reckord
 */
public class MarketplaceDropAdapter implements IStartup {

	private static final int[] PREFERRED_DROP_OPERATIONS = { DND.DROP_DEFAULT, DND.DROP_COPY, DND.DROP_MOVE,
		DND.DROP_LINK };

	private static final int DROP_OPERATIONS = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT;

	private final DropTargetAdapter dropListener = new MarketplaceDropTargetListener();

	private final WorkbenchListener workbenchListener = new WorkbenchListener();

	private Transfer[] transferAgents;

	public void earlyStartup() {
		UIJob registerJob = new UIJob(Display.getDefault(), Messages.MarketplaceDropAdapter_0) {
			{
				setPriority(Job.SHORT);
				setSystem(true);
			}

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IWorkbench workbench = PlatformUI.getWorkbench();
				workbench.addWindowListener(workbenchListener);
				IWorkbenchWindow[] workbenchWindows = workbench
						.getWorkbenchWindows();
				for (IWorkbenchWindow window : workbenchWindows) {
					workbenchListener.hookWindow(window);
				}
				return Status.OK_STATUS;
			}

		};
		registerJob.schedule();
	}

	public void installDropTarget(final Shell shell) {
		hookUrlTransfer(shell, dropListener);
	}

	private DropTarget hookUrlTransfer(final Shell shell, DropTargetAdapter dropListener) {
		DropTarget target = findDropTarget(shell);
		if (target != null) {
			//target exists, get it and check proper registration
			registerWithExistingTarget(target);
		} else {
			target = new DropTarget(shell, DROP_OPERATIONS);
			if (transferAgents == null) {
				transferAgents = new Transfer[] { URLTransfer.getInstance() };
			}
			target.setTransfer(transferAgents);
		}
		registerDropListener(target, dropListener);

		Control[] children = shell.getChildren();
		for (Control child : children) {
			hookRecursive(child, dropListener);
		}
		return target;
	}

	private void registerDropListener(DropTarget target, DropTargetListener dropListener) {
		target.removeDropListener(dropListener);
		target.addDropListener(dropListener);
	}

	private void hookRecursive(Control child, DropTargetListener dropListener) {
		DropTarget childTarget = findDropTarget(child);
		if (childTarget != null) {
			registerWithExistingTarget(childTarget);
			registerDropListener(childTarget, dropListener);
		}
		if (child instanceof Composite) {
			Composite composite = (Composite) child;
			Control[] children = composite.getChildren();
			for (Control control : children) {
				hookRecursive(control, dropListener);
			}
		}
	}

	private void registerWithExistingTarget(DropTarget target) {
		Transfer[] transfers = target.getTransfer();
		boolean exists = false;
		if (transfers != null) {
			for (Transfer transfer : transfers) {
				if (transfer instanceof URLTransfer) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				Transfer[] newTransfers = new Transfer[transfers.length + 1];
				System.arraycopy(transfers, 0, newTransfers, 0, transfers.length);
				newTransfers[transfers.length] = URLTransfer.getInstance();
				target.setTransfer(newTransfers);
			}
		}
	}

	private DropTarget findDropTarget(Control control) {
		Object object = control.getData(DND.DROP_TARGET_KEY);
		if (object instanceof DropTarget) {
			return (DropTarget) object;
		}
		return null;
	}

	protected void proceedInstallation(String url) {
		SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(url);
		if (info != null) {
			MarketplaceUrlHandler.triggerInstall(info);
		}
	}

	private class MarketplaceDropTargetListener extends DropTargetAdapter {

		@Override
		public void dragEnter(DropTargetEvent e) {
			updateDragDetails(e);
		}

		@Override
		public void dragOver(DropTargetEvent e) {
			updateDragDetails(e);
		}

		@Override
		public void dragLeave(DropTargetEvent e) {
			if (e.detail == DND.DROP_NONE) {
				setDropOperation(e);
			}
		}

		@Override
		public void dropAccept(DropTargetEvent e) {
			updateDragDetails(e);
		}

		@Override
		public void dragOperationChanged(DropTargetEvent e) {
			updateDragDetails(e);
		}

		private void setDropOperation(DropTargetEvent e) {
			int allowedOperations = e.operations;
			for (int op : PREFERRED_DROP_OPERATIONS) {
				if ((allowedOperations & op) != 0) {
					e.detail = op;
					return;
				}
			}
			e.detail = allowedOperations;
		}

		private void updateDragDetails(DropTargetEvent e) {
			if (dropTargetIsValid(e)) {
				setDropOperation(e);
			}
		}

		private boolean dropTargetIsValid(DropTargetEvent e) {
			if (URLTransfer.getInstance().isSupportedType(e.currentDataType)) {
				if (Util.isWindows()) {
					//FIXME find a way to check the URL early on other platforms, too...
					if (e.data == null && !extractEventData(e)) {
						return false;
					}
					final String url = getUrlFromEvent(e);
					if (!MarketplaceUrlHandler.isPotentialSolution(url)) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		private boolean extractEventData(DropTargetEvent e) {
			TransferData transferData = e.currentDataType;
			if (transferData != null) {
				Object data = URLTransfer.getInstance().nativeToJava(transferData);
				if (data != null) {
					e.data = data;
					return true;
				}
			}
			return false;
		}

		@Override
		public void drop(DropTargetEvent event) {
			if (!URLTransfer.getInstance().isSupportedType(event.currentDataType)) {
				//ignore
				return;
			}
			if (event.data == null || !dropTargetIsValid(event)) {
				event.detail = DND.DROP_NONE;
				return;
			}
			final String url = getUrlFromEvent(event);
			//http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1640500
			if (MarketplaceUrlHandler.isPotentialSolution(url)) {
				DropTarget source = (DropTarget) event.getSource();
				Display display = source.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						proceedInstallation(url);
					}
				});
			}
		}

		// Depending on the form the link and browser/os,
		// we get the url twice in the data separated by new lines
		private String getUrlFromEvent(DropTargetEvent event) {
			Object eventData = event.data;
			if (eventData == null || !(eventData instanceof String)) {
				return null;
			}
			String[] dataLines = ((String) eventData).split(System.getProperty("line.separator")); //$NON-NLS-1$
			String url = dataLines[0];
			return url;
		}
	}

	private class WorkbenchListener implements IPartListener2, IPageListener, IPerspectiveListener, IWindowListener {

		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			pageChanged(page);
		}

		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		}

		public void pageActivated(IWorkbenchPage page) {
			pageChanged(page);
		}

		public void pageClosed(IWorkbenchPage page) {
		}

		public void pageOpened(IWorkbenchPage page) {
			pageChanged(page);
		}

		private void pageChanged(IWorkbenchPage page) {
			if (page == null) {
				return;
			}
			IWorkbenchWindow workbenchWindow = page.getWorkbenchWindow();
			windowChanged(workbenchWindow);
		}

		public void windowActivated(IWorkbenchWindow window) {
			windowChanged(window);
		}

		private void windowChanged(IWorkbenchWindow window) {
			if (window == null) {
				return;
			}
			Shell shell = window.getShell();
			runUpdate(shell);
		}

		public void windowDeactivated(IWorkbenchWindow window) {
		}

		public void windowClosed(IWorkbenchWindow window) {
		}

		public void windowOpened(IWorkbenchWindow window) {
			hookWindow(window);
		}

		public void hookWindow(IWorkbenchWindow window) {
			window.addPageListener(this);
			window.addPerspectiveListener(this);
			IPartService partService = (IPartService) window.getService(IPartService.class);
			partService.addPartListener(this);
			windowChanged(window);
		}

		public void partOpened(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		public void partActivated(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		public void partVisible(IWorkbenchPartReference partRef) {
		}

		public void partClosed(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		public void partDeactivated(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		public void partHidden(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		public void partInputChanged(IWorkbenchPartReference partRef) {
		}

		private void partUpdate(IWorkbenchPartReference partRef) {
			IWorkbenchPage page = partRef.getPage();
			pageChanged(page);
		}

		private void runUpdate(final Shell shell) {
			if (shell == null || shell.isDisposed()) {
				return;
			}
			Display display = shell.getDisplay();
			if (display == null || display.isDisposed()) {
				return;
			}
			try {
				display.asyncExec(new Runnable() {

					public void run() {
						if (!shell.isDisposed()) {
							installDropTarget(shell);
						}
					}
				});
			} catch (SWTException ex) {
				if (ex.code == SWT.ERROR_DEVICE_DISPOSED) {
					//ignore
					return;
				}
				MarketplaceClientUi.error(ex);
			} catch (RuntimeException ex) {
				MarketplaceClientUi.error(ex);
			}
		}
	}
}
