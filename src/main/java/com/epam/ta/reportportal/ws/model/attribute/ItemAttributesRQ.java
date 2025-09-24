/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.ta.reportportal.ws.model.attribute;

import java.util.Objects;

public class ItemAttributesRQ extends ItemAttributeResource {

	private boolean system;

	public ItemAttributesRQ() {
	}

	public ItemAttributesRQ(String value) {
		super(null, value);
	}

	public ItemAttributesRQ(String key, String value) {
		super(key, value);
	}

	public ItemAttributesRQ(String key, String value, boolean system) {
		super(key, value);
		this.system = system;
	}

	public boolean isSystem() {
		return system;
	}

	public void setSystem(boolean system) {
		this.system = system;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ItemAttributesRQ)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ItemAttributesRQ that = (ItemAttributesRQ) o;
		return system == that.system;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), system);
	}
}
