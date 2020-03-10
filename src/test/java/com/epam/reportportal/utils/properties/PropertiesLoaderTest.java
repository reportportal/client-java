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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.Matchers.is;

public class PropertiesLoaderTest {

	@Test
	public void testFullReloadProperties() {
		Properties props = new Properties();
		for (ListenerProperty listenerProperties : ListenerProperty.values()) {
			props.setProperty(listenerProperties.getPropertyName(), listenerProperties.getPropertyName());
		}

		System.setProperties(props);

		Properties loadedProps = null;
		try {
			loadedProps = PropertiesLoader.load().getProperties();
		} catch (InternalReportPortalClientException e) {
			Assert.fail("Unable to load properties: " + e.getLocalizedMessage());
		}

		for (ListenerProperty listenerProperties : ListenerProperty.values()) {
			Assert.assertEquals(listenerProperties.getPropertyName(), loadedProps.getProperty(listenerProperties.getPropertyName()));
		}

	}

	@Test
	public void testOverride() {
		Properties properties = new Properties();
		String propertyKey = ListenerProperty.DESCRIPTION.getPropertyName();
		properties.setProperty(propertyKey, "testvalue");

		PropertiesLoader.overrideWith(properties, ImmutableMap.<String, String>builder().put(propertyKey, "anothervalue").build());
		Assert.assertEquals("Incorrect override behaviour", "anothervalue", properties.getProperty(propertyKey));

		Properties overrides = new Properties();
		overrides.setProperty(propertyKey, "overridenFromPropertiesObject");
		PropertiesLoader.overrideWith(properties, overrides);
		Assert.assertEquals("Incorrect override behaviour", properties.getProperty(propertyKey), "overridenFromPropertiesObject");
	}

	@Test
	public void testOverrideVariableWithUnderscore() {
		Properties properties = new Properties();
		properties.setProperty("rp.description", "testvalue");

		PropertiesLoader.overrideWith(properties, ImmutableMap.<String, String>builder().put("rp_description", "anothervalue").build());
		Assert.assertEquals("Incorrect override behaviour", "anothervalue", properties.getProperty(ListenerProperty.DESCRIPTION.getPropertyName()));

	}

	@Test
	public void testUtf() {
		Assert.assertThat("Incorrect encoding!", PropertiesLoader.load("utf-demo.properties").getProperty("utf8"), is("привет мир!"));
	}
}
