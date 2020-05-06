/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.utils.properties;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public enum DefaultProperties implements PropertyHolder {
	OS("os", true, "os.name", "os.arch", "os.version"),
	JVM("jvm", true, "java.vm.name", "java.version", "java.class.version"),
	AGENT("agent", false, "agent.name", "agent.version");

	private String name;
	private boolean internal;
	private String[] propertyKeys;

	DefaultProperties(String name, boolean internal, String... propertyKeys) {
		this.name = name;
		this.internal = internal;
		this.propertyKeys = propertyKeys;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isInternal() {
		return internal;
	}

	@Override
	public String[] getPropertyKeys() {
		return propertyKeys;
	}
}
