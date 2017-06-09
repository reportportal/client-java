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
package com.epam.reportportal.utils.properties;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.Matchers.is;

public class PropertiesLoaderTest {

	@Test
	public void testFullReloadProperties() throws IOException {
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
	public void testOverride() throws IOException {
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
	public void testUtf() throws IOException {
		Assert.assertThat("Incorrect encoding!", PropertiesLoader.load("utf-demo.properties").getProperty("utf8"), is("привет мир!"));

	}
}
