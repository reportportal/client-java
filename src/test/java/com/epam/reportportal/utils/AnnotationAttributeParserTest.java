package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.attribute.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("unused")
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

	private static Attributes getAttributesAnnotation(Class<?> clazz) throws NoSuchMethodException {
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

	private static class ValueExtract implements Function<ItemAttributesRQ, String> {
		@Override
		public String apply(ItemAttributesRQ input) {
			assertThat(input, notNullValue());
			return input.getValue();
		}
	}

	private static final ValueExtract VALUE_EXTRACT = new ValueExtract();

	private static void verify_value_attributes_converted_into_correct_rq(Class<?> clazz) throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(clazz);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(VALUE_ATTRIBUTE_VERIFY_TEST_ARRAY.length));
		for (ItemAttributesRQ request : result) {
			assertThat(request.isSystem(), equalTo(false));
			assertThat(request.getKey(), nullValue());
		}
		assertThat(result.stream().map(VALUE_EXTRACT).collect(toList()), containsInAnyOrder(VALUE_ATTRIBUTE_VERIFY_TEST_ARRAY));
	}

	@Test
	public void verify_two_value_attributes_converted_into_correct_rq() throws NoSuchMethodException {
		verify_value_attributes_converted_into_correct_rq(TwoValueAttributeVerify.class);
	}

	private static final class EmptyValueAttributeFilter {
		@Attributes(attributeValues = { @AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST_1), @AttributeValue(""),
				@AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST_2) })
		public void testMethod() {
		}
	}

	@Test
	public void verify_empty_value_attribute_filter_rq() throws NoSuchMethodException {
		verify_value_attributes_converted_into_correct_rq(EmptyValueAttributeFilter.class);
	}

	private static final String MULTI_VALUE_ATTRIBUTE_KEY_VERIFY = "keytest";

	private static final String MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1 = "test1";

	private static final String MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2 = "Test2";

	private static final String MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 = "TEST3";

	private static final String[] MULTI_VALUE_ATTRIBUTE_VERIFY_ARRAY = { MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1,
			MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 };

	private static final class MultiValueAttributeVerify {
		@Attributes(multiValueAttributes = @MultiValueAttribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, values = {
				MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 }))
		public void testMethod() {
		}
	}

	private static void verify_multi_value_attributes_converted_into_correct_rq(Class<?> clazz, Matcher<Object> keyVerify)
			throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(clazz);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(MULTI_VALUE_ATTRIBUTE_VERIFY_ARRAY.length));
		for (ItemAttributesRQ request : result) {
			assertThat(request.isSystem(), equalTo(false));
			assertThat(request.getKey(), keyVerify);
		}
		assertThat(result.stream().map(VALUE_EXTRACT).collect(toList()), containsInAnyOrder(MULTI_VALUE_ATTRIBUTE_VERIFY_ARRAY));
	}

	@Test
	public void verify_multi_value_attributes_converted_into_correct_rq() throws NoSuchMethodException {
		verify_multi_value_attributes_converted_into_correct_rq(MultiValueAttributeVerify.class, equalTo(MULTI_VALUE_ATTRIBUTE_KEY_VERIFY));
	}

	private static final class MultiValueAttributeNullKeyVerify {
		@Attributes(multiValueAttributes = @MultiValueAttribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, isNullKey = true, values = {
				MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 }))
		public void testMethod() {
		}
	}

	@Test
	public void verify_multi_value_attributes_null_key_converted_into_correct_rq() throws NoSuchMethodException {
		verify_multi_value_attributes_converted_into_correct_rq(MultiValueAttributeNullKeyVerify.class, nullValue());
	}

	private static final class MultiValueAttributeEmptyKeyVerify {
		@Attributes(multiValueAttributes = @MultiValueAttribute(values = { MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1,
				MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 }))
		public void testMethod() {
		}
	}

	@Test
	public void verify_multi_value_attributes_empty_key_converted_into_correct_rq() throws NoSuchMethodException {
		verify_multi_value_attributes_converted_into_correct_rq(MultiValueAttributeEmptyKeyVerify.class, equalTo(""));
	}

	private static final String MULTI_VALUE_ATTRIBUTE_KEY_VERIFY_2 = "keytest2";

	private static final class TwoMultiValueAttributeVerify {
		@Attributes(multiValueAttributes = {
				@MultiValueAttribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, values = { MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1,
						MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 }),
				@MultiValueAttribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY_2, values = { MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1,
						MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_2, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_3 }) })
		public void testMethod() {
		}
	}

	@Test
	public void verify_two_multi_value_attributes_converted_into_correct_rq() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(TwoMultiValueAttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		int expectedSize = MULTI_VALUE_ATTRIBUTE_VERIFY_ARRAY.length * 2;
		assertThat(result, hasSize(expectedSize));
		for (ItemAttributesRQ request : result) {
			assertThat(request.isSystem(), equalTo(false));
			assertThat(request.getKey(), anyOf(equalTo(MULTI_VALUE_ATTRIBUTE_KEY_VERIFY), equalTo(MULTI_VALUE_ATTRIBUTE_KEY_VERIFY_2)));
		}
		ArrayList<String> expectedValues = new ArrayList<>(expectedSize);
		expectedValues.addAll(Arrays.asList(MULTI_VALUE_ATTRIBUTE_VERIFY_ARRAY));
		expectedValues.addAll(Arrays.asList(MULTI_VALUE_ATTRIBUTE_VERIFY_ARRAY));
		assertThat(result.stream().map(VALUE_EXTRACT).collect(toList()), containsInAnyOrder(expectedValues.toArray()));
	}

	private static final class MultiValueAttributeEmptyArrayVerify {
		@Attributes(multiValueAttributes = @MultiValueAttribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, values = {}))
		public void testMethod() {
		}
	}

	@Test
	public void verify_multi_value_attribute_with_empty_array_returns_empty_set() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(MultiValueAttributeEmptyArrayVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(0));
	}

	private static class KeyExtract implements Function<ItemAttributesRQ, String> {
		@Override
		public String apply(ItemAttributesRQ input) {
			assertThat(input, notNullValue());
			return input.getKey();
		}
	}

	private static final KeyExtract KEY_EXTRACT = new KeyExtract();

	private static final String MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1 = "key1";

	private static final String MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_2 = "Key2";

	private static final String MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_3 = "KEY3";

	private static final String MULTI_VALUE_ATTRIBUTE_VERIFY = "valueTest";

	private static final String[] MULTI_KEY_ATTRIBUTE_VERIFY_ARRAY = { MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1,
			MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_2, MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_3 };

	private static final class MultiKeyAttributeVerify {
		@Attributes(multiKeyAttributes = @MultiKeyAttribute(keys = { MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1,
				MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_2, MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_3 }, value = MULTI_VALUE_ATTRIBUTE_VERIFY))
		public void testMethod() {
		}
	}

	@Test
	public void verify_multi_key_attributes_converted_into_correct_rq() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(MultiKeyAttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(MULTI_KEY_ATTRIBUTE_VERIFY_ARRAY.length));
		for (ItemAttributesRQ request : result) {
			assertThat(request.isSystem(), equalTo(false));
			assertThat(request.getValue(), equalTo(MULTI_VALUE_ATTRIBUTE_VERIFY));
		}
		assertThat(result.stream().map(KEY_EXTRACT).collect(toList()), containsInAnyOrder(MULTI_KEY_ATTRIBUTE_VERIFY_ARRAY));
	}

	private static final class MultiKeyEmptyValueAttributeVerify {
		@Attributes(multiKeyAttributes = @MultiKeyAttribute(keys = { MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1,
				MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_2, MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_3 }, value = ""))
		public void testMethod() {
		}
	}

	@Test
	public void verify_multi_key_empty_value_attributes_converted_into_empty_set() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(MultiKeyEmptyValueAttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(0));
	}

	private static final class MultiKeyEmptyKeyAttributeFilter {
		@Attributes(multiKeyAttributes = @MultiKeyAttribute(keys = {}, value = MULTI_VALUE_ATTRIBUTE_VERIFY))
		public void testMethod() {
		}
	}

	@Test
	public void verify_multi_key_with_empty_array_returns_value() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(MultiKeyEmptyKeyAttributeFilter.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(1));
		ItemAttributesRQ request = result.iterator().next();
		assertThat(request.isSystem(), equalTo(false));
		assertThat(request.getValue(), equalTo(MULTI_VALUE_ATTRIBUTE_VERIFY));
		assertThat(request.getKey(), nullValue());
	}

	private static final class AttributeVerify {
		@Attributes(attributes = @Attribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, value = MULTI_VALUE_ATTRIBUTE_VERIFY))
		public void testMethod() {
		}
	}

	@Test
	public void verify_attribute_converted_into_correct_rq() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(AttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(1));
		ItemAttributesRQ request = result.iterator().next();
		assertThat(request.isSystem(), equalTo(false));
		assertThat(request.getKey(), equalTo(MULTI_VALUE_ATTRIBUTE_KEY_VERIFY));
		assertThat(request.getValue(), equalTo(MULTI_VALUE_ATTRIBUTE_VERIFY));
	}

	private static final class TwoAttributeVerify {
		@Attributes(attributes = { @Attribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, value = MULTI_VALUE_ATTRIBUTE_VERIFY),
				@Attribute(key = MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1, value = MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1) })
		public void testMethod() {
		}
	}

	@Test
	public void verify_two_attributes_converted_into_correct_rq() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(TwoAttributeVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(2));
		for (ItemAttributesRQ request : result) {
			assertThat(request.isSystem(), equalTo(false));
		}

		assertThat(
				result.stream().map(KEY_EXTRACT).collect(toList()),
				containsInAnyOrder(MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1)
		);
		assertThat(
				result.stream().map(VALUE_EXTRACT).collect(toList()),
				containsInAnyOrder(MULTI_VALUE_ATTRIBUTE_VERIFY, MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1)
		);
	}

	private static final class AllAttributeAnnotationsVerify {
		@Attributes(attributes = @Attribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, value = MULTI_VALUE_ATTRIBUTE_VERIFY), attributeValues = @AttributeValue(VALUE_ATTRIBUTE_VERIFY_TEST_1), multiKeyAttributes = @MultiKeyAttribute(keys = MULTI_KEY_ATTRIBUTE_KEY_VERIFY_TEST_1, value = MULTI_VALUE_ATTRIBUTE_VERIFY), multiValueAttributes = @MultiValueAttribute(key = MULTI_VALUE_ATTRIBUTE_KEY_VERIFY, values = MULTI_VALUE_ATTRIBUTE_VERIFY_TEST_1))
		public void testMethod() {
		}
	}

	@Test
	public void verify_all_attributes_in_one_annotation() throws NoSuchMethodException {
		Attributes testAnnotation = getAttributesAnnotation(AllAttributeAnnotationsVerify.class);
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(testAnnotation);

		assertThat(result, hasSize(4));
	}

	private static final class StandaloneAttributeVerify {
		@Attribute(key = "standaloneKey", value = "standaloneValue")
		public void testMethod() {
		}
	}

	@Test
	public void verify_standalone_attribute_on_method() throws NoSuchMethodException {
		java.lang.reflect.Method method = StandaloneAttributeVerify.class.getMethod("testMethod");
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(method);

		assertThat(result, hasSize(1));
		ItemAttributesRQ request = result.iterator().next();
		assertThat(request.getKey(), equalTo("standaloneKey"));
		assertThat(request.getValue(), equalTo("standaloneValue"));
	}

	private static final class RepeatedAttributeVerify {
		@Attribute(key = "key1", value = "value1")
		@Attribute(key = "key2", value = "value2")
		public void testMethod() {
		}
	}

	@Test
	public void verify_repeated_attribute_on_method() throws NoSuchMethodException {
		java.lang.reflect.Method method = RepeatedAttributeVerify.class.getMethod("testMethod");
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(method);

		assertThat(result, hasSize(2));
		assertThat(result.stream().map(KEY_EXTRACT).collect(toList()), containsInAnyOrder("key1", "key2"));
		assertThat(result.stream().map(VALUE_EXTRACT).collect(toList()), containsInAnyOrder("value1", "value2"));
	}

	private static final class MixedAttributeVerify {
		@Attributes(attributes = @Attribute(key = "innerKey", value = "innerValue"))
		@Attribute(key = "outerKey", value = "outerValue")
		public void testMethod() {
		}
	}

	@Test
	public void verify_mixed_attributes_on_method() throws NoSuchMethodException {
		java.lang.reflect.Method method = MixedAttributeVerify.class.getMethod("testMethod");
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(method);

		assertThat(result, hasSize(2));
		assertThat(result.stream().map(KEY_EXTRACT).collect(toList()), containsInAnyOrder("innerKey", "outerKey"));
		assertThat(result.stream().map(VALUE_EXTRACT).collect(toList()), containsInAnyOrder("innerValue", "outerValue"));
	}

	private static final class AllTypesOnMethodVerify {
		@Attribute(key = "attrKey", value = "attrValue")
		@AttributeValue("attrValueOnly")
		@MultiKeyAttribute(keys = { "mk1", "mk2" }, value = "mkValue")
		@MultiValueAttribute(key = "mvKey", values = { "mv1", "mv2" })
		public void testMethod() {
		}
	}

	@Test
	public void verify_all_types_on_method() throws NoSuchMethodException {
		java.lang.reflect.Method method = AllTypesOnMethodVerify.class.getMethod("testMethod");
		Set<ItemAttributesRQ> result = AttributeParser.retrieveAttributes(method);

		// 1 (Attribute) + 1 (AttributeValue) + 2 (MultiKey) + 2 (MultiValue) = 6
		assertThat(result, hasSize(6));
	}
}
