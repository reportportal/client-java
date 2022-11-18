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
package com.epam.reportportal.listeners;

import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;

import static com.epam.ta.reportportal.ws.model.launch.Mode.DEBUG;
import static com.epam.ta.reportportal.ws.model.launch.Mode.DEFAULT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ListenerParametersTest {

	@Test
	public void testParseLaunchMode() {
		assertThat(new ListenerParameters().parseLaunchMode("notvalid"), equalTo(DEFAULT));
		assertThat(new ListenerParameters().parseLaunchMode("Debug"), equalTo(DEBUG));
	}

	@Test
	public void testNoNPEs() {
		PropertiesLoader properties = PropertiesLoader.load("test.null.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertThat(listenerParameters.getBaseUrl(), nullValue());
		assertThat(listenerParameters.getApiKey(), nullValue());
	}

	@Test
	public void testOnlyApiKeyProvided() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/reportportal-api-key.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals("test-api-key", listenerParameters.getApiKey());
	}

	@Test
	public void testOnlyUuidProvided() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/reportportal-uuid.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals("test-uuid", listenerParameters.getApiKey());
	}

	@Test
	public void parametersCloneTest() {
		ListenerParameters params = TestUtils.STANDARD_PARAMETERS;
		ListenerParameters paramsClone = params.clone();

		assertThat(paramsClone, not(sameInstance(params)));
		assertThat(paramsClone.getLaunchName(), equalTo(params.getLaunchName()));
		assertThat(paramsClone.getEnable(), equalTo(params.getEnable()));
		assertThat(paramsClone.getClientJoin(), equalTo(params.getClientJoin()));
		assertThat(paramsClone.getBatchLogsSize(), equalTo(params.getBatchLogsSize()));
	}

	/*
	 * We heavily rely on a fact that ListenerParameters is a POJO with no additional hidden logic and has a public constructor with no
	 * parameters. Eg. a clone test above.
	 */
	@Test
	public void verify_parameters_have_empty_public_constructor() throws NoSuchMethodException {
		//noinspection ResultOfMethodCallIgnored
		ListenerParameters.class.getConstructor();
	}

	@Test
	public void test_rx_buffer_size_property_file_bypass() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/reportportal-rx-size.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(1024, listenerParameters.getRxBufferSize());
	}

	@Test
	public void test_rx_buffer_size_system_property_bypass() {
		System.setProperty("rx2.buffer-size", "1024");
		ListenerParameters listenerParameters = TestUtils.standardParameters();

		assertEquals(1024, listenerParameters.getRxBufferSize());
		System.clearProperty("rx2.buffer-size");
	}

	@Test
	public void test_rx_buffer_size_system_property_override() {
		System.setProperty("rx2.buffer-size", "10");
		PropertiesLoader properties = PropertiesLoader.load("property-test/reportportal-rx-size.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(10, listenerParameters.getRxBufferSize());
		System.clearProperty("rx2.buffer-size");
	}

	@Test
	public void test_item_name_truncation_default_values() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/utf-demo.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertTrue(listenerParameters.isTruncateItemNames());
		assertEquals(1024, listenerParameters.getTruncateItemNamesLimit());
		assertEquals("...", listenerParameters.getTruncateItemNamesReplacement());
	}

	@Test
	public void test_item_name_truncation_property_file_bypass() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/reportportal-item-names-truncation.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertFalse(listenerParameters.isTruncateItemNames());
		assertEquals(512, listenerParameters.getTruncateItemNamesLimit());
		assertEquals("\\", listenerParameters.getTruncateItemNamesReplacement());
	}

	@Test
	public void test_generic_truncation_property_file_bypass() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/generic-truncation-properties.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertFalse(listenerParameters.isTruncateFields());
		assertEquals(512, listenerParameters.getTruncateItemNamesLimit());
		assertEquals(64, listenerParameters.getAttributeLengthLimit());
		assertEquals("\\", listenerParameters.getTruncateReplacement());
	}

	@Test
	public void verify_lock_wait_timeout_property_set_if_it_not_present_in_property_file() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/default-required.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(ListenerParameters.DEFAULT_FILE_WAIT_TIMEOUT_MS, listenerParameters.getLockWaitTimeout());
	}

	@Test
	public void verify_lock_wait_timeout_property_set_by_file_wait_property() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/lock-wait-file.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(15223, listenerParameters.getLockWaitTimeout());
	}

	@Test
	public void verify_lock_wait_timeout_property_set_by_client_join_lock_wait_property() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/lock-wait-client_lock.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(17000, listenerParameters.getLockWaitTimeout());
	}

	@Test
	public void verify_lock_wait_timeout_property_client_join_lock_wait_property_override() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/lock-wait-client_lock_override.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(19000, listenerParameters.getLockWaitTimeout());
	}

	@Test
	public void verify_lock_wait_timeout_property_set_by_client_join_lock_wait_property_no_time_unit() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/lock-wait-client_lock_no_time_unit.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertEquals(21, listenerParameters.getLockWaitTimeout());
	}


	@Test
	public void verify_http_timeout_values_bypass() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/http-timeout.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		Duration expected = Duration.ofSeconds(100);
		assertEquals(expected, listenerParameters.getHttpCallTimeout());
		assertEquals(expected, listenerParameters.getHttpConnectTimeout());
		assertEquals(expected, listenerParameters.getHttpReadTimeout());
		assertEquals(expected, listenerParameters.getHttpWriteTimeout());
	}

	@Test
	public void verify_call_timeout_unit_bypass() {
		PropertiesLoader properties = PropertiesLoader.load("property-test/http-timeout-unit.properties");
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		Duration expected = Duration.ofMillis(1);
		assertEquals(expected, listenerParameters.getHttpCallTimeout());
		assertEquals(expected, listenerParameters.getHttpConnectTimeout());
		assertEquals(expected, listenerParameters.getHttpReadTimeout());
		assertEquals(expected, listenerParameters.getHttpWriteTimeout());
	}
}
