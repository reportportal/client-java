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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class to decorate routine code of accessing fields and methods in complex objects. Supports access to private elements and
 * inheritance.
 */
public class Accessible {

	private final Object object;

	/**
	 * Create the decorator for given object.
	 *
	 * @param object an instance of object on which you need reflective access
	 */
	public Accessible(@Nonnull Object object) {
		this.object = object;
	}

	/**
	 * Create a decorator for an instance of accessible method.
	 *
	 * @param m method to access
	 * @return decorator instance
	 */
	@Nonnull
	public AccessibleMethod method(@Nonnull Method m) {
		return new AccessibleMethod(object, m);
	}

	/**
	 * Create a decorator for an instance of accessible method.
	 *
	 * @param m              method to access
	 * @param parameterTypes an array of specific parameters to distinguish the method
	 * @return decorator instance
	 * @throws NoSuchMethodException no method with such name found
	 */
	@Nonnull
	public AccessibleMethod method(@Nonnull String m, @Nullable Class<?>... parameterTypes) throws NoSuchMethodException {
		return new AccessibleMethod(object, getMethod(m, parameterTypes));
	}

	/**
	 * Create a decorator for an instance of accessible field.
	 *
	 * @param f field to access
	 * @return decorator instance
	 */
	@Nonnull
	public AccessibleField field(@Nonnull Field f) {
		return new AccessibleField(object, f);
	}

	/**
	 * Create a decorator for an instance of accessible field.
	 *
	 * @param name field to access
	 * @return decorator instance
	 * @throws NoSuchFieldException no field with such name found
	 */
	@Nonnull
	public AccessibleField field(@Nonnull String name) throws NoSuchFieldException {
		return new AccessibleField(object, getField(name));
	}

	/**
	 * Create the decorator for given object.
	 *
	 * @param object an instance of object on which you need reflective access
	 * @return decorator instance
	 */
	@Nonnull
	public static Accessible on(@Nonnull Object object) {
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

	@Nonnull
	private Method getMethod(@Nonnull String methodName, @Nullable Class<?>... parameterTypes) throws NoSuchMethodException {
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
