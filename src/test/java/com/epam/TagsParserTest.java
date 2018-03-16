/*
 * Copyright (C) 2018 EPAM Systems
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
package com.epam;

import com.epam.reportportal.utils.TagsParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TagsParserTest {
	@Test
	public void testNull() {
		Map<String, String> tags = TagsParser.findAllTags(null);
		Assert.assertNull(tags);
	}

	@Test
	public void testOnlyBuild() {
		String tagsString = "BuIld:123445566-2343ds";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(1, tags.size());
		Assert.assertEquals("123445566-2343ds", tags.get("build"));
	}

	@Test
	public void testBuildTagInStart() {
		String tagsString = "BuIld:123445566-2343ds;tag11;tag2;tag3";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(4, tags.size());
		Assert.assertEquals("123445566-2343ds", tags.get("build"));
		Assert.assertEquals("tag11", tags.get("tag11"));
		Assert.assertEquals("tag2", tags.get("tag2"));
		Assert.assertEquals("tag3", tags.get("tag3"));
	}

	@Test
	public void testFullTagInFinish() {
		String tagsString = "tag11;tag22;0;12345;BuIld:123445566-2343ds";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(5, tags.size());
		Assert.assertEquals("123445566-2343ds", tags.get("build"));
		Assert.assertEquals("tag11", tags.get("tag11"));
		Assert.assertEquals("tag22", tags.get("tag22"));
		Assert.assertEquals("0", tags.get("0"));
		Assert.assertEquals("12345", tags.get("12345"));
	}

	@Test
	public void testFullTagInMiddle() {
		String tagsString = "tag11;BuIld:123445566-2343ds;0";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(3, tags.size());
		Assert.assertEquals("123445566-2343ds", tags.get("build"));
		Assert.assertEquals("tag11", tags.get("tag11"));
		Assert.assertEquals("0", tags.get("0"));
	}

	@Test
	public void testTagSpaces() {
		String tagsString = " ;;BuIld:123445566-2343ds; ;tag; ; ;; ";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(2, tags.size());
		Assert.assertEquals("123445566-2343ds", tags.get("build"));
		Assert.assertEquals("tag", tags.get("tag"));
	}

	@Test
	public void testEmptyBuild() {
		String tagsString = "BUILD:;tag;BuIld 123:123445566-2343ds;";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(3, tags.size());
		Assert.assertEquals("", tags.get("build"));
		Assert.assertEquals("tag", tags.get("tag"));
		Assert.assertEquals("BuIld 123:123445566-2343ds", "BuIld 123:123445566-2343ds");
	}

	@Test
	public void testEmptyTags() {
		String tagsString = " ";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(0, tags.size());
	}

	@Test
	public void testMissedBuildTag() {
		String tagsString = "TAG1;TAG#";
		Map<String, String> tags = TagsParser.findAllTags(tagsString);
		Assert.assertEquals(2, tags.size());
		Assert.assertEquals("TAG1", tags.get("TAG1"));
		Assert.assertEquals("TAG#", tags.get("TAG#"));
	}

}
