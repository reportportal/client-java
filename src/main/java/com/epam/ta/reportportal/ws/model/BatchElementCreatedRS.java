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

package com.epam.ta.reportportal.ws.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Element of response for batch save operation.
 */
@JsonInclude(Include.NON_NULL)
public class BatchElementCreatedRS extends EntryCreatedAsyncRS {

	@JsonProperty("message")
	private String message;

	@JsonProperty("stackTrace")
	private String stackTrace;

	public BatchElementCreatedRS() {

	}

	public BatchElementCreatedRS(String id) {
		super.setId(id);
	}

	public BatchElementCreatedRS(String stackTrace, String message) {
		setMessage(message);
		setStackTrace(stackTrace);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
} 