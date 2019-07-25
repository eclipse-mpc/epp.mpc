/*******************************************************************************
 * Copyright (c) 2014, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - initial API and implementation, bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.core.service;

import org.eclipse.epp.internal.mpc.core.model.Category;
import org.eclipse.epp.internal.mpc.core.model.FavoriteList;
import org.eclipse.epp.internal.mpc.core.model.Identifiable;
import org.eclipse.epp.internal.mpc.core.model.Market;
import org.eclipse.epp.internal.mpc.core.model.Node;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.IIdentifiable;
import org.eclipse.epp.mpc.core.model.IMarket;
import org.eclipse.epp.mpc.core.model.INode;

/**
 * Factory for marketplace model instances suitable to use as input for {@link IMarketplaceService} requests.
 *
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class QueryHelper {

	private QueryHelper() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @return a node instance that can be used to look up a matching node on
	 * the marketplace by id.
	 */
	public static INode nodeById(String id) {
		return withId(new Node(), id);
	}

	/**
	 * @return a node instance that can be used to look up a matching node on
	 * the marketplace by url.
	 */
	public static INode nodeByUrl(String url) {
		return withUrl(new Node(), url);
	}

	/**
	 * @return a node instance that can be used to look up a matching node on
	 * the marketplace by either id or url.
	 */
	public static INode nodeByIdAndUrl(String id, String url) {
		return withUrl(withId(new Node(), id), url);
	}

	/**
	 * @return a category instance that can be used to look up a matching category on
	 * the marketplace by id.
	 */
	public static ICategory categoryById(String id) {
		return withId(new Category(), id);
	}

	/**
	 * @return a category instance that can be used to look up a matching category on
	 * the marketplace by url.
	 */
	public static ICategory categoryByUrl(String url) {
		return withUrl(new Category(), url);
	}

	/**
	 * @return a category instance that can be used to look up a matching category on
	 * the marketplace by name.
	 */
	public static ICategory categoryByName(String name) {
		return withName(new Category(), name);
	}

	/**
	 * @return a market instance that can be used to look up a matching market on
	 * the marketplace by id.
	 */
	public static IMarket marketById(String id) {
		return withId(new Market(), id);
	}

	/**
	 * @return a market instance that can be used to look up a matching market on
	 * the marketplace by url.
	 */
	public static IMarket marketByUrl(String url) {
		return withUrl(new Market(), url);
	}

	/**
	 * @return a market instance that can be used to look up a matching market on
	 * the marketplace by name.
	 */
	public static IMarket marketByName(String name) {
		return withName(new Market(), name);
	}

	public static IFavoriteList favoritesByUserId(String userId) {
		return withId(new FavoriteList(), userId);
	}

	public static IFavoriteList favoritesByUrl(String url) {
		return withUrl(new FavoriteList(), url);
	}

	private static <T extends Identifiable> T withId(T identifiable, String id) {
		identifiable.setId(id);
		return identifiable;
	}

	private static <T extends Identifiable> T withUrl(T identifiable, String url) {
		identifiable.setUrl(url);
		return identifiable;
	}

	private static <T extends Identifiable> T withName(T identifiable, String name) {
		identifiable.setName(name);
		return identifiable;
	}

	public static <T extends IIdentifiable> T findById(Iterable<? extends T> elements, String id) {
		for (T element : elements) {
			if (id.equals(element.getId())) {
				return element;
			}
		}
		return null;
	}

	public static <T extends IIdentifiable> T findById(Iterable<? extends T> elements, T template) {
		for (T element : elements) {
			if (element.equalsId(template)) {
				return element;
			}
		}
		return null;
	}
}
