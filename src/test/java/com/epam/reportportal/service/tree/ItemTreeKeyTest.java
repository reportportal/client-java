/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service.tree;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ItemTreeKeyTest {

	@Test
	public void different_keys_with_same_name_should_be_equal() {
		TestItemTree.ItemTreeKey key1 = TestItemTree.ItemTreeKey.of("test");
		TestItemTree.ItemTreeKey key2 = TestItemTree.ItemTreeKey.of("test");
		assertThat(key1, equalTo(key2));
	}

	@Test
	public void different_keys_with_same_name_should_have_the_same_hash() {
		TestItemTree.ItemTreeKey key1 = TestItemTree.ItemTreeKey.of("test");
		TestItemTree.ItemTreeKey key2 = TestItemTree.ItemTreeKey.of("test");
		assertThat(key1.hashCode(), equalTo(key2.hashCode()));
	}

	@Test
	public void it_is_not_possible_to_modify_leaf_by_changing_original_attributes_map() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("test", "test");
		TestItemTree.TestItemLeaf leaf = TestItemTree.createTestItemLeaf(null, Collections.emptyMap(), attributes);
		attributes.put("test2", "test2");
		assertThat(leaf.getAttributes().entrySet(), hasSize(1));
	}

	@Test
	public void it_is_not_possible_to_modify_leaf_by_getting_all_attributes() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("test", "test");
		TestItemTree.TestItemLeaf leaf = TestItemTree.createTestItemLeaf(null, Collections.emptyMap(), attributes);
		Map<String, Object> resultAttributes = leaf.getAttributes();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> resultAttributes.put("test2", "test2"));
	}

	@Test
	public void it_is_possible_to_modify_key_by_calling_set_attribute_method() {
		Map<String, Object> attributes = Collections.singletonMap("test", "test");
		TestItemTree.TestItemLeaf leaf = TestItemTree.createTestItemLeaf(null, Collections.emptyMap(), attributes);
		leaf.setAttribute("test2", "test2");
		Map<String, Object> resultAttributes = leaf.getAttributes();
		assertThat(resultAttributes.entrySet(), hasSize(2));
	}

	@Test
	public void it_is_possible_to_clear_an_attribute() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("test", "test");
		attributes.put("test2", "test2");
		TestItemTree.TestItemLeaf leaf = TestItemTree.createTestItemLeaf(null, Collections.emptyMap(), attributes);
		leaf.clearAttribute("test2");
		Map<String, Object> resultAttributes = leaf.getAttributes();
		assertThat(resultAttributes.entrySet(), hasSize(1));
	}

	@Test
	public void it_is_possible_to_get_a_single_attribute() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("test", "test");
		attributes.put("test2", "value2");
		TestItemTree.TestItemLeaf leaf = TestItemTree.createTestItemLeaf(null, Collections.emptyMap(), attributes);
		assertThat(leaf.getAttribute("test2"), equalTo("value2"));
	}
}
