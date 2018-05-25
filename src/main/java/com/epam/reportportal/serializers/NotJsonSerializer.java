/*
 * Copyright (C) 2018 EPAM Systems
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
package com.epam.reportportal.serializers;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.restendpoint.http.exception.SerializerException;
import com.epam.reportportal.restendpoint.serializer.Serializer;
import com.google.common.net.MediaType;
import com.google.common.reflect.TypeToken;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Type;

/**
 * @author Dzianis_Shybeka
 */
public class NotJsonSerializer implements Serializer {

	@Override
	public <T> byte[] serialize(T t) throws SerializerException {

		throw new NotImplementedException();
	}

	@Override
	public <T> T deserialize(byte[] content, Class<T> clazz) throws SerializerException {

		throw new InternalReportPortalClientException("Report portal is not functioning correctly. Response is not json");
	}

	@Override
	public <T> T deserialize(byte[] content, Type type) throws SerializerException {

		throw new InternalReportPortalClientException("Report portal is not functioning correctly. Response is not json");
	}

	@Override
	public MediaType getMimeType() {
		return MediaType.ANY_TEXT_TYPE;
	}

	@Override
	public boolean canRead(MediaType mimeType, Class<?> resultType) {
		return !mimeType.withoutParameters().is(MediaType.JSON_UTF_8.withoutParameters());
	}

	@Override
	public boolean canRead(MediaType mimeType, Type resultType) {
		return canRead(mimeType, TypeToken.of(resultType).getRawType());
	}

	@Override
	public boolean canWrite(Object o) {
		return false;
	}
}
