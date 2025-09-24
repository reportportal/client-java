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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class SystemAttributesExtractorTest {

	private static final Map<String, Pattern> properties = new HashMap<>();

	@BeforeAll
	public static void initKeys() {
		properties.put("os", Pattern.compile("^.+\\|.+\\|.+$"));
		properties.put("jvm", Pattern.compile("^.+\\|.+\\|.+$"));
		properties.put("agent", Pattern.compile("^test-agent\\|test-1\\.0$"));
		properties.put("client", Pattern.compile("^java-client\\|test-1\\.0$"));
	}

	@Test
	public void nullSafeTestForString() {
		SystemAttributesExtractor.extract((String) null, null);
	}

	@Test
	public void nullSafeTestForPath() {
		SystemAttributesExtractor.extract(null);
	}

	@Test
	public void testFromResource() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract(
				"agent-test.properties",
				SystemAttributesExtractorTest.class.getClassLoader()
		);
		assertThat(attributes, hasSize(3));

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ osAttribute = attributesList.get(0);
		Pattern osPattern = getPattern(osAttribute);
		assertThat(osPattern, notNullValue());
		assertThat(osAttribute.getValue(), matchesRegex(osPattern));

		ItemAttributesRQ jvmAttribute = attributesList.get(1);
		Pattern jvmPattern = getPattern(jvmAttribute);
		assertThat(jvmPattern, notNullValue());
		assertThat(jvmAttribute.getValue(), matchesRegex(jvmPattern));

		ItemAttributesRQ agentAttribute = attributesList.get(2);
		Pattern agentPattern = getPattern(agentAttribute);
		assertThat(agentPattern, notNullValue());
		assertThat(agentAttribute.getValue(), matchesRegex(agentPattern));
	}

	@Test
	public void testFromPath() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract(Paths.get("./src/test/resources/agent-test.properties"));
		assertThat(attributes, hasSize(3));

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ firstAttribute = attributesList.get(0);
		Pattern osPattern = getPattern(firstAttribute);
		assertThat(osPattern, notNullValue());
		assertThat(firstAttribute.getValue(), matchesRegex(osPattern));

		ItemAttributesRQ jvmAttribute = attributesList.get(1);
		Pattern jvmPattern = getPattern(jvmAttribute);
		assertThat(jvmPattern, notNullValue());
		assertThat(jvmAttribute.getValue(), matchesRegex(jvmPattern));

		ItemAttributesRQ agentAttribute = attributesList.get(2);
		Pattern agentPattern = getPattern(agentAttribute);
		assertThat(agentPattern, notNullValue());
		assertThat(agentAttribute.getValue(), matchesRegex(agentPattern));
	}

	@Test
	public void testFromResourceClientProperties() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract(
				"client-test.properties",
				SystemAttributesExtractorTest.class.getClassLoader(),
				ClientProperties.values()
		);
		assertThat(attributes, hasSize(1));

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ clientAttribute = attributesList.get(0);

		assertEquals("client", clientAttribute.getKey());
		Pattern osPattern = getPattern(clientAttribute);
		assertThat(osPattern, notNullValue());
		assertThat(clientAttribute.getValue(), matchesRegex(osPattern));
	}

	@Test
	public void testFromPathClientProperties() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract(
				Paths.get("./src/test/resources/client-test.properties"),
				ClientProperties.values()
		);
		assertThat(attributes, hasSize(1));

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ clientAttribute = attributesList.get(0);
		assertEquals("client", clientAttribute.getKey());
		Pattern osPattern = getPattern(clientAttribute);
		assertThat(osPattern, notNullValue());
		assertThat(clientAttribute.getValue(), matchesRegex(osPattern));
	}

	private Pattern getPattern(ItemAttributesRQ attribute) {
		return properties.get(attribute.getKey());
	}

}