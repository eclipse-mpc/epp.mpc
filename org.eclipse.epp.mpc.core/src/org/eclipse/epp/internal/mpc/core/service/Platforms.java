/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      The Eclipse Foundation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.core.service;


/**
 * @author David Green
 */
public class Platforms {
	
	protected java.util.List<String> platform = new java.util.ArrayList<String>();
	
	public Platforms() {
	}
	
	public java.util.List<String> getPlatform() {
		return platform;
	}
	
	public void setPlatform(java.util.List<String> platform) {
		this.platform = platform;
	}
	
}
