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
package org.eclipse.epp.internal.mpc.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;

/**
 * A utility for managing tasks performed in an executor service. Use as follows:
 *
 * <pre>
 * <code>
 * ConcurrentTaskManager executor = new ConcurrentTaskManager(anticipatedNumberOfTasks,"Checking for updates");
 *   try {
 *   	for (...) {
 *   		executor.submit(new Runnable() {...});
 *   	}
 *   	executor.waitUntilFinished(monitor);
 *   } finally {
 *   	executor.shutdownNow();
 *   }
 * </code>
 * </pre>
 *
 * @author dgreen
 */
public class ConcurrentTaskManager {

	private final java.util.concurrent.ExecutorService executor;

	private final List<Future<?>> futures = new ArrayList<Future<?>>();

	private final String taskName;

	public ConcurrentTaskManager(java.util.concurrent.ExecutorService executor, String taskName) {
		this.executor = executor;
		this.taskName = taskName;
	}

	public ConcurrentTaskManager(int size, String taskName) {
		this(Executors.newFixedThreadPool(Math.max(1, Math.min(size, 10))), taskName);
	}

	public <T> void submit(Callable<T> task) {
		futures.add(executor.submit(task));
	}

	public void submit(Runnable task) {
		futures.add(executor.submit(task));
	}

	public List<Future<?>> getFutures() {
		return futures;
	}

	public void waitUntilFinished(IProgressMonitor monitor) throws CoreException {
		final MultiStatus errorStatus = new MultiStatus(MarketplaceClientUi.BUNDLE_ID, IStatus.OK,
				Messages.ConcurrentTaskManager_multipleErrorsOccurred, null);
		final int totalWork = futures.isEmpty() ? 1 : futures.size();
		monitor.beginTask(taskName, totalWork);
		try {
			if (!futures.isEmpty()) {
				final int workUnit = 1;
				while (!futures.isEmpty()) {
					Future<?> future = futures.remove(0);
					final int maxRetries = 15;
					for (int retryCount = 0;; ++retryCount) {
						try {
							future.get(1L, TimeUnit.SECONDS);
							break;
						} catch (TimeoutException e) {
							if (monitor.isCanceled()) {
								return;
							}
						} catch (InterruptedException e) {
							throw new CoreException(new Status(IStatus.CANCEL, MarketplaceClientUi.BUNDLE_ID,
									e.getMessage()));
						} catch (ExecutionException e) {
							errorStatus.add(new Status(IStatus.ERROR, MarketplaceClientUi.BUNDLE_ID, e.getCause()
									.getMessage(), e.getCause()));
							if (monitor.isCanceled()) {
								break;
							}
						}
						if (retryCount > maxRetries) {
							future.cancel(true);
							break;
						}
					}
					monitor.worked(workUnit);
				}
			}
			if (!errorStatus.isOK() && errorStatus.getChildren().length > 0) {
				if (errorStatus.getChildren().length == 1) {
					throw new CoreException(errorStatus.getChildren()[0]);
				} else {
					throw new CoreException(errorStatus);
				}
			}
		} finally {
			executor.shutdownNow();
			monitor.done();
		}
	}

	public void shutdownNow() {
		executor.shutdownNow();
	}

}
