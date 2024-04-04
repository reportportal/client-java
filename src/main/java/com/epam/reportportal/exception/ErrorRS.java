/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.epam.reportportal.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.io.Serializable;

/**
 * Base Error response body for all Report Portal exceptions
 * 
 * @author Andrei Varabyeu
 * 
 */
@JsonPropertyOrder({ "errorCode", "message", "stackTrace" })
@JsonInclude(Include.NON_NULL)
public class ErrorRS implements Serializable {
	/**
	 * Generated SVUID
	 */
	private static final long serialVersionUID = -3717290684860161862L;

	@JsonSerialize(using = ErrorTypeSerializer.class)
	@JsonDeserialize(using = ErrorTypeDeserializer.class)
	@JsonProperty("errorCode")
	private ErrorType errorType;

	@JsonProperty("stackTrace")
	private String stackTrace;

	@JsonProperty("message")
	private String message;

	public ErrorType getErrorType() {
		return errorType;
	}

	public void setErrorType(ErrorType errorType) {
		this.errorType = errorType;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((errorType == null) ? 0 : errorType.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((stackTrace == null) ? 0 : stackTrace.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ErrorRS other = (ErrorRS) obj;
		if (errorType != other.errorType)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (stackTrace == null) {
			return other.stackTrace == null;
		} else
			return stackTrace.equals(other.stackTrace);
	}

	private static class ErrorTypeDeserializer extends JsonDeserializer<ErrorType> {

		@Override
		public ErrorType deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			ObjectCodec oc = parser.getCodec();
			JsonNode node = oc.readTree(parser);
			return ErrorType.getByCode(node.asInt());

		}

	}

	private static class ErrorTypeSerializer extends JsonSerializer<ErrorType> {

		@Override
		public void serialize(ErrorType error, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
			jsonGenerator.writeNumber(error.getCode());
		}

	}

	@Override
	public String toString() {
		return "ErrorRS{" + "errorType=" + errorType
				+ ", stackTrace='" + stackTrace + '\''
				+ ", message='" + message + '\''
				+ '}';
	}
}