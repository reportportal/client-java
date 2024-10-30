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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

	public static final String ATTRIBUTE_VALUE_SEPARATOR = "|";

	private SystemAttributesExtractor() {
		//static only
	}

	/**
	 * Loads properties from the specified location
	 *
	 * @param resource Path to the resource in classpath
	 * @param loader   context class loader, which is used by a specific agent implementation
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> extract(final String resource, final ClassLoader loader) {
		Set<ItemAttributesRQ> attributes = getInternalAttributes();
		Properties properties = loadProperties(resource, loader);
		attributes.addAll(getExternalAttributes(properties, DefaultProperties.values()));
		return attributes;
	}

	/**
	 * Loads properties from the specified location
	 *
	 * @param resource        Path to the resource in classpath
	 * @param loader          context class loader, which is used by a specific agent implementation
	 * @param propertyHolders an array of specific properties we want to extract
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> extract(final String resource, final ClassLoader loader, final PropertyHolder... propertyHolders) {
		Properties properties = loadProperties(resource, loader);
		return getExternalAttributes(properties, propertyHolders);
	}

	private static Properties loadProperties(final String resource, final ClassLoader loader) {
		Properties properties = new Properties();
		ofNullable(loader).flatMap(l -> ofNullable(resource).flatMap(res -> ofNullable(l.getResourceAsStream(res))))
				.ifPresent(resStream -> {
					try (InputStreamReader inputStreamReader = new InputStreamReader(resStream, StandardCharsets.UTF_8)) {
						properties.load(inputStreamReader);
					} catch (IOException e) {
						LOGGER.warn("Unable to load system properties file");
					}
				});
		return properties;
	}

	/**
	 * Loads properties from the specified location
	 *
	 * @param path Path to the resource the file system
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> extract(final Path path) {
		Set<ItemAttributesRQ> attributes = getInternalAttributes();
		Properties properties = loadProperties(path);
		attributes.addAll(getExternalAttributes(properties, DefaultProperties.values()));
		return attributes;
	}

	/**
	 * Loads properties from the specified location
	 *
	 * @param path            Path to the resource the file system
	 * @param propertyHolders an array of specific properties we want to extract
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> extract(final Path path, final PropertyHolder... propertyHolders) {
		Properties properties = loadProperties(path);
		return getExternalAttributes(properties, propertyHolders);
	}

	private static Properties loadProperties(final Path path) {
		Properties properties = new Properties();
		if (path != null) {
			File file = path.toFile();
			if (file.exists()) {
				try (InputStreamReader inputStreamReader = new InputStreamReader(
						Files.newInputStream(file.toPath()),
						StandardCharsets.UTF_8
				)) {
					properties.load(inputStreamReader);
				} catch (IOException e) {
					LOGGER.warn("Unable to load system properties file");
				}
			}
		}
		return properties;
	}

	private static Set<ItemAttributesRQ> getInternalAttributes() {
		return Arrays.stream(DefaultProperties.values())
				.filter(DefaultProperties::isInternal)
				.map(defaultProperty -> convert(defaultProperty.getName(), defaultProperty.getPropertyKeys()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
	}

	private static Set<ItemAttributesRQ> getExternalAttributes(final Properties externalAttributes,
			final PropertyHolder... propertyHolders) {
		return Arrays.stream(propertyHolders)
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
			return Optional.of(new ItemAttributesRQ(attributeKey, String.join(ATTRIBUTE_VALUE_SEPARATOR, values), true));
		} else {
			return Optional.empty();
		}
	}
}
