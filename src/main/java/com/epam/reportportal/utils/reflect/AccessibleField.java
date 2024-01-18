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

package com.epam.reportportal.utils.reflect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Utility class to decorate routine code of setting and getting a field thought Reflections.
 */
public class AccessibleField {

	private final Field f;
	private final Object bean;

	AccessibleField(@Nonnull Object bean, @Nonnull Field f) {
		this.bean = bean;
		this.f = f;
	}

	@Nonnull
	public Class<?> getType() {
		return this.f.getType();
	}

	/**
	 * Set given field value.
	 *
	 * @param value value to set
	 */
	public void setValue(@Nullable Object value) {
		try {
			this.f.set(this.bean, value);
		} catch (IllegalAccessException accessException) { //NOSONAR
			this.f.setAccessible(true);
			try {
				this.f.set(this.bean, value);
			} catch (IllegalAccessException e) { //NOSONAR
				throw new IllegalAccessError(e.getMessage());
			}
		}
	}

	/**
	 * Return given field value.
	 */
	@Nullable
	public Object getValue() {
		try {
			return this.f.get(this.bean);
		} catch (IllegalAccessException accessException) { //NOSONAR
			this.f.setAccessible(true);
			try {
				return this.f.get(this.bean);
			} catch (IllegalAccessException e) { //NOSONAR
				throw new IllegalAccessError(e.getMessage());
			}
		}
	}
}
