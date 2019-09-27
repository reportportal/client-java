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

import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.Maybe;
import io.reactivex.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestItemTree {

	private Maybe<String> launchId;
	private final Map<String, TestItemLeaf> testItems;

	public TestItemTree() {
		this.testItems = new ConcurrentHashMap<String, TestItemLeaf>();
	}

	public Maybe<String> getLaunchId() {
		return launchId;
	}

	public void setLaunchId(Maybe<String> launchId) {
		this.launchId = launchId;
	}

	public Map<String, TestItemLeaf> getTestItems() {
		return testItems;
	}

	public static class TestItemLeaf {

		@Nullable
		private Maybe<String> parentId;
		@Nullable
		private Maybe<OperationCompletionRS> finishResponse;
		private final Maybe<String> itemId;
		private final Map<String, TestItemLeaf> childItems;

		public TestItemLeaf(Maybe<String> itemId, int expectedChildrenCount) {
			this.itemId = itemId;
			this.childItems = new ConcurrentHashMap<String, TestItemLeaf>(expectedChildrenCount);
		}

		public TestItemLeaf(@Nullable Maybe<String> parentId, Maybe<String> itemId, int expectedChildrenCount) {
			this(itemId, expectedChildrenCount);
			this.parentId = parentId;
		}

		public TestItemLeaf(Maybe<String> itemId, ConcurrentHashMap<String, TestItemLeaf> childItems) {
			this.itemId = itemId;
			this.childItems = childItems;
		}

		public TestItemLeaf(@Nullable Maybe<String> parentId, Maybe<String> itemId, ConcurrentHashMap<String, TestItemLeaf> childItems) {
			this(itemId, childItems);
			this.parentId = parentId;
		}

		@Nullable
		public Maybe<String> getParentId() {
			return parentId;
		}

		public void setParentId(@Nullable Maybe<String> parentId) {
			this.parentId = parentId;
		}

		@Nullable
		public Maybe<OperationCompletionRS> getFinishResponse() {
			return finishResponse;
		}

		public void setFinishResponse(@Nullable Maybe<OperationCompletionRS> finishResponse) {
			this.finishResponse = finishResponse;
		}

		public Maybe<String> getItemId() {
			return itemId;
		}

		public Map<String, TestItemLeaf> getChildItems() {
			return childItems;
		}
	}
}
