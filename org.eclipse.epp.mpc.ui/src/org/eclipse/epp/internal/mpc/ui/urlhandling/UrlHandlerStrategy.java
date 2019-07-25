/*******************************************************************************
 * Copyright (c) 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.urlhandling;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface UrlHandlerStrategy {
	boolean handles(String url);

	abstract class Registry<S extends UrlHandlerStrategy> {

		private final AtomicReference<S> latestMatchingUrlHandler = new AtomicReference<>();

		protected abstract S[] getUrlHandlers();

		public Optional<S> selectUrlHandler(String url) {
			S latestHandler = latestMatchingUrlHandler.get();
			if (latestHandler != null && latestHandler.handles(url)) {
				return Optional.of(latestHandler);
			}
			for (S handler : getUrlHandlers()) {
				if (handler.handles(url)) {
					latestMatchingUrlHandler.compareAndSet(latestHandler, handler);
					return Optional.of(handler);
				}
			}
			return Optional.empty();
		}
	}
}