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

import org.eclipse.epp.internal.mpc.core.model.Tags;
import org.eclipse.epp.mpc.core.model.ITag;
import org.eclipse.epp.mpc.core.model.ITags;
import org.eclipse.epp.mpc.rest.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@SuppressWarnings("restriction")
@Mapper
public abstract class TagMapper extends AbstractMapper {

	public ITag toTag(Tag tag) {
		return toTagInternal(tag);
	}

	public ITags toTags(List<Tag> tags) {
		return toTagsInternal(tags);
	}

	@Mapping(source = "title", target = "name")
	abstract org.eclipse.epp.internal.mpc.core.model.Tag toTagInternal(Tag tag);

	Tags toTagsInternal(List<Tag> tags) {
		Tags result = new Tags();
		tags.stream().map(this::toTagInternal).forEach(t -> result.getTags().add(t));
		return result;
	}
}
