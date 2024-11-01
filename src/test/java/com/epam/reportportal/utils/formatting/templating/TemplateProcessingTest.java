/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils.formatting.templating;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.utils.ParameterUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TemplateProcessingTest {

	@Step
	public void annotationProvider() {

	}

	@ParameterizedTest
	@MethodSource("data")
	public void retrieveValue(String template, String expected) throws NoSuchMethodException, NoSuchFieldException {

		Outer.Inner someObject = createInnerObject();

		Step stepAnnotation = TemplateProcessingTest.class.getDeclaredMethod("annotationProvider").getAnnotation(Step.class);
		TemplateConfiguration templateConfig = new TemplateConfiguration(stepAnnotation.config());
		String replacement = TemplateProcessing.retrieveValue(templateConfig, 1, template.split("\\."), someObject);

		assertThat(replacement, equalTo(expected));
	}

	public static Object[][] data() {
		return new Object[][]{{"someObject.outerName", "outer"}, {"someObject.innerName", "inner"},
				{"someObject.innerStrings", "[firstInner, secondInner, thirdInner]"}, {"someObject", "INNER"},
				{"someObject.outers", "[OUTER]"},
				{"someObject.outers.outerStrings", "[[{first, second, third}, {fourth, fifth, sixth}]]"},
				{"someObject.outers.outerName", "[outer]"}, {"someObject.innerNullString", ParameterUtils.NULL_VALUE},
				{"someObject.innerNullList", ParameterUtils.NULL_VALUE}};
	}

	private Outer.Inner createInnerObject() {

		final String[] strings = {"first", "second", "third"};
		final String[] moreStrings = {"fourth", "fifth", "sixth"};
		List<String[]> outerStrings = new ArrayList<String[]>() {
			{
				add(strings);
				add(moreStrings);
			}
		};

		final String outerName = "outer";
		final String innerName = "inner";

		List<String> innerStrings = getInnerStrings();
		return new Outer.Inner(outerName, outerStrings, innerName, innerStrings,
				Collections.singletonList(new Outer(outerName, outerStrings)));
	}

	private static List<String> getInnerStrings() {
		return Arrays.asList("firstInner", "secondInner", "thirdInner");
	}

	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	private static class Outer {

		private final String outerName;

		private final List<String[]> outerStrings;

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

			private final List<String> innerStrings;

			private final List<Outer> outers;

			private String innerNullString;
			private List<String> innerNullList;

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