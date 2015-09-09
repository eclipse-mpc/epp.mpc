/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	The Eclipse Foundation - initial API and implementation
 * 	Yatta Solutions - bug 413871: performance
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.catalog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.epp.internal.mpc.core.util.TransportFactory;
import org.eclipse.epp.internal.mpc.core.util.URLUtil;
import org.eclipse.epp.internal.mpc.ui.MarketplaceClientUi;
import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 * @author Carsten Reckord
 */
public class ResourceProvider {

	public static interface ResourceReceiver<T> {
		T processResource(URL resource);

		void setResource(T resource);
	}

	public static final class ResourceFuture implements Future<URL> {

		private InputStream input;

		private final FutureTask<URL> delegate;

		ResourceFuture(final File outputFile) {
			delegate = new FutureTask<URL>(new Callable<URL>() {

				public URL call() throws Exception {
					if (input == null) {
						throw new IllegalStateException();
					}
					URL outputURL;
					try {
						outputURL = outputFile.toURI().toURL();
					} catch (MalformedURLException e) {
						MarketplaceClientUi.error(e);
						return null;
					}
					BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
					boolean success = false;
					try {
						InputStream buffered = new BufferedInputStream(input);
						int i;
						while ((i = buffered.read()) != -1) {
							output.write(i);
						}
						success = true;
					} finally {
						output.close();
						if (!success || !outputFile.exists()) {
							outputFile.delete();
							outputURL = null;
						}
					}
					return outputURL;
				}
			});
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			return delegate.cancel(mayInterruptIfRunning);
		}

		public boolean isCancelled() {
			return delegate.isCancelled();
		}

		public boolean isDone() {
			return delegate.isDone();
		}

		public URL get() throws InterruptedException, ExecutionException {
			return delegate.get();
		}

		public URL get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return delegate.get(timeout, unit);
		}

		FutureTask<URL> getDelegate() {
			return delegate;
		}

		public URL retrieve(InputStream stream) throws IOException {
			synchronized(this)
			{
				if (this.isDone() || this.input != null) {
					return getURL();
				}
				this.input = stream;
			}
			try
			{
				delegate.run();
				return getURL();
			}
			finally
			{
				synchronized (this) {
					this.input = null;
				}
			}
		}

