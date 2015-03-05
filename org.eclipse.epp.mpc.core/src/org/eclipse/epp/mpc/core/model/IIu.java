/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yatta Solutions GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.core.model;

/**
 * A installable unit that can be optional and/or preseleted on install
 *
 * @author Carsten Reckord
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IIu {

	String getId();

	boolean isOptional();

	boolean isSelected();

}
