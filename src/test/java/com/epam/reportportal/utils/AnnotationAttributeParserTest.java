package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.attribute.AttributeValue;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AnnotationAttributeParserTest {

	private static final String VALUE_ATTRIBUTE_VERIFY_TEST_1 = "my_attribute_value_test";

	private static final String VALUE_ATTRIBUTE_VERIFY_TEST_2 = "my_attribute_value_test_2";

	private static final String[] VALUE_ATTRIBUTE_VERIFY_TEST_ARRAY = { VALUE_ATTRIBUTE_VERIFY_TEST_1, VALUE_ATTRIBUTE_VERIFY_TEST_2 };

	private static final class ValueAttributeVerify {
		@Attributes(attributeValues = @AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST_1))
		public void testMethod() {
		}
	}

	private static final class TwoValueAttributeVerify {
		@Attributes(attributeValues = { @AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST_1), @AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST_2) })
		public void testMethod() {
		}
	}

	private Attributes getAttributesAnnotation(Class<?> clazz) throws NoSuchMethodException {
		return clazz.getMethod("testMethod").getAnnotation(Attributes.class);
	}

	@Test
	public void verify_value_attribute_converted_into_correct_rq() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(ValueAttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(1));
		ItemAttributesRQ request = result.iterator().next();
		assertThat(request.isSystem(), equalTo(false));
		assertThat(request.getKey(), nullValue());
		assertThat(request.getValue(), equalTo(VALUE_ATTRIBUTE_VERIFY_TEST_1));
	}

	@Test
	public void verify_two_value_attributes_converted_into_correct_rq() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(TwoValueAttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(2));
		for (ItemAttributesRQ request : result) {
			assertThat(request.isSystem(), equalTo(false));
			assertThat(request.getKey(), nullValue());
		}
		assertThat(Iterables.transform(result, new Function<ItemAttributesRQ, String>() {
			@Nullable
			@Override
			public String apply(@Nullable ItemAttributesRQ input) {
				assertThat(input, notNullValue());
				return input.getValue();
			}
		}), containsInAnyOrder(VALUE_ATTRIBUTE_VERIFY_TEST_ARRAY));
	}

}
