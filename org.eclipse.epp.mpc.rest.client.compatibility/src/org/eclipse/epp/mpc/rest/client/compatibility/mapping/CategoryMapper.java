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
package org.eclipse.epp.mpc.rest.client.compatibility.mapping;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.epp.internal.mpc.core.model.Categories;
import org.eclipse.epp.mpc.core.model.ICategories;
import org.eclipse.epp.mpc.core.model.ICategory;
import org.eclipse.epp.mpc.rest.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@SuppressWarnings("restriction")
@Mapper(uses = { NodeMapper.class })
public abstract class CategoryMapper extends AbstractMapper {

	public ICategory toCategory(Category category) {
		return toCategoryInternal(category);
	}

	public ICategories toCategories(List<Category> categories) {
		return toCategoriesInternal(categories);
	}

	@Mappings({ @Mapping(source = "title", target = "name"), @Mapping(ignore = true, target = "node"),
		@Mapping(ignore = true, target = "count") })
	abstract org.eclipse.epp.internal.mpc.core.model.Category toCategoryInternal(Category category);

	Categories toCategoriesInternal(List<Category> categories) {
		Categories result = new Categories();
		result.setCategory(categories.stream().map(this::toCategoryInternal).collect(Collectors.toList()));
		return result;
	}

	List<org.eclipse.epp.internal.mpc.core.model.Category> toCategoryListInternal(List<Category> categories) {
		return categories.stream().map(this::toCategoryInternal).collect(Collectors.toList());
	}
}
