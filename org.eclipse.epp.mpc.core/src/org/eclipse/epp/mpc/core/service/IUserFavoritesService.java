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
package org.eclipse.epp.mpc.core.service;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.epp.internal.mpc.core.service.AbstractDataStorageService.NotAuthorizedException;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NoServiceException;

public interface IUserFavoritesService {

	IMarketplaceStorageService getStorageService();

	Integer getFavoriteCount(INode node);

	Set<String> getFavoriteIds(IProgressMonitor monitor) throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException;

	List<INode> getFavorites(IProgressMonitor monitor) throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException;

	void setFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NoServiceException, ConflictException, NotAuthorizedException, IllegalStateException, IOException;

	void setFavorite(INode node, boolean favorite, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException;

	void addFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException;

	void removeFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException;

	List<String> getFavoriteIds(URI user, IProgressMonitor monitor) throws IOException;

	List<INode> getFavorites(URI user, IProgressMonitor monitor) throws IOException;

}