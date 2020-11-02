/*
 * Copyright 2019 EPAM Systems
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

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class PropertiesLoaderTest {

	@Test
	public void testFullReloadProperties() {
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

	@Test
	public void testOverride() {
		Properties properties = new Properties();
		String propertyKey = ListenerProperty.DESCRIPTION.getPropertyName();
		properties.setProperty(propertyKey, "testvalue");

		PropertiesLoader.overrideWith(properties, ImmutableMap.<String, String>builder().put(propertyKey, "anothervalue").build());
		assertThat("Incorrect override behaviour", properties.getProperty(propertyKey), equalTo("anothervalue"));

		Properties overrides = new Properties();
		overrides.setProperty(propertyKey, "overridenFromPropertiesObject");
		PropertiesLoader.overrideWith(properties, overrides);
		assertThat("Incorrect override behaviour", properties.getProperty(propertyKey), equalTo("overridenFromPropertiesObject"));
	}

	@Test
	public void testOverrideVariableWithUnderscore() {
		Properties properties = new Properties();
		properties.setProperty("rp.description", "testvalue");

		PropertiesLoader.overrideWith(properties, ImmutableMap.<String, String>builder().put("rp_description", "anothervalue").build());
		assertThat(
				"Incorrect override behaviour",
				properties.getProperty(ListenerProperty.DESCRIPTION.getPropertyName()),
				equalTo("anothervalue")
		);
	}

	@Test
	public void test_loader_ignores_upper_case() {
		Properties properties = new Properties();
		properties.setProperty("rp.description", "testvalue");

		PropertiesLoader.overrideWith(properties, ImmutableMap.<String, String>builder().put("RP_DESCRIPTION", "anothervalue").build());
		assertThat(
				"Incorrect override behaviour",
				properties.getProperty(ListenerProperty.DESCRIPTION.getPropertyName()),
				equalTo("anothervalue")
		);
	}


	@Test
	public void testUtf() {
		assertThat("Incorrect encoding!", PropertiesLoader.load("utf-demo.properties").getProperty("utf8"), is("привет мир!"));
	}
}
