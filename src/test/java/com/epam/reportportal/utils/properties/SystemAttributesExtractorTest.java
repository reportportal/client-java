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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class SystemAttributesExtractorTest {

	private static final Map<String, Pattern> properties = new HashMap<>();

	@BeforeClass
	public static void initKeys() {
		properties.put("os", Pattern.compile("^.+\\|.+\\|.+$"));
		properties.put("jvm", Pattern.compile("^.+\\|.+\\|.+$"));
		properties.put("agent", Pattern.compile("^test-agent\\|test-1\\.0$"));
	}

	@Test
	public void nullSafeTestForString() {
		SystemAttributesExtractor.extract((String) null, null);
	}

	@Test
	public void nullSafeTestForPath() {
		SystemAttributesExtractor.extract((Path) null);
	}

	@Test
	public void testFromResource() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract("agent-test.properties", SystemAttributesExtractorTest.class.getClassLoader());
		Assert.assertEquals(3, attributes.size());

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ osAttribute = attributesList.get(0);
		Pattern osPattern = getPattern(osAttribute);
		Assert.assertNotNull(osPattern);
		Assert.assertTrue(osPattern.matcher(osAttribute.getValue()).matches());

		ItemAttributesRQ jvmAttribute = attributesList.get(1);
		Pattern jvmPattern = getPattern(jvmAttribute);
		Assert.assertNotNull(jvmPattern);
		Assert.assertTrue(jvmPattern.matcher(jvmAttribute.getValue()).matches());

		ItemAttributesRQ agentAttribute = attributesList.get(2);
		Pattern agentPattern = getPattern(agentAttribute);
		Assert.assertNotNull(agentPattern);
		Assert.assertTrue(agentPattern.matcher(agentAttribute.getValue()).matches());
	}

	@Test
	public void testFromPath() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract(Paths.get("./src/test/resources/agent-test.properties"));
		Assert.assertEquals(3, attributes.size());

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ firstAttribute = attributesList.get(0);
		Pattern osPattern = getPattern(firstAttribute);
		Assert.assertNotNull(osPattern);
		Assert.assertTrue(osPattern.matcher(firstAttribute.getValue()).matches());

		ItemAttributesRQ jvmAttribute = attributesList.get(1);
		Pattern jvmPattern = getPattern(jvmAttribute);
		Assert.assertNotNull(jvmPattern);
		Assert.assertTrue(jvmPattern.matcher(jvmAttribute.getValue()).matches());

		ItemAttributesRQ agentAttribute = attributesList.get(2);
		Pattern agentPattern = getPattern(agentAttribute);
		Assert.assertNotNull(agentPattern);
		Assert.assertTrue(agentPattern.matcher(agentAttribute.getValue()).matches());
	}

	private Pattern getPattern(ItemAttributesRQ attribute) {
		return ofNullable(properties.get(attribute.getKey())).orElse(null);

	}

}