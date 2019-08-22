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
package com.epam.reportportal.utils.markdown;

import org.junit.Assert;
import org.junit.Test;

import static com.epam.reportportal.utils.markdown.MarkdownUtils.asCode;

/**
 * @author Andrei Varabyeu
 */
public class MarkdownUtilsTest {
	@Test
	public void asMarkdown() throws Exception {
		Assert.assertEquals("Incorrect markdown prefix", "!!!MARKDOWN_MODE!!!hello", MarkdownUtils.asMarkdown("hello"));
	}

	@Test
	public void toMarkdownScript() throws Exception {
		Assert.assertEquals("Incorrect markdown prefix", "!!!MARKDOWN_MODE!!!```groovy\nhello\n```", asCode("groovy", "hello"));
	}

}