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

package com.epam.reportportal.utils.properties;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PropertiesLoaderReloadTest {

	@Test
	public void testFullReloadProperties() {
		PropertiesLoader.load(); // avoid initialization exception

		Properties props = new Properties();
		for (ListenerProperty listenerProperties : ListenerProperty.values()) {
			props.setProperty(listenerProperties.getPropertyName(), listenerProperties.getPropertyName());
		}

		System.setProperties(props);

		Properties loadedProps = PropertiesLoader.load().getProperties();

		for (ListenerProperty listenerProperties : ListenerProperty.values()) {
			assertThat(loadedProps.getProperty(listenerProperties.getPropertyName()), equalTo(listenerProperties.getPropertyName()));
		}

	}
}
