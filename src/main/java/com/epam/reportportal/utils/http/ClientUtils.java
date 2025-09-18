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

package com.epam.reportportal.utils.http;

import com.epam.reportportal.listeners.ListenerParameters;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Common utility code for {@link okhttp3.OkHttpClient}
 */
public class ClientUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientUtils.class);

	private ClientUtils() {
		throw new IllegalStateException("Static only class");
	}

	@Nonnull
	public static OkHttpClient.Builder setupProxy(@Nonnull OkHttpClient.Builder builder, @Nonnull ListenerParameters parameters) {
		String proxyStr = parameters.getProxyUrl();
		if (isBlank(proxyStr)) {
			return builder;
		}
		try {
			URL proxyUrl = new URL(proxyStr);
			int port = proxyUrl.getPort();
			builder.proxy(new Proxy(
					Proxy.Type.HTTP,
					InetSocketAddress.createUnresolved(proxyUrl.getHost(), port >= 0 ? port : proxyUrl.getDefaultPort())
			));
			String proxyUser = parameters.getProxyUser();
			if (isNotBlank(proxyUser)) {
				builder.proxyAuthenticator((route, response) -> {
					String credential = Credentials.basic(proxyUser, parameters.getProxyPassword(), StandardCharsets.UTF_8);
					return response.request().newBuilder().header("Proxy-Authorization", credential).build();
				});
			}
		} catch (MalformedURLException e) {
			LOGGER.warn("Unable to parse proxy URL", e);
			return builder;
		}
		return builder;
	}
}
