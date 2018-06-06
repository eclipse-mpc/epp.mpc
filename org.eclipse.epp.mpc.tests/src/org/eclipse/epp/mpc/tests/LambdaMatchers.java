/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.SelfDescribing;

public class LambdaMatchers {

	private static final class PredicateMatcher<T> extends AbstractMatcher<T> {
		private final Predicate<? super T> predicate;

		public PredicateMatcher(Predicate<? super T> predicate) {
			super();
			this.predicate = predicate;
		}

		@Override
		public void describeTo(Description description) {
			if (predicate instanceof SelfDescribing) {
				((SelfDescribing) predicate).describeTo(description);
			} else {
				description.appendText("predicate ").appendValue(predicate);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		protected boolean doMatch(Object item) {
			return predicate.test((T) item);
		}
	}

	private static final class TransformedValueMatcher<S, T> extends AbstractMatcher<S> {
		private final Function<? super S, ? extends T> function;

		private final Matcher<? super T> matcher;

		public TransformedValueMatcher(Function<? super S, ? extends T> function, Matcher<? super T> matcher) {
			this.function = function;
			this.matcher = matcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("under ");
			if (function instanceof SelfDescribing) {
				SelfDescribing describingFunction = (SelfDescribing) function;
				describingFunction.describeTo(description);
			} else {
				description.appendValue(function);
			}
			matcher.describeTo(description);
		}

		@Override
		protected boolean doMatch(Object item) {
			T transformedItem = function.apply((S) item);
			return matcher.matches(transformedItem);
		}
	}

	private static final class OptionalFunction<S, T> implements Function<Optional<S>, Optional<T>> {
		private final Function<S, T> function;

		public OptionalFunction(Function<S, T> function) {
			super();
			this.function = function;
		}

		@Override
		public Optional<T> apply(Optional<S> value) {
			if (!value.isPresent()) {
				return Optional.empty();
			}
			T result = function.apply(value.get());
			return Optional.ofNullable(result);
		}
	}

	public static class OngoingTransformation<S, T> {
		private final Function<S, T> transformation;

		private OngoingTransformation(Function<S, T> transformation) {
			this.transformation = transformation;
		}

		public <V> OngoingTransformation<S, V> map(Function<? super T, V> f) {
			return new OngoingTransformation<>(transformation.andThen(f));
		}

		public Matcher<S> matches(Predicate<? super T> p) {
			return matches(new PredicateMatcher<>(p));
		}

		public Matcher<S> matches(Matcher<? super T> m) {
			return new TransformedValueMatcher<>(transformation, m);
		}

		public Matcher<S> matches(T value) {
			return new TransformedValueMatcher<>(transformation, Matchers.is(value));
		}
	}

	public static <T> Matcher<T> matches(Predicate<T> p) {
		return new PredicateMatcher<>(p);
	}

	public static <S, T> OngoingTransformation<S, T> map(Function<S, T> f) {
		return new OngoingTransformation<>(f);
	}
}
