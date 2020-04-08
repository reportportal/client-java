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

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepTemplateUtilsTest {

	@Step
	public void annotationProvider() {

	}

	@ParameterizedTest
	@MethodSource("data")
	public void retrieveValue(String template, String expected) throws NoSuchMethodException, NoSuchFieldException {

		Outer.Inner someObject = createInnerObject();

		Step stepAnnotation = StepTemplateUtilsTest.class.getDeclaredMethod("annotationProvider").getAnnotation(Step.class);
		StepTemplateConfig templateConfig = stepAnnotation.templateConfig();
		String replacement = StepTemplateUtils.retrieveValue(templateConfig, 1, template.split("\\."), someObject);

		assertThat(replacement, equalTo(expected));
	}

	public static Object[][] data() {
		return new Object[][] { { "someObject.outerName", "outer" }, { "someObject.innerName", "inner" },
				{ "someObject.innerStrings", "[firstInner, secondInner, thirdInner]" }, { "someObject", "INNER" },
				{ "someObject.outers", "[OUTER]" },
				{ "someObject.outers.outerStrings", "[[{first, second, third}, {fourth, fifth, sixth}]]" },
				{ "someObject.outers.outerName", "[outer]" } };
	}

	private Outer.Inner createInnerObject() {

		final String[] strings = { "first", "second", "third" };
		final String[] moreStrings = { "fourth", "fifth", "sixth" };
		List<String[]> outerStrings = new ArrayList<String[]>() {
			{
				add(strings);
				add(moreStrings);
			}
		};

		final String outerName = "outer";
		final String innerName = "inner";

		List<String> innerStrings = getInnerStrings();
		return new Outer.Inner(outerName, outerStrings, innerName, innerStrings, Lists.newArrayList(new Outer(outerName, outerStrings)));
	}

	private static List<String> getInnerStrings() {
		return Lists.newArrayList("firstInner", "secondInner", "thirdInner");
	}

	private static class Outer {

		private final String outerName;

		private List<String[]> outerStrings;

		public Outer(String outerName, List<String[]> outerStrings) {
			this.outerName = outerName;
			this.outerStrings = outerStrings;
		}

		@Override
		public String toString() {
			return "OUTER";
		}

		private static class Inner extends Outer {
			private final String innerName;

			private List<String> innerStrings;

			private List<Outer> outers;

			public Inner(String outerName, List<String[]> outerStrings, String innerName, List<String> innerStrings, List<Outer> outers) {
				super(outerName, outerStrings);
				this.innerName = innerName;
				this.innerStrings = innerStrings;
				this.outers = outers;
			}

			@Override
			public String toString() {
				return "INNER";
			}
		}

	}

}