		public URL getURL() throws IOException {
			try {
				return get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					RuntimeException runtimeException = (RuntimeException) cause;
					throw runtimeException;
				} else if (cause instanceof IOException) {
					IOException ioException = (IOException) cause;
					throw ioException;
				} else {
					IOException ioException = new IOException(cause.getMessage());
					ioException.initCause(cause);
					throw ioException;
				}
			} catch (InterruptedException e) {
				return null;
			}
		}

		public URL getLocalURL() {
			if (isDone() && !isCancelled()) {
				try {
					return getURL();
				} catch (Exception ex) {
					// this is explicitly ignored here...
				}
			}
			return null;
		}

	}


	private final File dir;

	private final Map<String, ResourceFuture> resources = new ConcurrentHashMap<String, ResourceFuture>();

	public ResourceProvider() throws IOException {
		dir = File.createTempFile(ResourceProvider.class.getSimpleName(), ".tmp"); //$NON-NLS-1$
		dir.delete();
		if (!dir.mkdirs() || !dir.isDirectory()) {
			throw new IOException(NLS.bind(Messages.ResourceProvider_FailedCreatingTempDir, dir.getAbsolutePath()));
		}
	}

	public URL getLocalResource(String resourceName) {
		Future<URL> resource = getResource(resourceName);
		try {
			return resource == null ? null : resource.isCancelled() || !resource.isDone() ? null : resource.get();
		} catch (Exception e) {
			return null;
		}
	}

	public ResourceFuture getResource(String resourceName) {
		synchronized (resources) {
			return resources.get(resourceName);
		}
	}

	public boolean containsResource(String resourceName) {
		synchronized (resources) {
			return resources.containsKey(resourceName);
		}
	}

	public ResourceFuture registerResource(String resourceName) throws IOException {
		ResourceFuture resourceFuture;
		synchronized (resources) {
			resourceFuture = resources.get(resourceName);
			if (resourceFuture == null) {
				String filenameHint = resourceName;
				if (filenameHint.lastIndexOf('/') != -1) {
					filenameHint = filenameHint.substring(filenameHint.lastIndexOf('/') + 1);
				}
				filenameHint = filenameHint.replaceAll("[^a-zA-Z0-9\\.]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
				if (filenameHint.length() > 32) {
					String hash = Integer.toHexString(filenameHint.hashCode());
					filenameHint = filenameHint.substring(0, 6) + "_" //$NON-NLS-1$
							+ hash + "_" //$NON-NLS-1$
							+ filenameHint.substring(filenameHint.length() - (32 - hash.length() - 1 - 6 - 1));
				}
				final File outputFile = createTempFile(filenameHint);
				outputFile.deleteOnExit();
				resourceFuture = new ResourceFuture(outputFile);
				resources.put(resourceName, resourceFuture);
			}
		}
		return resourceFuture;
	}

	private File createTempFile(String filenameHint) throws IOException {
		for (int i = 0; i < 5; i++) {
			// we sometimes get intermittent errors creating the temp file. so retry a couple of times
			try {
				if (!dir.isDirectory()) {
					if (!dir.mkdirs()) {
						throw new IOException(
								NLS.bind(Messages.ResourceProvider_FailedCreatingTempDir, dir.getAbsolutePath()));
					}
				}
				final File outputFile = File.createTempFile("res_", filenameHint, dir); //$NON-NLS-1$
				return outputFile;
			} catch (IOException e) {
				//ignore
			}
		}
		final File outputFile = File.createTempFile("res_", filenameHint, dir); //$NON-NLS-1$
		return outputFile;
	}

	public ResourceFuture retrieveResource(String requestSource, String resourceUrl) throws IOException,
	URISyntaxException {
		URI resourceUri = URLUtil.toURI(resourceUrl);
		return retrieveResource(requestSource, resourceUrl, resourceUri);
	}

	public ResourceFuture retrieveResource(String requestSource, URI resourceUrl) throws IOException {
		return retrieveResource(requestSource, resourceUrl.toString(), resourceUrl);
	}

	public ResourceFuture retrieveResource(final String requestSource, final String resourceName, final URI resourceUrl)
			throws IOException {
		ResourceFuture resourceFuture;
		synchronized (resources) {
			resourceFuture = resources.get(resourceName);
			if (resourceFuture == null) {
				final ResourceFuture finalResourceFuture = registerResource(resourceName);
				resourceFuture = finalResourceFuture;
				new Job(Messages.ResourceProvider_retrievingResource) {

					{
						setPriority(INTERACTIVE);
						setUser(false);
						setSystem(true);
					}

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							InputStream in = TransportFactory.createTransport().stream(resourceUrl, monitor);
							finalResourceFuture.retrieve(in);
						} catch (FileNotFoundException e) {
							//MarketplaceClientUi.error(NLS.bind(Messages.AbstractResourceRunnable_resourceNotFound, new Object[] { catalogItem.getName(),
							//catalogItem.getId(), resourceUrl }), e);
						} catch (IOException e) {
							if (e.getCause() instanceof OperationCanceledException) {
								// canceled, nothing we want to do here
							} else {
								MarketplaceClientUi.log(IStatus.WARNING, Messages.ResourceProvider_downloadError,
										requestSource, resourceUrl, e);
							}
						} catch (CoreException e) {
							MarketplaceClientUi.log(IStatus.WARNING, Messages.ResourceProvider_downloadError,
									requestSource, resourceUrl, e);
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		}
		return resourceFuture;
	}

	public void dispose() {
		if (dir != null && dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					file.delete();
				}
			}
			dir.delete();
		}
		resources.clear();
	}

	public <T> void provideResource(final ResourceReceiver<T> receiver, final String resourcePath, T fallbackResource) {
		final ResourceFuture resource = getResource(resourcePath);
		if (resource != null) {
			if (resource.isDone()) {
				setResource(receiver, resourcePath, resource);
				return;
			}
			if (fallbackResource != null) {
				receiver.setResource(fallbackResource);
			}
			new Job(Messages.ResourceProvider_waitingForDownload) {

				{
					setPriority(INTERACTIVE);
					setUser(false);
					setSystem(true);
				}

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					return setResource(receiver, resourcePath, resource);
				}
			}.schedule();
		} else if (fallbackResource != null) {
			receiver.setResource(fallbackResource);
		}
	}

	private <T> IStatus setResource(final ResourceReceiver<T> receiver, final String resourcePath,
			final ResourceFuture resource) {
		try {
			URL resourceUrl = resource.get();
			if (resourceUrl != null) {
				T processedResource = receiver.processResource(resourceUrl);
				receiver.setResource(processedResource);
			}
		} catch (InterruptedException e) {
			return Status.CANCEL_STATUS;
		} catch (ExecutionException e) {
			// already logged during download
		}
		return Status.OK_STATUS;
	}

}
