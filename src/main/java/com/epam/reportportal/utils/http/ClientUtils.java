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
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
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

	@Nonnull
	public static OkHttpClient.Builder setupProxy(@Nonnull OkHttpClient.Builder builder, @Nonnull ListenerParameters parameters) {
		String proxyStr = parameters.getProxyUrl();
		if (isBlank(proxyStr)) {
			return builder;
		}
		URL proxyUrl;
		try {
			proxyUrl = new URL(proxyStr);
		} catch (MalformedURLException e) {
			throw new InternalReportPortalClientException("Unable to parse proxy URL", e);
		}
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
		return builder;
	}

	@Nonnull
	public static OkHttpClient.Builder setupSsl(@Nonnull OkHttpClient.Builder builder, URL baseUrl,
			@Nonnull ListenerParameters parameters) {
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

	private static boolean isOAuthConfigured(@Nonnull ListenerParameters parameters) {
		return StringUtils.isNotBlank(parameters.getOauthTokenUri()) && StringUtils.isNotBlank(parameters.getOauthUsername())
				&& StringUtils.isNotBlank(parameters.getOauthPassword()) && StringUtils.isNotBlank(parameters.getOauthClientId());
	}

	@Nonnull
	private static Interceptor createAuthInterceptor(@Nonnull ListenerParameters parameters) {
		// Check if OAuth 2.0 is configured (takes precedence over API key)
		if (isOAuthConfigured(parameters)) {
			return new OAuth2PasswordGrantAuthInterceptor(parameters);
		}

		// Fall back to API key authentication
		if (StringUtils.isNotBlank(parameters.getApiKey())) {
			return new BearerAuthInterceptor(parameters.getApiKey());
		}

		// No authentication configured
		throw new InternalReportPortalClientException("Neither OAuth 2.0 nor API key authentication is configured");
	}

	@Nonnull
	public static OkHttpClient.Builder setupAuthInterceptor(@Nonnull OkHttpClient.Builder builder, @Nonnull ListenerParameters parameters) {
		return builder.addInterceptor(ClientUtils.createAuthInterceptor(parameters));
	}

	@Nonnull
	public static OkHttpClient.Builder setupHttpLoggingInterceptor(@Nonnull OkHttpClient.Builder builder,
			@Nonnull ListenerParameters parameters) {
		if (!parameters.isHttpLogging()) {
			return builder;
		}
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.BODY);
		return builder.addNetworkInterceptor(logging);
	}
}
