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

package com.epam.reportportal.service.tree;

import io.reactivex.Maybe;
import io.reactivex.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestItemTree {

	private final ThreadLocal<Map<String, TestItemLeaf>> testItems;
	private final Map<String, TestItemLeaf> defaultMap;

	public TestItemTree() {
		this.testItems = new InheritableThreadLocal<Map<String, TestItemLeaf>>() {
			@Override
			protected Map<String, TestItemLeaf> initialValue() {
				return new HashMap<String, TestItemLeaf>();
			}
		};
		this.defaultMap = new HashMap<String, TestItemLeaf>();
	}

	public Map<String, TestItemLeaf> getTestItems() {
		return testItems.get();
	}

	public Map<String, TestItemLeaf> getDefaultMap() {
		return defaultMap;
	}

	public static class TestItemLeaf {

		@Nullable
		private Maybe<String> parentId;
		private final Maybe<String> itemId;
		private final Map<String, TestItemLeaf> childItems;

		public TestItemLeaf(@Nullable Maybe<String> parentId, Maybe<String> itemId, Map<String, TestItemLeaf> childItems) {
			this.parentId = parentId;
			this.itemId = itemId;
			this.childItems = childItems;
		}

		public TestItemLeaf(Maybe<String> itemId, Map<String, TestItemLeaf> childItems) {
			this.itemId = itemId;
			this.childItems = childItems;
		}

		@Nullable
		public Maybe<String> getParentId() {
			return parentId;
		}

		public void setParentId(@Nullable Maybe<String> parentId) {
			this.parentId = parentId;
		}

		public Maybe<String> getItemId() {
			return itemId;
		}

		public Map<String, TestItemLeaf> getChildItems() {
			return childItems;
		}
	}
}
