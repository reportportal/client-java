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
 * Implementation of the {@link PropertyHolder} with `client name and version` attribute names:
 * 1) internal - from the environment (OS, JVM)
 * 2) external - from `*.properties` file
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public enum ClientProperties implements PropertyHolder {
	CLIENT("client", false, "client.name", "client.version");

	private String name;
	private boolean internal;
	private String[] propertyKeys;

	ClientProperties(String name, boolean internal, String... propertyKeys) {
		this.name = name;
		this.internal = internal;
		this.propertyKeys = propertyKeys;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getPropertyKeys() {
		return propertyKeys;
	}

	@Override
	public boolean isInternal() {
		return internal;
	}
}
