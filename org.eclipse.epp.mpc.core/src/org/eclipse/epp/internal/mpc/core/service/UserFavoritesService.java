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
package org.eclipse.epp.internal.mpc.core.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NoServiceException;
import org.eclipse.userstorage.util.NotFoundException;
import org.eclipse.userstorage.util.ProtocolException;

public class UserFavoritesService extends AbstractDataStorageService {

	private static final String KEY = "mpc_favorites";

	private static final int RETRY_COUNT = 3;

	private static final String SEPARATOR = ",";

	protected IBlob getFavoritesBlob() {
		return getStorageService().getBlob(KEY);
	}

	public Set<String> getFavoriteIds()
			throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException {
		try {
			String favoritesData = getFavoritesBlob().getContentsUTF();
			return parseFavoritesBlobData(favoritesData);
		} catch (NotFoundException ex) {
			//the user does not yet have favorites
			return new LinkedHashSet<String>();
		} catch (ProtocolException ex) {
			throw processProtocolException(ex);
		}
	}

	public List<INode> getFavorites()
			throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException {
		Set<String> favoriteIds = getFavoriteIds();
		List<INode> favoriteNodes = new ArrayList<INode>(favoriteIds.size());
		for (String nodeId : favoriteIds) {
			INode node = QueryHelper.nodeById(nodeId);
			favoriteNodes.add(node);
		}
		return favoriteNodes;
	}

	protected Set<String> parseFavoritesBlobData(String favoritesData) {
		Set<String> favoriteIds = new LinkedHashSet<String>();
		for (StringTokenizer tokenizer = new StringTokenizer(favoritesData, SEPARATOR); tokenizer.hasMoreTokens();) {
			String nodeId = tokenizer.nextToken();
			favoriteIds.add(nodeId);
		}
		return favoriteIds;
	}

	public void setFavorites(Collection<? extends INode> nodes)
			throws NoServiceException, ConflictException, NotAuthorizedException, IllegalStateException, IOException
	{
		String favoritesData = createFavoritesBlobData(nodes);
		try {
			getFavoritesBlob().setContentsUTF(favoritesData);
		} catch (ProtocolException ex) {
			throw processProtocolException(ex);
		}
	}

	protected String createFavoritesBlobData(Collection<? extends INode> nodes) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (INode node : nodes) {
			if (first) {
				first = false;
			} else {
				builder.append(SEPARATOR);
			}
			builder.append(node.getId());
		}
		return builder.toString();
	}

	public void setFavorite(INode node, boolean favorite)
			throws NotFoundException, NotAuthorizedException, IOException, ConflictException {
		ConflictException conflictException = null;
		for (int i = 0; i < RETRY_COUNT; i++) {
			try {
				doSetFavorite(node, favorite);
				return;
			} catch (ConflictException e) {
				conflictException = e;
			} catch (ProtocolException ex) {
				throw processProtocolException(ex);
			}
		}
		if (conflictException != null) {
			throw conflictException;
		}
	}

	private void doSetFavorite(INode node, boolean favorite) throws NotFoundException, IOException, ConflictException {
		List<INode> favorites = getFavorites();
		INode currentFavorite = QueryHelper.findById(favorites, node);
		if (currentFavorite != null && !favorite) {
			favorites.remove(currentFavorite);
			setFavorites(favorites);
		} else if (currentFavorite == null && favorite) {
			favorites.add(node);
			setFavorites(favorites);
		}
	}
}
