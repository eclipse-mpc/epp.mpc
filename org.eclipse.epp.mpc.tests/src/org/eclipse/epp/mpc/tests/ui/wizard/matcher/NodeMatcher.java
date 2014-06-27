/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.wizard.matcher;

import java.util.regex.Pattern;

import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

public class NodeMatcher<T extends Widget> extends AbstractMatcher<T> {

	private final Matcher<? super INode> matcher;

	NodeMatcher(Matcher<? super INode> matcher) {
		this.matcher = matcher;
	}

	@Override
	protected boolean doMatch(Object obj) {
		if (obj instanceof Widget) {
			Widget w = (Widget) obj;
			Object data = w.getData();
			INode node = null;
			if (data instanceof CatalogItem) {
				data = ((CatalogItem) data).getData();
			}
			if (data instanceof INode) {
				node = (INode) data;
			}
			return node != null && matcher.matches(node);
		}
		return false;
	}

	public void describeTo(Description description) {
		description.appendText("with node matching "); //$NON-NLS-1$
		matcher.describeTo(description);
	}

	@Factory
	public static <T extends Widget> NodeMatcher<T> any() {
		return new NodeMatcher<T>(IsAnything.anything("Any node"));
	}

	@Factory
	public static <T extends Widget> NodeMatcher<T> withNode(Matcher<INode> matcher) {
		return new NodeMatcher<T>(matcher);
	}

	@Factory
	public static <T extends Widget> NodeMatcher<T> withId(String id) {
		return withNode(new NodeValueMatcher<String>("id", id) {

			@Override
			protected String getValue(INode item) {
				return item.getId();
			}
		});
	}

	@Factory
	public static <T extends Widget> NodeMatcher<T> withUrl(String url) {
		return withNode(new NodeValueMatcher<String>("url", url) {

			@Override
			protected String getValue(INode item) {
				return item.getUrl();
			}
		});
	}

	@Factory
	public static <T extends Widget> NodeMatcher<T> withName(String name) {
		return withNode(new NodeValueMatcher<String>("name", name) {

			@Override
			protected String getValue(INode item) {
				return item.getName();
			}
		});
	}

	@Factory
	public static <T extends Widget> NodeMatcher<T> withNameRegex(String name) {
		return withNode(new NodeValueMatcher<String>("name", name) {
			private final Pattern pattern = Pattern.compile(expected);
			@Override
			protected boolean doMatch(String expected, String actual) {
				return pattern.matcher(actual).matches();
			}

			@Override
			protected String getValue(INode item) {
				return item.getName();
			}
		});
	}

	private static abstract class NodeValueMatcher<T> extends AbstractMatcher<INode> {
		final String valueName;

		final T expected;

		public NodeValueMatcher(String valueName, T expected) {
			super();
			this.valueName = valueName;
			this.expected = expected;
		}

		public void describeTo(Description description) {
			description.appendText(valueName).appendText(" ");
			description.appendValue(expected);
		}

		@Override
		protected boolean doMatch(Object item) {
			return item instanceof INode && doMatch(expected, getValue((INode) item));
		}

		protected boolean doMatch(T expected, T actual) {
			return expected.equals(actual);
		}

		protected abstract T getValue(INode item);
	}
}
