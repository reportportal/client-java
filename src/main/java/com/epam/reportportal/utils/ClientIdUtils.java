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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;

/**
 * A static class which contains methods to get current Client ID.
 */
public class ClientIdUtils {
	private ClientIdUtils() {
		throw new IllegalStateException("Static only class");
	}


	private static final String CLIENT_ID_PROPERTY = "client.id";
	private static final Path LOCAL_DATA_STORAGE = Paths.get(System.getProperty("user.home"), ".rp", "rp.properties");

	@Nullable
	private static String readClientId() {
		Properties properties = new Properties();
		if (Files.exists(LOCAL_DATA_STORAGE)) {
			try {
				properties.load(Files.newInputStream(LOCAL_DATA_STORAGE, StandardOpenOption.READ));
			} catch (IOException ignore) {
				// do nothing on read error, client ID will be always new
			}
			return properties.getProperty(CLIENT_ID_PROPERTY);
		}
		return null;
	}

	private static void storeClientId(@Nonnull String clientId) {
		Path folder = LOCAL_DATA_STORAGE.getParent();
		try {
			if (!Files.exists(folder)) {
				Files.createDirectories(folder);
			}
			Properties properties = new Properties();
			if (Files.exists(LOCAL_DATA_STORAGE)) {
				properties.load(Files.newInputStream(LOCAL_DATA_STORAGE, StandardOpenOption.READ));
			}
			properties.put(CLIENT_ID_PROPERTY, clientId);
			properties.store(Files.newOutputStream(LOCAL_DATA_STORAGE, StandardOpenOption.CREATE), null);
		} catch (IOException ignore) {
			// do nothing on saving error, client ID will be always new
		}
	}

	/**
	 * Return current Client ID, generate new in case of errors.
	 *
	 * @return Client ID String
	 */
	@Nonnull
	public static String getClientId() {
		String clientId = readClientId();
		if (clientId == null) {
			clientId = UUID.randomUUID().toString();
			storeClientId(clientId);
		}
		return clientId;
	}
}
