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
package com.epam.reportportal.utils;

import com.epam.reportportal.utils.AttributeParser;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class StringAttributeParserTest {
	@Test
	public void testNull() {
		Set<ItemAttributesRQ> itemAttributeResources = AttributeParser.parseAsSet(null);
		Assert.assertThat(itemAttributeResources, Matchers.empty());
	}

	@Test
	public void testOnlyBuild() {
		String attributesString = "BuIld:123445566-2343ds";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(1, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ("BuIld", "123445566-2343ds")));
	}

	@Test
	public void testAttributesWithoutKey() {
		String attributesString = "BuIld:123445566-2343ds;tag11";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(2, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ("BuIld", "123445566-2343ds")));
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ(null, "tag11")));
	}

	@Test
	public void testIncorrectAttribute() {
		String attributesString = "BuIld:123445566-2343ds;0:ff:fs";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(1, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ("BuIld", "123445566-2343ds")));
	}

	@Test
	public void testWithSpaces() {
		String attributesString = " ;;BuIld:123445566-2343ds; ;tag; ; ;; ";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(2, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ("BuIld", "123445566-2343ds")));
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ(null, "tag")));
	}

	@Test
	public void testEmptyBuild() {
		String attributesString = "BUILD:;tag;BuIld 123:123445566-2343ds;";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(3, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ("BuIld 123", "123445566-2343ds")));
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ(null, "BUILD")));
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ(null, "tag")));
	}

	@Test
	public void testEmpty() {
		String attributesString = " ";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(0, attributes.size());
	}

	@Test
	public void testEmptyKey() {
		String attributesString = ":BUILD;tag;";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(2, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ(null, "BUILD")));
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ(null, "tag")));
	}

	@Test
	public void testMissedBuildTag() {
		String attributesString = "TAG1;:;";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(1, attributes.size());
	}

	@Test
	public void testIgnoreDuplicates() {
		String attributesString = "key:value;key:value";
		Set<ItemAttributesRQ> attributes = AttributeParser.parseAsSet(attributesString);
		Assert.assertEquals(1, attributes.size());
		Assert.assertTrue(attributes.contains(new ItemAttributesRQ("key", "value")));
	}

}
