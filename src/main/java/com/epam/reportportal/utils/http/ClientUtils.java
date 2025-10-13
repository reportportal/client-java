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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.BearerAuthInterceptor;
import com.epam.reportportal.service.OAuth2PasswordGrantAuthInterceptor;
import com.epam.reportportal.utils.SslUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Common utility code for {@link okhttp3.OkHttpClient}
 */
public class ClientUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientUtils.class);

	private static final String HTTPS = "https";

	private ClientUtils() {
		throw new IllegalStateException("Static only class");
	}

	@Nullable
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
			return null;
		}
		return builder;
	}

	@Nullable
	public static OkHttpClient.Builder setupSsl(@Nonnull OkHttpClient.Builder builder, @Nonnull ListenerParameters parameters) {
		String baseUrlStr = parameters.getBaseUrl();
		if (baseUrlStr == null) {
			LOGGER.warn("Base url for ReportPortal server is not set!");
			return null;
		}

		URL baseUrl;
		try {
			baseUrl = new URL(baseUrlStr);
		} catch (MalformedURLException e) {
			LOGGER.warn("Unable to parse ReportPortal URL", e);
			return null;
		}

		String keyStore = parameters.getKeystore();
		String keyStorePassword = parameters.getKeystorePassword();
		String trustStore = parameters.getTruststore();
		String trustStorePassword = parameters.getTruststorePassword();

		if (HTTPS.equals(baseUrl.getProtocol()) && (keyStore != null || trustStore != null)) {
			KeyManager[] keyManagers = null;
			if (keyStore != null) {
				KeyStore ks = SslUtils.loadKeyStore(keyStore, keyStorePassword, parameters.getKeystoreType());
				try {
					KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					kmf.init(ks, ofNullable(keyStorePassword).map(String::toCharArray).orElse(null));
					keyManagers = kmf.getKeyManagers();
				} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
					String error = "Unable to load key store";
					LOGGER.error(error, e);
					throw new InternalReportPortalClientException(error, e);
				}
			}

			TrustManager[] trustManagers = null;
			if (trustStore != null) {
				KeyStore ts = SslUtils.loadKeyStore(trustStore, trustStorePassword, parameters.getTruststoreType());
				try {
					TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					tmf.init(ts);
					trustManagers = tmf.getTrustManagers();
				} catch (KeyStoreException | NoSuchAlgorithmException e) {
					String trustStoreError = "Unable to load trust store";
					LOGGER.error(trustStoreError, e);
					throw new InternalReportPortalClientException(trustStoreError, e);
				}
			}

			if (trustManagers == null) {
				try {
					TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					tmf.init((KeyStore) null);
					trustManagers = tmf.getTrustManagers();
				} catch (NoSuchAlgorithmException | KeyStoreException e) {
					String trustStoreError = "Unable to load default trust store";
					LOGGER.error(trustStoreError, e);
					throw new InternalReportPortalClientException(trustStoreError, e);
				}
			}

			try {
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyManagers, trustManagers, new SecureRandom());
				X509TrustManager trustManager = Arrays.stream(ofNullable(trustManagers).orElse(new TrustManager[] {}))
						.filter(m -> m instanceof X509TrustManager)
						.map(m -> (X509TrustManager) m)
						.findAny()
						.orElseThrow(() -> new InternalReportPortalClientException("Unable to find X509 trust manager"));
				builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
			} catch (NoSuchAlgorithmException | KeyManagementException e) {
				String error = "Unable to initialize SSL context";
				LOGGER.error(error, e);
				throw new InternalReportPortalClientException(error, e);
			}
		}
		return builder;
	}

	@Nullable
	private static Interceptor createAuthInterceptor(@Nonnull ListenerParameters parameters) {
		// Check if OAuth 2.0 is configured (takes precedence over API key)
		if (isOAuthConfigured(parameters)) {
			try {
				return new OAuth2PasswordGrantAuthInterceptor(parameters);
			} catch (Exception e) {
				LOGGER.error("Failed to create OAuth 2.0 authentication interceptor", e);
				return null;
			}
		}

		// Fall back to API key authentication
		if (StringUtils.isNotBlank(parameters.getApiKey())) {
			return new BearerAuthInterceptor(parameters.getApiKey());
		}

		// No authentication configured
		LOGGER.warn("Neither OAuth 2.0 nor API key authentication is configured");
		return null;
	}

	private static boolean isOAuthConfigured(@Nonnull ListenerParameters parameters) {
		return StringUtils.isNotBlank(parameters.getOauthTokenUri()) && StringUtils.isNotBlank(parameters.getOauthUsername())
				&& StringUtils.isNotBlank(parameters.getOauthPassword()) && StringUtils.isNotBlank(parameters.getOauthClientId());
	}

	@Nullable
	public static OkHttpClient.Builder setupAuthInterceptor(@Nonnull OkHttpClient.Builder builder, @Nonnull ListenerParameters parameters) {
		Interceptor authInterceptor = ClientUtils.createAuthInterceptor(parameters);
		if (authInterceptor == null) {
			return null;
		}
		builder.addInterceptor(authInterceptor);
		return builder;
	}
}
