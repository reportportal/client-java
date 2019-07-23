package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import com.google.common.collect.Lists;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@RunWith(DataProviderRunner.class)
public class StepTemplateUtilsTest {

	@Step
	public void annotationProvider() {

	}

	@Test
	@UseDataProvider("data")
	public void retrieveValue(String template, String expected) throws NoSuchMethodException, NoSuchFieldException {

		Outer.Inner someObject = createInnerObject();

		Step stepAnnotation = StepTemplateUtilsTest.class.getDeclaredMethod("annotationProvider").getAnnotation(Step.class);
		StepTemplateConfig templateConfig = stepAnnotation.templateConfig();
		String replacement = StepTemplateUtils.retrieveValue(templateConfig, 1, template.split("\\."), someObject);

		Assert.assertEquals(expected, replacement);

	}

	@DataProvider
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