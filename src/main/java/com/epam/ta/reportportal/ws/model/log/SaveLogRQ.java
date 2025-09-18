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

package com.epam.ta.reportportal.ws.model.log;

import com.epam.reportportal.utils.serialize.TimeDeserializer;
import com.epam.reportportal.utils.serialize.TimeSerializer;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(Include.NON_NULL)
public class SaveLogRQ {

	@JsonProperty("uuid")
	private String uuid;

	@JsonAlias({ "itemUuid", "item_id" })
	private String itemUuid;

	@JsonProperty(value = "launchUuid")
	private String launchUuid;

	@JsonProperty(value = "time", required = true)
	@JsonSerialize(using = TimeSerializer.class)
	@JsonDeserialize(using = TimeDeserializer.class)
	private Comparable<? extends Comparable<?>> logTime;

	@JsonProperty(value = "message")
	private String message;

	@JsonProperty(value = "level")
	private String level;

	@JsonProperty(value = "file")
	private File file;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Comparable<? extends Comparable<?>> getLogTime() {
		return logTime;
	}

	public void setLogTime(Comparable<? extends Comparable<?>> logTime) {
		this.logTime = logTime;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getItemUuid() {
		return itemUuid;
	}

	public void setItemUuid(String itemUuid) {
		this.itemUuid = itemUuid;
	}

	public String getLaunchUuid() {
		return launchUuid;
	}

	public void setLaunchUuid(String launchUuid) {
		this.launchUuid = launchUuid;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getLevel() {
		return level;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	@JsonInclude(Include.NON_NULL)
	public static class File {

		@JsonProperty(value = "name")
		private String name;

		@JsonIgnore
		private byte[] content;

		@JsonIgnore
		private String contentType;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public byte[] getContent() {
			return content;
		}

		public void setContent(byte[] content) {
			this.content = content;
		}

		public String getContentType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
	}
} 