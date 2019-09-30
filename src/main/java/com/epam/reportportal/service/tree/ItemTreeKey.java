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

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class ItemTreeKey {

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
