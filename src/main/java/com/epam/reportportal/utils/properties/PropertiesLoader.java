/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.utils.properties;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.io.Closer;
import com.google.common.io.Resources;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import static com.epam.reportportal.utils.properties.ListenerProperty.values;
import static com.google.common.base.Suppliers.memoize;

/**
 * Load report portal launch start properties
 */
public class PropertiesLoader {

	public static final String INNER_PATH = "reportportal.properties";
	public static final String PATH = "./reportportal.properties";

	private Supplier<Properties> propertiesSupplier;

	/**
	 * Loads properties from default location
	 *
	 * @return PropertiesLoader instance
	 * @see #INNER_PATH
	 */
	public static PropertiesLoader load() {
		return new PropertiesLoader(new Supplier<Properties>() {
			@Override
			public Properties get() {
				try {
					return loadProperties(INNER_PATH);
				} catch (IOException e) {
					throw new InternalReportPortalClientException("Unable to load properties", e);
				}
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
		return new PropertiesLoader(new Supplier<Properties>() {
			@Override
			public Properties get() {
				try {
					return loadProperties(resource);
				} catch (IOException e) {
					throw new InternalReportPortalClientException("Unable to load properties", e);
				}
			}
		});
	}

	private PropertiesLoader(Supplier<Properties> propertiesSupplier) {
		this.propertiesSupplier = memoize(propertiesSupplier);
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
		return null != value ? Boolean.valueOf(value) : defaultValue;
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
	 * Overrides properties with provided values
	 *
	 * @param overrides Values to overrides
	 */
	public void overrideWith(Properties overrides) {
		overrideWith(propertiesSupplier.get(), overrides);
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
			props.load(Resources.asCharSource(propertyFile.get(), Charsets.UTF_8).openBufferedStream());
		}
		overrideWith(props, System.getProperties());
		overrideWith(props, System.getenv());

		return props;
	}

	/**
	 * Current version of agents should load properties only from properties
	 * file on classpath
	 */
	@SuppressWarnings("unused")
	@Deprecated()
	private static Properties loadFromFile() throws IOException {
		Properties props = new Properties();
		File propertiesFile = new File(PATH);

		Closer closer = Closer.create();
		try {
			InputStream is = propertiesFile.exists() ?
					new FileInputStream(propertiesFile) :
					PropertiesLoader.class.getResourceAsStream(INNER_PATH);
			closer.register(is);
			InputStreamReader isr = new InputStreamReader(is, Charsets.UTF_8);
			closer.register(isr);
			props.load(isr);
		} catch (Throwable e) {
			throw closer.rethrow(e);
		} finally {
			closer.close();
		}
		return props;
	}

	/**
	 * Validates properties
	 */
	public void validate() {
		validateProperties(this.getProperties());
	}

	/**
	 * Validate required properties presence
	 *
	 * @param properties Properties to be validated
	 */
	private static void validateProperties(Properties properties) {
		for (ListenerProperty listenerProperty : values()) {
			if (listenerProperty.isRequired() && properties.getProperty(listenerProperty.getPropertyName()) == null) {
				throw new IllegalArgumentException("Property '" + listenerProperty.getPropertyName() + "' should not be null.");
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
	static void overrideWith(Properties source, Map<String, String> overrides) {
		for (ListenerProperty listenerProperty : values()) {
			if (overrides.get(listenerProperty.getPropertyName()) != null) {
				source.setProperty(listenerProperty.getPropertyName(), overrides.get(listenerProperty.getPropertyName()));
			}
		}
	}

	/**
	 * Overrides properties from another source
	 *
	 * @param source    Properties to be overridden
	 * @param overrides Overrides
	 */
	@SuppressWarnings("unchecked")
	@VisibleForTesting
	static void overrideWith(Properties source, Properties overrides) {
		overrideWith(source, ((Map) overrides));
	}

	private static Optional<URL> getResource(String resourceName) {
		ClassLoader loader = MoreObjects.firstNonNull(Thread.currentThread().getContextClassLoader(),
				PropertiesLoader.class.getClassLoader()
		);
		return Optional.fromNullable(loader.getResource(resourceName));
	}

}
