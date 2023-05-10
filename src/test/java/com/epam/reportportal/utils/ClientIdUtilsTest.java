/*
 *  Copyright 2023 EPAM Systems
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

package com.epam.reportportal.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.epam.reportportal.utils.ClientIdUtils.RP_PROPERTIES_FILE_PATH;
import static com.epam.reportportal.utils.ClientIdUtils.getClientId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientIdUtilsTest {

	@BeforeAll
	public static void makeDirs() throws IOException {
		Files.createDirectories(RP_PROPERTIES_FILE_PATH.getParent());
	}

	@Test
	public void test_get_client_id_should_return_the_id_for_two_calls() {
		String clientId1 = getClientId();
		String clientId2 = getClientId();

		assertThat(clientId2, equalTo(clientId1));
	}

	@Test
	public void test_get_client_id_should_return_different_ids_if_store_file_removed() throws IOException {
		String clientId1 = getClientId();
		Files.delete(RP_PROPERTIES_FILE_PATH);
		String clientId2 = getClientId();

		assertThat(clientId2, not(equalTo(clientId1)));
	}

	@Test
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void test_get_client_id_should_return_uuid() {
		String clientId = getClientId();
		UUID.fromString(clientId);
	}

	@Test
	public void test_get_client_id_should_save_id_to_property_file() throws IOException {
		Files.deleteIfExists(RP_PROPERTIES_FILE_PATH);
		String clientId = getClientId();
		List<String> lines = Files.readAllLines(RP_PROPERTIES_FILE_PATH);
		assertThat(lines, hasItem(matchesRegex("^client\\.id\\s*=\\s*" + clientId + "\\s*$")));
	}

	@Test
	public void test_get_client_id_should_read_id_from_property_file() throws IOException {
		Files.deleteIfExists(RP_PROPERTIES_FILE_PATH);
		String clientId = UUID.randomUUID().toString();
		Files.write(RP_PROPERTIES_FILE_PATH, Collections.singletonList("client.id=" + clientId), StandardCharsets.UTF_8,
				StandardOpenOption.CREATE);
		String actualClientId = getClientId();
		assertThat(actualClientId, equalTo(clientId));
	}

	@Test
	public void test_get_client_id_should_read_id_from_property_file_if_not_empty_and_id_is_the_first_line()
			throws IOException {
		Files.deleteIfExists(RP_PROPERTIES_FILE_PATH);
		String clientId = UUID.randomUUID().toString();
		Files.write(RP_PROPERTIES_FILE_PATH, Arrays.asList("client.id=" + clientId, "test.property=555"),
				StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		String actualClientId = getClientId();
		assertThat(actualClientId, equalTo(clientId));
	}

	@Test
	public void test_get_client_id_should_read_id_from_property_file_if_not_empty_and_id_is_not_the_first_line()
			throws IOException {
		Files.deleteIfExists(RP_PROPERTIES_FILE_PATH);
		String clientId = UUID.randomUUID().toString();
		Files.write(RP_PROPERTIES_FILE_PATH, Arrays.asList("test.property=555", "client.id=" + clientId),
				StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		String actualClientId = getClientId();
		assertThat(actualClientId, equalTo(clientId));
	}

	@Test
	public void test_get_client_id_should_write_id_to_property_file_if_it_is_not_empty() throws IOException {
		Files.deleteIfExists(RP_PROPERTIES_FILE_PATH);
		Files.write(RP_PROPERTIES_FILE_PATH, Collections.singletonList("test.property=555"), StandardCharsets.UTF_8);
		String clientId = getClientId();
		List<String> lines = Files.readAllLines(RP_PROPERTIES_FILE_PATH);
		assertThat(lines, hasItems(matchesRegex("^client\\.id\\s*=\\s*" + clientId + "\\s*$"),
				equalTo("test.property=555")));
	}
}
