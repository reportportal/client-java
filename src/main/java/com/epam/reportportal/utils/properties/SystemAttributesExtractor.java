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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class SystemAttributesExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemAttributesExtractor.class);

	public static Set<ItemAttributesRQ> extract(final String resource) {

		return ofNullable(SystemAttributesExtractor.class.getClassLoader().getResource(resource)).map(url -> {
			Properties properties = new Properties();
			try (InputStream resourceStream = url.openStream()) {
				properties.load(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.warn("Unable to load system properties file");
			}
			return getAttributes(properties);
		}).orElseGet(Collections::emptySet);
	}

	private static Set<ItemAttributesRQ> getAttributes(final Properties properties) {
		Set<ItemAttributesRQ> attributes = Arrays.stream(DefaultSystemProperties.values())
				.filter(DefaultSystemProperties::isSystem)
				.map(defaultProperty -> convert(defaultProperty.getName(), properties, defaultProperty.getPropertyKeys()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());

		Arrays.stream(DefaultSystemProperties.values()).filter(defaultProperty -> !defaultProperty.isSystem()).forEach(defaultProperty -> {
			convert(defaultProperty.getName(), defaultProperty.getPropertyKeys()).ifPresent(attributes::add);
		});

		return attributes;
	}

	private static Optional<ItemAttributesRQ> convert(String attributeKey, Properties properties, String... propertyKeys) {
		Function<String, Optional<String>> propertyExtractor = getPropertyExtractor(properties);
		return extractAttribute(propertyExtractor, attributeKey, propertyKeys);
	}

	private static Optional<ItemAttributesRQ> convert(String attributeKey, String... propertyKeys) {
		Function<String, Optional<String>> propertyExtractor = getPropertyExtractor();
		return extractAttribute(propertyExtractor, attributeKey, propertyKeys);
	}

	private static Function<String, Optional<String>> getPropertyExtractor(Properties properties) {
		return key -> ofNullable(properties.getProperty(key));
	}

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
			return Optional.of(new ItemAttributesRQ(attributeKey, StringUtils.join(values, " ", true)));
		} else {
			return Optional.empty();
		}
	}

	private enum DefaultSystemProperties {
		OS("os", true, "os.name", "os.arch", "os.version"),
		AGENT("agent", false, "agent.name", "agent.version");

		private String name;
		private boolean system;
		private String[] propertyKeys;

		DefaultSystemProperties(String name, boolean system, String... propertyKeys) {
			this.name = name;
			this.system = system;
			this.propertyKeys = propertyKeys;
		}

		public String getName() {
			return name;
		}

		public boolean isSystem() {
			return system;
		}

		public String[] getPropertyKeys() {
			return propertyKeys;
		}
	}
}
