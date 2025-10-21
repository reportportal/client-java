/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.service;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.concurrency.LockCloseable;
import com.epam.reportportal.utils.http.ClientUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Optional.ofNullable;

/**
 * OkHttp interceptor that handles OAuth 2.0 Password Grant authentication.
 * This interceptor automatically obtains and refreshes access tokens using the OAuth 2.0 password grant flow.
 *
 * @see <a href="https://www.oauth.com/oauth2-servers/access-tokens/password-grant/">OAuth 2.0 Password Grant</a>
 */
public class OAuth2PasswordGrantAuthInterceptor implements Interceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2PasswordGrantAuthInterceptor.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final long TOKEN_EXPIRY_BUFFER_MILLIS = TimeUnit.MINUTES.toMillis(1);

	private final ListenerParameters parameters;

	private volatile String accessToken;
	private volatile String refreshToken;
	private volatile long tokenExpiryTime;
	private volatile long lastAttemptTime;
	private volatile long lastAuthFailureTime;
	private final LockCloseable tokenLock = new LockCloseable(new ReentrantLock());
	private final OkHttpClient client;

	private static URL parseTokenUri(@Nonnull ListenerParameters parameters) {
		String tokenUrlStr = parameters.getOauthTokenUri();
		if (StringUtils.isBlank(tokenUrlStr)) {
			LOGGER.error("URL for oAuth token is not set!");
			throw new InternalReportPortalClientException("URL for oAuth token is not set!");
		}

		URL tokenUrl;
		try {
			tokenUrl = new URL(tokenUrlStr);
		} catch (MalformedURLException e) {
			LOGGER.error("Unable to parse oAuth token URL", e);
			throw new InternalReportPortalClientException("Unable to parse oAuth token URL", e);
		}
		return tokenUrl;
	}

	/**
	 * Creates a new OAuth 2.0 Password Grant interceptor.
	 *
	 * @param client     the HTTP client to use
	 * @param parameters The listener parameters containing OAuth 2.0 and proxy configuration
	 */
	public OAuth2PasswordGrantAuthInterceptor(@Nonnull OkHttpClient client, @Nonnull ListenerParameters parameters) {
		this.client = client;
		this.parameters = parameters;

		parseTokenUri(parameters);
	}

	/**
	 * Creates a new OAuth 2.0 Password Grant interceptor.
	 *
	 * @param parameters The listener parameters containing OAuth 2.0 and proxy configuration
	 */
	public OAuth2PasswordGrantAuthInterceptor(@Nonnull ListenerParameters parameters) {
		this.parameters = parameters;

		URL tokenUrl = parseTokenUri(parameters);

		OkHttpClient.Builder clientBuilder = ClientUtils.setupSsl(new OkHttpClient.Builder(), tokenUrl, parameters);
		ClientUtils.setupProxy(clientBuilder, parameters);

		ofNullable(parameters.getHttpConnectTimeout()).ifPresent(d -> clientBuilder.connectTimeout(d.toMillis(), TimeUnit.MILLISECONDS));
		ofNullable(parameters.getHttpReadTimeout()).ifPresent(d -> clientBuilder.readTimeout(d.toMillis(), TimeUnit.MILLISECONDS));
		ofNullable(parameters.getHttpWriteTimeout()).ifPresent(d -> clientBuilder.writeTimeout(d.toMillis(), TimeUnit.MILLISECONDS));
		ofNullable(parameters.getHttpCallTimeout()).ifPresent(d -> clientBuilder.callTimeout(d.toMillis(), TimeUnit.MILLISECONDS));

		client = clientBuilder.build();
	}

	/**
	 * Parses the OAuth 2.0 token response and updates the internal token state.
	 *
	 * @param responseBody The JSON response body from the token endpoint
	 * @return true if token was successfully parsed and set, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean parseTokenResponse(@Nonnull String responseBody) {
		Map<String, Object> tokenResponse;
		try {
			tokenResponse = MAPPER.readValue(responseBody, Map.class);
		} catch (JsonProcessingException e) {
			LOGGER.error("Failed to parse OAuth 2.0 token response", e);
			return false;
		}

		accessToken = (String) tokenResponse.get("access_token");
		if (accessToken == null) {
			LOGGER.error("OAuth 2.0 token response does not contain 'access_token' field");
			return false;
		}

		// Refresh token is optional in the response
		refreshToken = (String) tokenResponse.get("refresh_token");

		// Calculate token expiry time
		Object expiresIn = tokenResponse.get("expires_in");
		if (expiresIn != null) {
			long expiresInSeconds = expiresIn instanceof Number ? ((Number) expiresIn).longValue() : Long.parseLong(expiresIn.toString());
			tokenExpiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresInSeconds);
		} else {
			// Default to 1 hour if expires_in is not provided
			tokenExpiryTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
		}

		LOGGER.debug(
				"Successfully obtained OAuth 2.0 access token, expires in {} seconds",
				TimeUnit.MILLISECONDS.toSeconds(tokenExpiryTime - System.currentTimeMillis())
		);
		return true;
	}

	/**
	 * Executes a token request and updates the internal token state.
	 *
	 * @param tokenRequest The token request to execute
	 * @return true if token was successfully obtained and parsed, false otherwise
	 */
	private boolean executeTokenRequest(@Nonnull Request tokenRequest) {
		try (Response response = client.newCall(tokenRequest).execute()) {
			if (!response.isSuccessful()) {
				LOGGER.error("Failed to obtain OAuth 2.0 token. Status code: {}", response.code());
				return false;
			}

			String responseBody = response.body() != null ? response.body().string() : "{}";
			return parseTokenResponse(responseBody);
		} catch (IOException e) {
			LOGGER.error("Failed to obtain OAuth 2.0 token", e);
			return false;
		}
	}

	/**
	 * Checks if the current access token is valid (exists and not expired).
	 *
	 * @return true if the token is valid, false otherwise
	 */
	private boolean isTokenValid() {
		return accessToken != null && System.currentTimeMillis() < (tokenExpiryTime - TOKEN_EXPIRY_BUFFER_MILLIS);
	}

	/**
	 * Refreshes the access token using the refresh token.
	 *
	 * @return true if token was successfully refreshed, false otherwise
	 */
	private boolean refreshAccessToken() {
		lastAttemptTime = System.currentTimeMillis();
		FormBody.Builder formBuilder = new FormBody.Builder().add("grant_type", "refresh_token")
				.add("refresh_token", refreshToken)
				.add("client_id", Objects.requireNonNull(parameters.getOauthClientId()));
		ofNullable(parameters.getOauthClientSecret()).ifPresent(s -> formBuilder.add("client_secret", s));

		RequestBody formBody = formBuilder.build();
		Request tokenRequest = new Request.Builder().url(Objects.requireNonNull(parameters.getOauthTokenUri())).post(formBody).build();

		return executeTokenRequest(tokenRequest);
	}

	/**
	 * Obtains a new access token using the password grant flow.
	 */
	private void obtainAccessToken() {
		lastAttemptTime = System.currentTimeMillis();
		FormBody.Builder formBuilder = new FormBody.Builder().add("grant_type", "password")
				.add("username", Objects.requireNonNull(parameters.getOauthUsername()))
				.add("password", Objects.requireNonNull(parameters.getOauthPassword()))
				.add("client_id", Objects.requireNonNull(parameters.getOauthClientId()));
		ofNullable(parameters.getOauthClientSecret()).ifPresent(s -> formBuilder.add("client_secret", s));
		ofNullable(parameters.getOauthScope()).ifPresent(s -> formBuilder.add("scope", s));

		RequestBody formBody = formBuilder.build();
		Request tokenRequest = new Request.Builder().url(Objects.requireNonNull(parameters.getOauthTokenUri())).post(formBody).build();

		executeTokenRequest(tokenRequest);
	}

	/**
	 * Invalidates the current access token, forcing a refresh or re-authentication on the next request.
	 */
	private void invalidateToken() {
		try (LockCloseable ignored = tokenLock.lock()) {
			accessToken = null;
			tokenExpiryTime = 0;
			// Reset last attempt time to allow immediate refresh after 403
			lastAttemptTime = 0;
		}
	}

	/**
	 * Ensures that a valid access token is available. If the token is expired or not yet obtained,
	 * this method will obtain a new one or refresh the existing one.
	 */
	private void ensureValidToken() {
		// Check if token is valid without acquiring lock for better performance
		if (isTokenValid()) {
			return;
		}

		// Skip token request if we already attempted within the same second
		// This prevents slowing down tests when OAuth is failing
		long currentTime = System.currentTimeMillis();
		if (lastAttemptTime > 0 && TimeUnit.MILLISECONDS.toSeconds(currentTime) == TimeUnit.MILLISECONDS.toSeconds(lastAttemptTime)) {
			return;
		}

		try (LockCloseable ignored = tokenLock.lock()) {
			// Double-check after acquiring lock
			if (isTokenValid()) {
				return;
			}

			// Double-check last attempt time after acquiring lock
			if (lastAttemptTime > 0 && TimeUnit.MILLISECONDS.toSeconds(currentTime) == TimeUnit.MILLISECONDS.toSeconds(lastAttemptTime)) {
				return;
			}

			if (refreshToken != null) {
				boolean refreshed = refreshAccessToken();
				if (refreshed) {
					return;
				}
			}
			obtainAccessToken();
		}
	}

	@Override
	@Nonnull
	public Response intercept(@Nonnull Chain chain) throws IOException {
		ensureValidToken();

		if (accessToken == null) {
			return chain.proceed(chain.request());
		}

		Request request = chain.request().newBuilder().header("Authorization", "Bearer " + accessToken).build();
		Response response = chain.proceed(request);

		if (response.code() != 403 && response.code() != 401) {
			return response;
		}

		// Skip token request if we already caught 403 within the same second
		// This prevents slowing down tests when OAuth is failing
		long currentTime = System.currentTimeMillis();
		if (lastAuthFailureTime > 0
				&& TimeUnit.MILLISECONDS.toSeconds(currentTime) == TimeUnit.MILLISECONDS.toSeconds(lastAuthFailureTime)) {
			return response;
		}
		lastAuthFailureTime = currentTime;

		// If we get a 401/403, the token might be invalid. Try to refresh and retry once.
		LOGGER.debug("Received {} response, attempting to refresh token and retry request", response.code());

		// Invalidate the current token
		invalidateToken();

		// Try to get a new token
		ensureValidToken();

		// If we still don't have a token, return the original response without closing it
		if (accessToken == null) {
			return response;
		}

		// Close the previous response
		response.close();

		// Retry the request with the new token
		Request retryRequest = chain.request().newBuilder().header("Authorization", "Bearer " + accessToken).build();
		response = chain.proceed(retryRequest);

		if (response.code() == 403 || response.code() == 401) {
			LOGGER.warn("Still received {} after token refresh, request may be forbidden", response.code());
		}
		return response;
	}
}
