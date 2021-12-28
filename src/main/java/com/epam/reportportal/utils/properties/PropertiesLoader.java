/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.utils.properties;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.epam.reportportal.utils.properties.ListenerProperty.values;

/**
 * Load report portal launch start properties
 */
public class PropertiesLoader {

	public static final String INNER_PATH = "reportportal.properties";
	public static final Charset STANDARD_CHARSET = StandardCharsets.UTF_8;

	private final Supplier<Properties> propertiesSupplier;

	/**
	 * Loads properties from default location
	 *
	 * @return PropertiesLoader instance
	 * @see #INNER_PATH
	 */
	public static PropertiesLoader load() {
		return new PropertiesLoader(() -> {
			try {
				return loadProperties(INNER_PATH);
			} catch (IOException e) {
				throw new InternalReportPortalClientException("Unable to load properties", e);
			}
		});
	}

	/**
	 * Loads properties from specified location
	 *
	 * @param resource Path to resources in classpath
	 * @return PropertiesLoader instance
	 */
	public static PropertiesLoader load(final String resource) {
		return new PropertiesLoader(() -> {
			try {
				return loadProperties(resource);
			} catch (IOException e) {
				throw new InternalReportPortalClientException("Unable to load properties", e);
			}
		});
	}

	private PropertiesLoader(final Supplier<Properties> propertiesSupplier) {
		this.propertiesSupplier = new MemoizingSupplier<>(propertiesSupplier);
	}

	/**
	 * Get specified property loaded from properties file and reloaded from from
	 * environment variables.
	 *
	 * @param propertyName Name of property
	 * @return Property value or null
	 */
	public String getProperty(String propertyName) {
		return propertiesSupplier.get().getProperty(propertyName);
	}

	/**
	 * Get specified property
	 *
	 * @param propertyName Name of property
	 * @param defaultValue Default value
	 * @return Property value if exists or default value
	 */
	public String getProperty(ListenerProperty propertyName, String defaultValue) {
		final String value = propertiesSupplier.get().getProperty(propertyName.getPropertyName());
		return value != null ? value : defaultValue;
	}

	/**
	 * Get specified property
	 *
	 * @param propertyName Name of property
	 * @param defaultValue Default value
	 * @return Property value if exists or default value
	 */
	public boolean getPropertyAsBoolean(ListenerProperty propertyName, boolean defaultValue) {
		final String value = propertiesSupplier.get().getProperty(propertyName.getPropertyName());
		return null != value ? Boolean.parseBoolean(value) : defaultValue;
	}

	/**
	 * Get specified property
	 *
	 * @param propertyName Name of property
	 * @param defaultValue Default value
	 * @return property value if present, {@code defaultValue} otherwise
	 */
	public int getPropertyAsInt(ListenerProperty propertyName, int defaultValue) {
		final String value = propertiesSupplier.get().getProperty(propertyName.getPropertyName());
		return null != value ? Integer.parseInt(value) : defaultValue;
	}

	/**
	 * Get specified property
	 *
	 * @param propertyName Name of property
	 * @return Property value or null
	 */
	public String getProperty(ListenerProperty propertyName) {
		return propertiesSupplier.get().getProperty(propertyName.getPropertyName());
	}

	/**
	 * Get all properties loaded from properties file and reloaded from from
	 * environment variables.
	 *
	 * @return All properties
	 */
	public Properties getProperties() {
		return propertiesSupplier.get();
	}

	/**
	 * Try to load properties from file situated in the class path, and then
	 * reload existing parameters from environment variables
	 *
	 * @return loaded properties
	 * @throws IOException In case of IO error
	 */
	private static Properties loadProperties(String resource) throws IOException {
		Properties props = new Properties();
		Optional<URL> propertyFile = getResource(resource);
		if (propertyFile.isPresent()) {
			try (InputStream is = propertyFile.get().openStream()) {
				props.load(new InputStreamReader(is, STANDARD_CHARSET));
			}
		}
		overrideWith(props, System.getenv());
		overrideWith(props, System.getProperties());

		return props;
	}

	/**
	 * replace underscores with dots (dots are normally not allowed in spring boot variables, so like in spring boot,
	 * underscores can be used.
	 *
	 * @param overrides a property set to normalize
	 * @return the overrides without underscores and with dots.
	 */
	private static Map<String, String> normalizeOverrides(Map<?, ?> overrides) {
		return overrides.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().toString().toLowerCase().replace('_', '.'), e -> e.getValue().toString()));
	}

	/**
	 * Overrides properties from another source
	 *
	 * @param source    Properties to be overridden
	 * @param overrides Overrides
	 */
	@VisibleForTesting
	static void overrideWith(Properties source, Map<?, ?> overrides) {
		Map<String, String> overridesNormalized = normalizeOverrides(overrides);
		for (ListenerProperty listenerProperty : values()) {
			if (overridesNormalized.get(listenerProperty.getPropertyName()) != null) {
				source.setProperty(listenerProperty.getPropertyName(), overridesNormalized.get(listenerProperty.getPropertyName()));
			}
		}
	}

	/**
	 * Overrides properties from another source
	 *
	 * @param source    Properties to be overridden
	 * @param overrides Overrides
	 */
	@VisibleForTesting
	static void overrideWith(Properties source, Properties overrides) {
		overrideWith(source, (Map<Object, Object>) overrides);
	}

	private static Optional<URL> getResource(String resourceName) {
		ClassLoader loader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
				.orElse(PropertiesLoader.class.getClassLoader());
		return Optional.ofNullable(loader.getResource(resourceName));
	}
}
