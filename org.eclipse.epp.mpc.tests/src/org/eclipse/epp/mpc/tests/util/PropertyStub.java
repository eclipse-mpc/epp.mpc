/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.util;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PropertyStub<T> {
	private final Class<T> type;

	private T value;

	public PropertyStub(Class<T> type) {
		super();
		this.type = type;
	}

	private final Answer<Void> setterAnswer = new Answer<Void>() {

		@Override
		public Void answer(InvocationOnMock invocation) throws Throwable {
			value = type.cast(invocation.getArguments()[0]);
			return null;
		}
	};

	private final Answer<T> getterAnswer = invocation -> value;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public Answer<Void> getSetterAnswer() {
		return setterAnswer;
	}

	public Answer<T> getGetterAnswer() {
		return getterAnswer;
	}

	public static <X, T> X mock(X mock, Class<T> type, T getterCall) {
		PropertyStub<T> propertyStub = new PropertyStub<>(type);
		Mockito.when(getterCall).thenAnswer(propertyStub.getGetterAnswer());
		return Mockito.doAnswer(propertyStub.getSetterAnswer()).when(mock);
	}
}
