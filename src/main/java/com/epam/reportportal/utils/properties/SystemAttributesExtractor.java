/*
 * Copyright 2020 EPAM Systems
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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Util for retrieving properties from `System` env variables and provided `resource` and converting them to the {@link ItemAttributesRQ}
 * with {@link ItemAttributesRQ#isSystem()}='true'
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class SystemAttributesExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemAttributesExtractor.class);

	private static final String ATTRIBUTE_VALUE_SEPARATOR = "|";

	private SystemAttributesExtractor() {
		//static only
	}

	/**
	 * Loads properties from the specified location
	 *
	 * @param resource Path to the resource in classpath
	 * @param loader context class loader, which is used by a specific agent implementation
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> extract(final String resource, final ClassLoader loader) {
		Set<ItemAttributesRQ> attributes = getInternalAttributes();

		Properties properties = new Properties();
		ofNullable(loader).flatMap(l -> ofNullable(resource).flatMap(res -> ofNullable(l.getResource(res))))
				.ifPresent(url -> {
					try (InputStreamReader inputStreamReader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
						properties.load(inputStreamReader);
					} catch (IOException e) {
						LOGGER.warn("Unable to load system properties file");
					}
		});

		attributes.addAll(getExternalAttributes(properties));
		return attributes;
	}

	/**
	 * Loads properties from the specified location
	 *
	 * @param path Path to the resource the file system
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> extract(final Path path) {
		Set<ItemAttributesRQ> attributes = getInternalAttributes();

		Properties properties = new Properties();
		ofNullable(path).ifPresent(p -> {
			File file = p.toFile();
			if (file.exists()) {
				try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
					properties.load(inputStreamReader);
				} catch (IOException e) {
					LOGGER.warn("Unable to load system properties file");
				}
			}
		});

		attributes.addAll(getExternalAttributes(properties));
		return attributes;
	}

	private static Set<ItemAttributesRQ> getInternalAttributes() {
		return Arrays.stream(DefaultProperties.values())
				.filter(DefaultProperties::isInternal)
				.map(defaultProperty -> convert(defaultProperty.getName(), defaultProperty.getPropertyKeys()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
	}

	private static Set<ItemAttributesRQ> getExternalAttributes(final Properties externalAttributes) {
		return Arrays.stream(DefaultProperties.values())
				.filter(defaultProperties -> !defaultProperties.isInternal())
				.map(defaultProperty -> convert(defaultProperty.getName(), externalAttributes, defaultProperty.getPropertyKeys()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());

	}

	private static Optional<ItemAttributesRQ> convert(String attributeKey, Properties properties, String... propertyKeys) {
		Function<String, Optional<String>> propertyExtractor = getPropertyExtractor(properties);
		return extractAttribute(propertyExtractor, attributeKey, propertyKeys);
	}

	private static Optional<ItemAttributesRQ> convert(String attributeKey, String... propertyKeys) {
		Function<String, Optional<String>> propertyExtractor = getPropertyExtractor();
		return extractAttribute(propertyExtractor, attributeKey, propertyKeys);
	}

	/**
	 * Function for loading properties from the {@link Properties}
	 *
	 * @param properties {@link Properties}
	 * @return {@link Function} that retrieves value from the `properties` by provided String `key`
	 */
	private static Function<String, Optional<String>> getPropertyExtractor(Properties properties) {
		return key -> ofNullable(properties.getProperty(key));
	}

	/**
	 * Function for loading properties from the {@link System}
	 *
	 * @return {@link Function} that retrieves value from the `System` by provided String `key`
	 */
	private static Function<String, Optional<String>> getPropertyExtractor() {
		return key -> ofNullable(System.getProperty(key));
	}

	private static Optional<ItemAttributesRQ> extractAttribute(Function<String, Optional<String>> propertyExtractor, String attributeKey,
			String... propertyKeys) {
		List<String> values = Arrays.stream(propertyKeys)
				.map(propertyExtractor)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
		if (!values.isEmpty()) {
			return Optional.of(new ItemAttributesRQ(attributeKey, StringUtils.join(values, ATTRIBUTE_VALUE_SEPARATOR), true));
		} else {
			return Optional.empty();
		}
	}

	private enum DefaultProperties {
		OS("os", true, "os.name", "os.arch", "os.version"),
		JVM("jvm", true, "java.vm.name", "java.version", "java.class.version"),
		AGENT("agent", false, "agent.name", "agent.version");

		private String name;
		private boolean internal;
		private String[] propertyKeys;

		DefaultProperties(String name, boolean internal, String... propertyKeys) {
			this.name = name;
			this.internal = internal;
			this.propertyKeys = propertyKeys;
		}

		public String getName() {
			return name;
		}

		public boolean isInternal() {
			return internal;
		}

		public String[] getPropertyKeys() {
			return propertyKeys;
		}
	}
}
