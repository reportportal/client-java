/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam;

import java.util.Map;

import com.epam.reportportal.utils.TagsParser;
import org.junit.Assert;
import org.junit.Test;

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
