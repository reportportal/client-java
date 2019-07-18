package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepTemplateUtilsTest {

	@Test
	@Step
	public void retrieveValue() throws NoSuchMethodException, NoSuchFieldException {

		final String field = "someObject.outerName";

		DummyClass.InnerDummyClass someObject = new DummyClass.InnerDummyClass("outer", "inner");

		Step stepAnnotation = StepTemplateUtilsTest.class.getMethod("retrieveValue").getAnnotation(Step.class);
		StepTemplateConfig templateConfig = stepAnnotation.templateConfig();
		String replacement = StepTemplateUtils.retrieveValue(templateConfig, 1, field.split("\\."), someObject);

		Assert.assertEquals("outer", replacement);

	}

	private static class DummyClass {

		private String outerName;

		public DummyClass(String outerName) {
			this.outerName = outerName;
		}

		private static class InnerDummyClass extends DummyClass {
			private String innerName;

			public InnerDummyClass(String outerName, String innerName) {
				super(outerName);
				this.innerName = innerName;
			}
		}

	}

}