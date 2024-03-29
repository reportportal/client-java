/*
 *  Copyright 2022 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A class for auxiliary manipulations with objects.
 */
public class ObjectUtils {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ObjectUtils() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Convert POJO object through ObjectMapper to a string.
	 *
	 * @param pojoObject an object to convert
	 * @return representation of the object
	 */
	@Nonnull
	public static String toString(@Nonnull Object pojoObject) {
		try {
			return MAPPER.writeValueAsString(pojoObject);
		} catch (IOException e) {
			throw new InternalReportPortalClientException(
					"Unable to serialize " + pojoObject.getClass().getSimpleName() + " object to String", e);
		}
	}

	/**
	 * Clone POJO object through ObjectMapper to avoid implementation of clone method and model modification.
	 *
	 * @param pojoObject an object to clone
	 * @param clazz      the clone object type
	 * @param <T>        the clone object type
	 * @return cloned object
	 */
	@Nonnull
	public static <T> T clonePojo(@Nonnull T pojoObject, Class<T> clazz) {
		try {
			return MAPPER.readValue(toString(pojoObject), clazz);
		} catch (IOException e) {
			throw new InternalReportPortalClientException(
					"Unable to clone " + pojoObject.getClass().getSimpleName() + " object", e);
		}
	}
}
