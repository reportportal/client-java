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

package com.epam.reportportal.utils.templating;

import com.epam.reportportal.annotations.StepTemplateConfig;

public class TemplateConfiguration {
	public static final String METHOD_NAME_TEMPLATE = "method";
	public static final String SELF_NAME_TEMPLATE = "this";
	public static final String FIELD_REFERENCE_DELIMITER = ".";
	public static final String ITERABLE_START_PATTERN = "[";
	public static final String ITERABLE_END_PATTERN = "]";
	public static final String ITERABLE_ELEMENT_DELIMITER = ", ";
	public static final String ARRAY_START_PATTERN = "{";
	public static final String ARRAY_END_PATTERN = "}";
	public static final String ARRAY_ELEMENT_DELIMITER = ", ";

	private String methodName;
	private String selfName;
	private String fieldDelimiter;
	private String iterableStart;
	private String iterableEnd;
	private String iterableDelimiter;
	private String arrayStart;
	private String arrayEnd;
	private String arrayDelimiter;

	public TemplateConfiguration() {
		methodName = METHOD_NAME_TEMPLATE;
		selfName = SELF_NAME_TEMPLATE;
		fieldDelimiter = FIELD_REFERENCE_DELIMITER;
		iterableStart = ITERABLE_START_PATTERN;
		iterableEnd = ITERABLE_END_PATTERN;
		iterableDelimiter = ITERABLE_ELEMENT_DELIMITER;
		arrayStart = ARRAY_START_PATTERN;
		arrayEnd = ARRAY_END_PATTERN;
		arrayDelimiter = ARRAY_ELEMENT_DELIMITER;
	}

	public TemplateConfiguration(StepTemplateConfig config) {
		methodName = config.methodNameTemplate();
		selfName = config.selfNameTemplate();
		fieldDelimiter = config.fieldDelimiter();
		iterableStart = config.iterableStartSymbol();
		iterableEnd = config.iterableEndSymbol();
		iterableDelimiter = config.iterableElementDelimiter();
		arrayStart = config.arrayStartSymbol();
		arrayEnd = config.arrayEndSymbol();
		arrayDelimiter = config.arrayElementDelimiter();
	}

	public String getMethodName() {
		return methodName;
	}

	public TemplateConfiguration setMethodName(String methodName) {
		this.methodName = methodName;
		return this;
	}

	public String getSelfName() {
		return selfName;
	}

	public TemplateConfiguration setSelfName(String selfName) {
		this.selfName = selfName;
		return this;
	}

	public String getFieldDelimiter() {
		return fieldDelimiter;
	}

	public TemplateConfiguration setFieldDelimiter(String fieldDelimiter) {
		this.fieldDelimiter = fieldDelimiter;
		return this;
	}

	public String getIterableStart() {
		return iterableStart;
	}

	public TemplateConfiguration setIterableStart(String iterableStart) {
		this.iterableStart = iterableStart;
		return this;
	}

	public String getIterableEnd() {
		return iterableEnd;
	}

	public TemplateConfiguration setIterableEnd(String iterableEnd) {
		this.iterableEnd = iterableEnd;
		return this;
	}

	public String getIterableDelimiter() {
		return iterableDelimiter;
	}

	public TemplateConfiguration setIterableDelimiter(String iterableDelimiter) {
		this.iterableDelimiter = iterableDelimiter;
		return this;
	}

	public String getArrayStart() {
		return arrayStart;
	}

	public TemplateConfiguration setArrayStart(String arrayStart) {
		this.arrayStart = arrayStart;
		return this;
	}

	public String getArrayEnd() {
		return arrayEnd;
	}

	public TemplateConfiguration setArrayEnd(String arrayEnd) {
		this.arrayEnd = arrayEnd;
		return this;
	}

	public String getArrayDelimiter() {
		return arrayDelimiter;
	}

	public TemplateConfiguration setArrayDelimiter(String arrayDelimiter) {
		this.arrayDelimiter = arrayDelimiter;
		return this;
	}
}
