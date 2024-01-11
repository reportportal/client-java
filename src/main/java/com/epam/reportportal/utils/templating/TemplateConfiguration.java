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

import com.epam.reportportal.annotations.TemplateConfig;

import java.util.Objects;

/**
 * Template configuration holder class. With the help of {@link TemplateConfig} annotation one can configure every
 * aspect of template keywords and special characters.
 */
public class TemplateConfiguration {
	public static final String CLASS_SIMPLE_NAME_TEMPLATE = "class";
	public static final String CLASS_FULL_NAME_TEMPLATE = "classRef";
	public static final String METHOD_NAME_TEMPLATE = "method";
	public static final String SELF_NAME_TEMPLATE = "this";
	public static final String FIELD_REFERENCE_DELIMITER = ".";
	public static final String ITERABLE_START_PATTERN = "[";
	public static final String ITERABLE_END_PATTERN = "]";
	public static final String ITERABLE_ELEMENT_DELIMITER = ", ";
	public static final String ARRAY_START_PATTERN = "{";
	public static final String ARRAY_END_PATTERN = "}";
	public static final String ARRAY_ELEMENT_DELIMITER = ", ";

	private String className;
	private String classRef;
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
		className = CLASS_SIMPLE_NAME_TEMPLATE;
		classRef = CLASS_FULL_NAME_TEMPLATE;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TemplateConfiguration)) {
			return false;
		}
		TemplateConfiguration that = (TemplateConfiguration) o;
		return className.equals(that.className) && classRef.equals(that.classRef) && methodName.equals(that.methodName)
				&& selfName.equals(that.selfName) && fieldDelimiter.equals(that.fieldDelimiter) && iterableStart.equals(
				that.iterableStart) && iterableEnd.equals(that.iterableEnd)
				&& iterableDelimiter.equals(that.iterableDelimiter) && arrayStart.equals(that.arrayStart)
				&& arrayEnd.equals(that.arrayEnd) && arrayDelimiter.equals(that.arrayDelimiter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				className,
				classRef,
				methodName,
				selfName,
				fieldDelimiter,
				iterableStart,
				iterableEnd,
				iterableDelimiter,
				arrayStart,
				arrayEnd,
				arrayDelimiter
		);
	}

	public TemplateConfiguration(TemplateConfig config) {
		className = config.classNameTemplate();
		classRef = config.classRefTemplate();
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

	public String getClassName() {
		return className;
	}

	public TemplateConfiguration setClassName(String className) {
		this.className = className;
		return this;
	}

	public String getClassRef() {
		return classRef;
	}

	public TemplateConfiguration setClassRef(String classRef) {
		this.classRef = classRef;
		return this;
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
