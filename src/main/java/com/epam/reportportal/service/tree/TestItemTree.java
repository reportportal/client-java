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
	private final Map<ItemTreeKey, TestItemLeaf> testItems;

	public TestItemTree() {
		this.testItems = new ConcurrentHashMap<ItemTreeKey, TestItemLeaf>();
	}

	public static TestItemTree.TestItemLeaf createTestItemLeaf(Maybe<String> itemId, int expectedChildrenCount) {
		return new TestItemTree.TestItemLeaf(itemId, expectedChildrenCount);
	}

	public static TestItemTree.TestItemLeaf createTestItemLeaf(Maybe<String> parentId, Maybe<String> itemId, int expectedChildrenCount) {
		return new TestItemTree.TestItemLeaf(parentId, itemId, expectedChildrenCount);
	}

	public static TestItemTree.TestItemLeaf createTestItemLeaf(Maybe<String> itemId,
			ConcurrentHashMap<ItemTreeKey, TestItemLeaf> childItems) {
		return new TestItemTree.TestItemLeaf(itemId, childItems);
	}

	public static TestItemTree.TestItemLeaf createTestItemLeaf(Maybe<String> parentId, Maybe<String> itemId,
			ConcurrentHashMap<ItemTreeKey, TestItemLeaf> childItems) {
		return new TestItemTree.TestItemLeaf(parentId, itemId, childItems);
	}

	public Maybe<String> getLaunchId() {
		return launchId;
	}

	public void setLaunchId(Maybe<String> launchId) {
		this.launchId = launchId;
	}

	public Map<ItemTreeKey, TestItemLeaf> getTestItems() {
		return testItems;
	}

	public static final class ItemTreeKey {

		private final String name;
		private final int hash;

		private ItemTreeKey(String name) {
			this.name = name;
			this.hash = name != null ? name.hashCode() : 0;
		}

		private ItemTreeKey(String name, int hash) {
			this.name = name;
			this.hash = hash;
		}

		public String getName() {
			return name;
		}

		public int getHash() {
			return hash;
		}

		public static ItemTreeKey of(String name) {
			return new ItemTreeKey(name);
		}

		public static ItemTreeKey of(String name, int hash) {
			return new ItemTreeKey(name, hash);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ItemTreeKey that = (ItemTreeKey) o;

			if (hash != that.hash) {
				return false;
			}
			return name != null ? name.equals(that.name) : that.name == null;
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + hash;
			return result;
		}
	}

	public static class TestItemLeaf {

		@Nullable
		private Maybe<String> parentId;
		@Nullable
		private Maybe<OperationCompletionRS> finishResponse;
		private final Maybe<String> itemId;
		private final Map<ItemTreeKey, TestItemLeaf> childItems;

		private TestItemLeaf(Maybe<String> itemId, int expectedChildrenCount) {
			this.itemId = itemId;
			this.childItems = new ConcurrentHashMap<ItemTreeKey, TestItemLeaf>(expectedChildrenCount);
		}

		private TestItemLeaf(Maybe<String> itemId, ConcurrentHashMap<ItemTreeKey, TestItemLeaf> childItems) {
			this.itemId = itemId;
			this.childItems = childItems;
		}

		private TestItemLeaf(@Nullable Maybe<String> parentId, Maybe<String> itemId, int expectedChildrenCount) {
			this(itemId, expectedChildrenCount);
			this.parentId = parentId;
		}

		private TestItemLeaf(@Nullable Maybe<String> parentId, Maybe<String> itemId,
				ConcurrentHashMap<ItemTreeKey, TestItemLeaf> childItems) {
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

		public Map<ItemTreeKey, TestItemLeaf> getChildItems() {
			return childItems;
		}
	}
}
