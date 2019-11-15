package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.attribute.AttributeValue;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AnnotationAttributeParserTest {

	private static final String VALUE_ATTRIBUTE_VERIFY_TEST = "my_attribute_value_test";

	private static final class ValueAttributeVerify {
		@Attributes(attributeValues = @AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST))
		public void testMethod() {}
	}

	@Test
	public void verify_value_attribute_converted_into_correct_rq() throws NoSuchMethodException {
		ValueAttributeVerify testObject = new ValueAttributeVerify();
		Attributes testAnnotation = testObject.getClass().getMethod("testMethod").getAnnotation(Attributes.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(1));
		ItemAttributesRQ request = result.iterator().next();
		assertThat(request.isSystem(), equalTo(false));
		assertThat(request.getKey(), nullValue());
		assertThat(request.getValue(), equalTo(VALUE_ATTRIBUTE_VERIFY_TEST));
	}
}
