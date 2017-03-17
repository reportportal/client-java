/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.guice;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import com.epam.reportportal.utils.properties.ListenerProperty;
import com.google.common.base.Preconditions;

/**
 * Implementation of
 * {@link ListenerPropertyValue}
 * annotation. This is not good approach from Java-style, but only one
 * possibility to create annotation similar to Google's
 * <a href="http://google.github.io/guice/api-docs/3.0/javadoc/com/google/inject/name/Named.html">Named</a>. Grabbed from Google
 * {@link com.google.inject.name.NamedImpl}
 * 
 * @author Andrei Varabyeu
 * 
 */
public class ListenerPropertyValueImpl implements ListenerPropertyValue, Serializable {

	private final ListenerProperty value;

	public ListenerPropertyValueImpl(ListenerProperty value) {
		this.value = Preconditions.checkNotNull(value, "name");
	}

	@Override
	public ListenerProperty value() {
		return this.value;
	}

	@Override
	public int hashCode() {
		// This is specified in java.lang.Annotation.
		return (127 * "value".hashCode()) ^ value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ListenerPropertyValue)) {
			return false;
		}

		ListenerPropertyValue other = (ListenerPropertyValue) o;
		return value.equals(other.value());
	}

	@Override
	public String toString() {
		return "@" + ListenerPropertyValue.class.getName() + "(value=" + value + ")";
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ListenerPropertyValue.class;
	}

	private static final long serialVersionUID = 0;

}
