package com.epam.reportportal.serializers;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.restendpoint.http.exception.SerializerException;
import com.epam.reportportal.restendpoint.serializer.Serializer;
import com.google.common.net.MediaType;
import com.google.common.reflect.TypeToken;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Type;

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
