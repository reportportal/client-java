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

/**
 * Template configuration holder class. With the help of {@link TemplateConfig} annotation one can configure every
 * aspect of template keywords and special characters.
 *
 * @deprecated use {@link com.epam.reportportal.utils.formatting.templating.TemplateConfiguration} annotation instead
 */
@Deprecated
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

	private final com.epam.reportportal.utils.formatting.templating.TemplateConfiguration delegate;

	public TemplateConfiguration() {
		delegate = new com.epam.reportportal.utils.formatting.templating.TemplateConfiguration();
	}

	public TemplateConfiguration(TemplateConfig config) {
		delegate = new com.epam.reportportal.utils.formatting.templating.TemplateConfiguration(config);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TemplateConfiguration that = (TemplateConfiguration) o;
		return delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public String getClassName() {
		return delegate.getClassName();
	}

	public TemplateConfiguration setClassName(String className) {
		delegate.setClassName(className);
		return this;
	}

	public String getClassRef() {
		return delegate.getClassRef();
	}

	public TemplateConfiguration setClassRef(String classRef) {
		delegate.setClassRef(classRef);
		return this;
	}

	public String getMethodName() {
		return delegate.getMethodName();
	}

	public TemplateConfiguration setMethodName(String methodName) {
		delegate.setMethodName(methodName);
		return this;
	}

	public String getSelfName() {
		return delegate.getSelfName();
	}

	public TemplateConfiguration setSelfName(String selfName) {
		delegate.setSelfName(selfName);
		return this;
	}

	public String getFieldDelimiter() {
		return delegate.getFieldDelimiter();
	}

	public TemplateConfiguration setFieldDelimiter(String fieldDelimiter) {
		delegate.setFieldDelimiter(fieldDelimiter);
		return this;
	}

	public String getIterableStart() {
		return delegate.getIterableStart();
	}

	public TemplateConfiguration setIterableStart(String iterableStart) {
		delegate.setIterableStart(iterableStart);
		return this;
	}

	public String getIterableEnd() {
		return delegate.getIterableEnd();
	}

	public TemplateConfiguration setIterableEnd(String iterableEnd) {
		delegate.setIterableEnd(iterableEnd);
		return this;
	}

	public String getIterableDelimiter() {
		return delegate.getIterableDelimiter();
	}

	public TemplateConfiguration setIterableDelimiter(String iterableDelimiter) {
		delegate.setIterableDelimiter(iterableDelimiter);
		return this;
	}

	public String getArrayStart() {
		return delegate.getArrayStart();
	}

	public TemplateConfiguration setArrayStart(String arrayStart) {
		delegate.setArrayStart(arrayStart);
		return this;
	}

	public String getArrayEnd() {
		return delegate.getArrayEnd();
	}

	public TemplateConfiguration setArrayEnd(String arrayEnd) {
		delegate.setArrayEnd(arrayEnd);
		return this;
	}

	public String getArrayDelimiter() {
		return delegate.getArrayDelimiter();
	}

	public TemplateConfiguration setArrayDelimiter(String arrayDelimiter) {
		this.delegate.setArrayDelimiter(arrayDelimiter);
		return this;
	}

	public com.epam.reportportal.utils.formatting.templating.TemplateConfiguration getDelegate() {
		return delegate;
	}
}
