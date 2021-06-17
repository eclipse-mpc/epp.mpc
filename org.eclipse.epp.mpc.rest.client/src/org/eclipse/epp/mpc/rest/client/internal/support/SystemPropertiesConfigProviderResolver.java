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
package org.eclipse.epp.mpc.rest.client.internal.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

public class SystemPropertiesConfigProviderResolver extends ConfigProviderResolver {

	private static final Config SYSTEM_PROPERIES_CONFIG = new Config() {

		@Override
		public <T> T getValue(String propertyName, Class<T> propertyType) {
			Object value = System.getProperty(propertyName);
			if (value == null) {
				throw new NoSuchElementException();
			} else if (propertyType.isInstance(value)) {
				return propertyType.cast(value);
			} else if (value instanceof String) {
				Optional<Method> parseMethod = Arrays.stream(propertyType.getMethods())
						.filter(m -> m.getParameterCount() == 1 && m.getName().startsWith("parse")
						&& Modifier.isStatic(m.getModifiers())
						&& propertyType.isAssignableFrom(m.getReturnType())
						&& String.class.isAssignableFrom(m.getParameters()[0].getType()))
						.findFirst();
				if (parseMethod.isPresent()) {
					try {
						return propertyType.cast(parseMethod.get().invoke(null, value));
					} catch (Exception e) {
						throw new IllegalArgumentException(e);
					}
				}
				try {
					Constructor<T> ctor = propertyType.getConstructor(String.class);
					return ctor.newInstance(value);
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
			}
			throw new IllegalArgumentException(String.valueOf(value));
		}

		@Override
		public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
			try {
				return Optional.of(getValue(propertyName, propertyType));
			} catch (Exception ex) {
				return Optional.empty();
			}
		}

		@Override
		public Iterable<String> getPropertyNames() {
			return System.getProperties().stringPropertyNames();
		}

		@Override
		public Iterable<ConfigSource> getConfigSources() {
			return Collections.emptyList();
		}

	};

	private static final ConfigBuilder SYSTEM_PROPERTIES_CONFIG_BUILDER = new ConfigBuilder() {

		@Override
		public ConfigBuilder withSources(ConfigSource... sources) {
			// ignore
			return this;
		}

		@Override
		public ConfigBuilder withConverters(Converter<?>... converters) {
			// ignore
			return this;
		}

		@Override
		public <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
			// ignore
			return this;
		}

		@Override
		public ConfigBuilder forClassLoader(ClassLoader loader) {
			// ignore
			return this;
		}

		@Override
		public Config build() {
			// ignore
			return SYSTEM_PROPERIES_CONFIG;
		}

		@Override
		public ConfigBuilder addDiscoveredSources() {
			// ignore
			return this;
		}

		@Override
		public ConfigBuilder addDiscoveredConverters() {
			// ignore
			return this;
		}

		@Override
		public ConfigBuilder addDefaultSources() {
			// ignore
			return this;
		}
	};

	public static final SystemPropertiesConfigProviderResolver INSTANCE = new SystemPropertiesConfigProviderResolver();

	@Override
	public Config getConfig() {
		return SYSTEM_PROPERIES_CONFIG;
	}

	@Override
	public Config getConfig(ClassLoader loader) {
		return SYSTEM_PROPERIES_CONFIG;
	}

	@Override
	public ConfigBuilder getBuilder() {
		return SYSTEM_PROPERTIES_CONFIG_BUILDER;
	}

	@Override
	public void registerConfig(Config config, ClassLoader classLoader) {
		// ignore

	}

	@Override
	public void releaseConfig(Config config) {
		// ignore

	}

}
