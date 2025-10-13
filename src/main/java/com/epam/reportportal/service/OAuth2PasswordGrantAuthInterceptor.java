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
package com.epam.reportportal.service;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.http.ClientUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OkHttp interceptor that handles OAuth 2.0 Password Grant authentication.
 * This interceptor automatically obtains and refreshes access tokens using the OAuth 2.0 password grant flow.
 *
 * @see <a href="https://www.oauth.com/oauth2-servers/access-tokens/password-grant/">OAuth 2.0 Password Grant</a>
 */
public class OAuth2PasswordGrantAuthInterceptor implements Interceptor {

	/**
	 * AutoCloseable wrapper for Lock that enables try-with-resources usage.
	 */
	private static class LockCloseable implements AutoCloseable {
		private final Lock lock;

		private LockCloseable(Lock lock) {
			this.lock = lock;
		}

		/**
		 * Locks the underlying lock and returns this instance for use in try-with-resources.
		 *
		 * @return this LockCloseable instance
		 */
		public LockCloseable lock() {
			lock.lock();
			return this;
		}

		@Override
		public void close() {
			lock.unlock();
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2PasswordGrantAuthInterceptor.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final long TOKEN_EXPIRY_BUFFER_SECONDS = TimeUnit.MINUTES.toMillis(1);

	private final ListenerParameters parameters;

	private volatile String accessToken;
	private volatile String refreshToken;
	private volatile long tokenExpiryTime;
	private final LockCloseable tokenLock = new LockCloseable(new ReentrantLock());

	/**
	 * Creates a new OAuth 2.0 Password Grant interceptor.
	 *
	 * @param parameters The listener parameters containing OAuth 2.0 and proxy configuration
	 */
	public OAuth2PasswordGrantAuthInterceptor(@Nonnull ListenerParameters parameters) {
		this.parameters = parameters;
	}

	@Override
	@Nonnull
	public Response intercept(@Nonnull Chain chain) throws IOException {
		ensureValidToken(chain);

		Request request = chain.request().newBuilder().addHeader("Authorization", "Bearer " + accessToken).build();

		return chain.proceed(request);
	}

	/**
	 * Ensures that a valid access token is available. If the token is expired or not yet obtained,
	 * this method will obtain a new one or refresh the existing one.
	 *
	 * @param chain The OkHttp interceptor chain
	 */
	private void ensureValidToken(@Nonnull Chain chain) {
		// Check if token is valid without acquiring lock for better performance
		if (isTokenValid()) {
			return;
		}

		try (LockCloseable ignored = tokenLock.lock()) {
			// Double-check after acquiring lock
			if (isTokenValid()) {
				return;
			}

			if (refreshToken != null) {
				try {
					refreshAccessToken(chain);
					return;
				} catch (Exception e) {
					LOGGER.warn("Failed to refresh access token, attempting to obtain new token", e);
				}
			}

			obtainAccessToken(chain);
		} catch (Exception e) {
			LOGGER.error("Failed to obtain OAuth 2.0 access token", e);
		}
	}

	/**
	 * Checks if the current access token is valid (exists and not expired).
	 *
	 * @return true if the token is valid, false otherwise
	 */
	private boolean isTokenValid() {
		return accessToken != null && System.currentTimeMillis() < (tokenExpiryTime - TOKEN_EXPIRY_BUFFER_SECONDS);
	}

	/**
	 * Obtains a new access token using the password grant flow.
	 *
	 * @param chain The OkHttp interceptor chain
	 */
	private void obtainAccessToken(@Nonnull Chain chain) throws IOException {
		FormBody.Builder formBuilder = new FormBody.Builder().add("grant_type", "password")
				.add("username", Objects.requireNonNull(parameters.getOauthUsername()))
				.add("password", Objects.requireNonNull(parameters.getOauthPassword()))
				.add("client_id", Objects.requireNonNull(parameters.getOauthClientId()));

		String clientSecret = parameters.getOauthClientSecret();
		if (clientSecret != null) {
			formBuilder.add("client_secret", clientSecret);
		}

		String scope = parameters.getOauthScope();
		if (scope != null) {
			formBuilder.add("scope", scope);
		}

		RequestBody formBody = formBuilder.build();
		Request tokenRequest = new Request.Builder().url(Objects.requireNonNull(parameters.getOauthTokenUri())).post(formBody).build();

		executeTokenRequest(chain, tokenRequest);
	}

	/**
	 * Refreshes the access token using the refresh token.
	 *
	 * @param chain The OkHttp interceptor chain
	 */
	private void refreshAccessToken(@Nonnull Chain chain) throws IOException {
		FormBody.Builder formBuilder = new FormBody.Builder().add("grant_type", "refresh_token")
				.add("refresh_token", refreshToken)
				.add("client_id", Objects.requireNonNull(parameters.getOauthClientId()));

		String clientSecret = parameters.getOauthClientSecret();
		if (clientSecret != null) {
			formBuilder.add("client_secret", clientSecret);
		}

		RequestBody formBody = formBuilder.build();
		Request tokenRequest = new Request.Builder().url(Objects.requireNonNull(parameters.getOauthTokenUri())).post(formBody).build();

		executeTokenRequest(chain, tokenRequest);
	}

	/**
	 * Executes a token request and updates the internal token state.
	 *
	 * @param chain        The OkHttp interceptor chain
	 * @param tokenRequest The token request to execute
	 */
	private void executeTokenRequest(@Nonnull Chain chain, @Nonnull Request tokenRequest) throws IOException {
		// Create a new client without this interceptor to avoid infinite recursion
		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().connectTimeout(
						chain.connectTimeoutMillis(),
						java.util.concurrent.TimeUnit.MILLISECONDS
				)
				.readTimeout(chain.readTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
				.writeTimeout(chain.writeTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

		// Setup proxy if configured
		clientBuilder = ClientUtils.setupProxy(clientBuilder, parameters);
		if (clientBuilder == null) {
			LOGGER.error("Failed to setup proxy for OAuth 2.0 token request");
			throw new IOException("Failed to setup proxy for OAuth 2.0 token request");
		}

		OkHttpClient client = clientBuilder.build();

		try (Response response = client.newCall(tokenRequest).execute()) {
			if (!response.isSuccessful()) {
				String errorBody = response.body() != null ? response.body().string() : "No response body";
				LOGGER.error("Failed to obtain OAuth 2.0 token. Status: {}, Body: {}", response.code(), errorBody);
				throw new IOException("Failed to obtain OAuth 2.0 token. Status: " + response.code());
			}

			String responseBody = response.body() != null ? response.body().string() : "{}";
			parseTokenResponse(responseBody);
		}
	}

	/**
	 * Parses the OAuth 2.0 token response and updates the internal token state.
	 *
	 * @param responseBody The JSON response body from the token endpoint
	 */
	@SuppressWarnings("unchecked")
	private void parseTokenResponse(@Nonnull String responseBody) {
		try {
			Map<String, Object> tokenResponse = MAPPER.readValue(responseBody, Map.class);

			accessToken = (String) tokenResponse.get("access_token");
			if (accessToken == null) {
				LOGGER.error("OAuth 2.0 token response does not contain 'access_token' field");
				throw new IOException("Invalid OAuth 2.0 token response");
			}

			// Refresh token is optional in the response
			refreshToken = (String) tokenResponse.get("refresh_token");

			// Calculate token expiry time
			Object expiresIn = tokenResponse.get("expires_in");
			if (expiresIn != null) {
				long expiresInSeconds = expiresIn instanceof Number ?
						((Number) expiresIn).longValue() :
						Long.parseLong(expiresIn.toString());
				tokenExpiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresInSeconds);
			} else {
				// Default to 1 hour if expires_in is not provided
				tokenExpiryTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
			}

			LOGGER.debug(
					"Successfully obtained OAuth 2.0 access token, expires in {} seconds",
					TimeUnit.MILLISECONDS.toSeconds(tokenExpiryTime - System.currentTimeMillis())
			);
		} catch (Exception e) {
			LOGGER.error("Failed to parse OAuth 2.0 token response", e);
		}
	}
}
