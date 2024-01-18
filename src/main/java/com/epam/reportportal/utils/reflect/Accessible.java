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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Representation of accessible method or field
 */
public class Accessible {

	private final Object object;

	public Accessible(Object object) {
		this.object = object;
	}

	public AccessibleMethod method(Method m) {
		return new AccessibleMethod(object, m);
	}

	public AccessibleMethod method(String m, Class<?>... parameterTypes) throws NoSuchMethodException {
		return new AccessibleMethod(object, getMethod(m, parameterTypes));
	}

	public AccessibleField field(Field f) {
		return new AccessibleField(object, f);
	}

	public AccessibleField field(String name) throws NoSuchFieldException {
		return new AccessibleField(object, getField(name));
	}

	public static Accessible on(Object object) {
		return new Accessible(object);
	}

	@Nonnull
	private Field getField(@Nonnull String fieldName) throws NoSuchFieldException {
		Class<?> clazz = object.getClass();
		try {
			return clazz.getField(fieldName);
		} catch (NoSuchFieldException e) {
			do {
				try {
					return clazz.getDeclaredField(fieldName);
				} catch (NoSuchFieldException ignore) {
				}

				clazz = clazz.getSuperclass();
			} while (clazz != null);
			throw e;
		}
	}

	private Method getMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Class<?> clazz = object.getClass();
		try {
			return clazz.getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			do {
				try {
					return clazz.getDeclaredMethod(methodName, parameterTypes);
				} catch (NoSuchMethodException ignore) {
				}

				clazz = clazz.getSuperclass();
			} while (clazz != null);
			throw e;
		}
	}
}
