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

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.Maybe;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tree for holding test items structure to provide {@link TestItemLeaf} retrieving by {@link ItemTreeKey}
 * for API calls using {@link ItemTreeReporter}.
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestItemTree {

	private Maybe<String> launchId;
	private final Map<ItemTreeKey, TestItemLeaf> testItems;

	public TestItemTree() {
		this.testItems = new ConcurrentHashMap<>();
	}

	public static TestItemTree.TestItemLeaf createTestItemLeaf(Maybe<String> itemId) {
		return new TestItemTree.TestItemLeaf(itemId);
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

	/**
	 * Key for test items structure storing in the {@link TestItemTree}
	 */
	public static final class ItemTreeKey {

		private final String name;
		private final int hash;
		private final Map<String, Object> attributes = new ConcurrentHashMap<>();

		private ItemStatus status;
		private ItemType type;

		private ItemTreeKey(String name, int hash) {
			this.name = name;
			this.hash = hash;
		}

		private ItemTreeKey(String name) {
			this(name, name != null ? name.hashCode() : 0);
		}

		private ItemTreeKey(String name, Map<String, Object> attributes) {
			this(name);
			this.attributes.putAll(attributes);
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

		public static ItemTreeKey of(String name, Map<String, Object> attributes) {
			return new ItemTreeKey(name, attributes);
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
			return Objects.equals(name, that.name);
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + hash;
			return result;
		}

		public ItemStatus getStatus() {
			return status;
		}

		public void setStatus(ItemStatus status) {
			this.status = status;
		}

		public ItemType getType() {
			return type;
		}

		public void setType(ItemType type) {
			this.type = type;
		}

		public Object getAttribute(String key) {
			return attributes.get(key);
		}

		public Object setAttribute(String key, Object value) {
			return attributes.put(key, value);
		}

		public void clearAttribute(String key) {
			attributes.remove(key);
		}

		public Map<String, Object> getAttributes() {
			return Collections.unmodifiableMap(attributes);
		}
	}

	/**
	 * Class represents test item with links on parent and descendants. Contains item id and finish response promises for reporting
	 * using {@link ItemTreeReporter}.
	 * Finish response is required to provide correct request ordering:
	 * 1) default finish response from agent
	 * 2) callback requests that will be sent only after default finish response is returned or if it's NULL
	 */
	public static class TestItemLeaf {

		@Nullable
		private Maybe<String> parentId;
		@Nullable
		private Maybe<OperationCompletionRS> finishResponse;
		private final Maybe<String> itemId;
		private final Map<ItemTreeKey, TestItemLeaf> childItems;

		private TestItemLeaf(Maybe<String> itemId) {
			this.itemId = itemId;
			this.childItems = new ConcurrentHashMap<>();
		}

		private TestItemLeaf(Maybe<String> itemId, int expectedChildrenCount) {
			this.itemId = itemId;
			this.childItems = new ConcurrentHashMap<>(expectedChildrenCount);
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
