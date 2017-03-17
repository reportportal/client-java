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

import com.epam.reportportal.utils.properties.ListenerProperty;

/**
 * Wraps {@link ListenerProperty} to it's implementation. Grabbed from Google
 * <a href="http://google.github.io/guice/api-docs/3.0/javadoc/com/google/inject/name/Names.html">Names</a>
 * 
 * @author Andrei Varabyeu
 * 
 */
public class ListenerPropertyBinder {

	/**
	 * We don't need to create instance of this class since it contains only
	 * static methods
	 */
	private ListenerPropertyBinder() {
	}

	public static ListenerPropertyValue named(ListenerProperty listenerProperty) {
		return new ListenerPropertyValueImpl(listenerProperty);
	}
}
