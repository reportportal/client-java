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

	private final Maybe<String> launchId;
	private final Map<String, TestItemLeaf> testItems = new HashMap<String, TestItemLeaf>();

	public TestItemTree(Maybe<String> launchId) {
		this.launchId = launchId;
	}

	public Maybe<String> getLaunchId() {
		return launchId;
	}

	public Map<String, TestItemLeaf> getTestItems() {
		return testItems;
	}

	private static class TestItemLeaf {

		@Nullable
		private Maybe<String> parentId;
		private final Maybe<String> itemId;
		private final Map<String, TestItemLeaf> childItems = new HashMap<String, TestItemLeaf>();

		private TestItemLeaf(Maybe<String> itemId) {
			this.itemId = itemId;
		}

		private TestItemLeaf(@Nullable Maybe<String> parentId, Maybe<String> itemId) {
			this.parentId = parentId;
			this.itemId = itemId;
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
