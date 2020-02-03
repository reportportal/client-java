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

	private static final Map<String, Pattern> agentProperties = new HashMap<>();
	private static final Map<String, Pattern> systemProperties = new HashMap<>();

	@BeforeClass
	public static void initKeys() {
		systemProperties.put("os", Pattern.compile("^.+\\|.+\\|.+$"));
		agentProperties.put("agent", Pattern.compile("^test-agent\\|test-1\\.0$"));
	}

	@Test
	public void nullSafeTestForString() {
		SystemAttributesExtractor.extract((String) null);
	}

	@Test
	public void nullSafeTestForPath() {
		SystemAttributesExtractor.extract((Path) null);
	}

	@Test
	public void testFromResource() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract("agent-test.properties");
		Assert.assertEquals(2, attributes.size());

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ firstAttribute = attributesList.get(0);
		Pattern firstPattern = getPattern(firstAttribute);
		Assert.assertNotNull(firstPattern);
		Assert.assertTrue(firstPattern.matcher(firstAttribute.getValue()).matches());

		ItemAttributesRQ secondAttribute = attributesList.get(1);
		Pattern secondPattern = getPattern(secondAttribute);
		Assert.assertNotNull(secondPattern);
		Assert.assertTrue(secondPattern.matcher(secondAttribute.getValue()).matches());
	}

	@Test
	public void testFromPath() {
		Set<ItemAttributesRQ> attributes = SystemAttributesExtractor.extract(Paths.get("./src/test/resources/agent-test.properties"));
		Assert.assertEquals(2, attributes.size());

		ArrayList<ItemAttributesRQ> attributesList = new ArrayList<>(attributes);

		ItemAttributesRQ firstAttribute = attributesList.get(0);
		Pattern firstPattern = getPattern(firstAttribute);
		Assert.assertNotNull(firstPattern);
		Assert.assertTrue(firstPattern.matcher(firstAttribute.getValue()).matches());

		ItemAttributesRQ secondAttribute = attributesList.get(1);
		Pattern secondPattern = getPattern(secondAttribute);
		Assert.assertNotNull(secondPattern);
		Assert.assertTrue(secondPattern.matcher(secondAttribute.getValue()).matches());
	}

	private Pattern getPattern(ItemAttributesRQ attribute) {
		return ofNullable(systemProperties.get(attribute.getKey())).orElseGet(() -> ofNullable(agentProperties.get(attribute.getKey())).orElse(
				null));

	}

}