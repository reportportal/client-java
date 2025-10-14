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

import com.epam.reportportal.listeners.ListenerParameters;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OAuth2PasswordGrantAuthInterceptor}
 */
public class OAuth2PasswordGrantAuthInterceptorTest {

	private static final String TOKEN_URL = "http://localhost/oauth/token";
	private static final String API_URL = "http://localhost/api/test";

	private ListenerParameters parameters;
	private final List<Request> capturedRequests = new ArrayList<>();

	@BeforeEach
	public void setup() {
		parameters = new ListenerParameters();
		parameters.setOauthTokenUri(TOKEN_URL);
		parameters.setOauthUsername("test-user");
		parameters.setOauthPassword("test-password");
		parameters.setOauthClientId("test-client-id");
		parameters.setOauthClientSecret("test-client-secret");
		parameters.setOauthScope("test-scope");

		capturedRequests.clear();
	}

	private String createTokenResponse(String accessToken, String refreshToken, int expiresIn) {
		StringBuilder json = new StringBuilder("{");
		json.append("\"access_token\":\"").append(accessToken).append("\",");
		if (refreshToken != null) {
			json.append("\"refresh_token\":\"").append(refreshToken).append("\",");
		}
		json.append("\"token_type\":\"Bearer\",");
		json.append("\"expires_in\":").append(expiresIn);
		json.append("}");
		return json.toString();
	}

	private OkHttpClient createMockTokenClient(String responseBody) {
		return new OkHttpClient.Builder().addInterceptor(chain -> {
			Request request = chain.request();
			capturedRequests.add(request);

			return new Response.Builder().request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(ResponseBody.create(responseBody, MediaType.parse("application/json")))
					.build();
		}).build();
	}

	private OkHttpClient createMockTokenClientWithRefreshToken(int accessTokenExpiresIn) {
		return new OkHttpClient.Builder().addInterceptor(chain -> {
			Request request = chain.request();
			capturedRequests.add(request);

			// First request returns initial token with short expiry
			// Second request (refresh) returns refreshed token
			String responseBody;
			if (capturedRequests.size() == 1) {
				responseBody = createTokenResponse("initial-token", "refresh-token-value", accessTokenExpiresIn);
			} else {
				responseBody = createTokenResponse("refreshed-token", "new-refresh-token", 3600);
			}

			return new Response.Builder().request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(ResponseBody.create(responseBody, MediaType.parse("application/json")))
					.build();
		}).build();
	}

	private Response createMockApiResponse(Request request) {
		return new Response.Builder().request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(200)
				.message("OK")
				.body(ResponseBody.create("{\"status\":\"ok\"}", MediaType.parse("application/json")))
				.build();
	}

	private OkHttpClient createMockApiClient(Interceptor interceptor) {
		return new OkHttpClient.Builder().addInterceptor(interceptor)
				.addInterceptor(chain -> createMockApiResponse(chain.request()))
				.build();
	}

	private Response createAuthFailureMockApiResponse(Request request, int statusCode) {
		return new Response.Builder().request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(statusCode)
				.message(statusCode == 401 ? "Unauthorized" : "Forbidden")
				.body(ResponseBody.create("{\"error\":\"invalid_token\"}", MediaType.parse("application/json")))
				.build();
	}

	@Test
	public void testHappyPathFreshStart() throws Exception {
		// Setup: Create mock token client that returns successful response
		OkHttpClient tokenClient = createMockTokenClient(createTokenResponse("test-access-token", "test-refresh-token", 3600));

		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client with OAuth interceptor
		OkHttpClient apiClient = createMockApiClient(interceptor);

		// Execute API request
		Request apiRequest = new Request.Builder().url(API_URL).get().build();

		try (Response response = apiClient.newCall(apiRequest).execute()) {
			// Verify API response is successful
			assertTrue(response.isSuccessful());
			assertEquals(200, response.code());

			// Verify token request was made
			assertEquals(1, capturedRequests.size(), "Should have made exactly 1 token request");

			Request tokenRequest = capturedRequests.get(0);
			assertEquals(TOKEN_URL, tokenRequest.url().toString());
			assertEquals("POST", tokenRequest.method());

			// Verify token request body contains correct parameters
			RequestBody body = tokenRequest.body();
			assertNotNull(body);
			okio.Buffer buffer = new okio.Buffer();
			body.writeTo(buffer);
			String tokenBody = buffer.readUtf8();

			assertThat(tokenBody, containsString("grant_type=password"));
			assertThat(tokenBody, containsString("username=test-user"));
			assertThat(tokenBody, containsString("password=test-password"));
			assertThat(tokenBody, containsString("client_id=test-client-id"));
			assertThat(tokenBody, containsString("client_secret=test-client-secret"));
			assertThat(tokenBody, containsString("scope=test-scope"));
		}
	}

	@Test
	public void testTokenRefreshHappyPath() throws Exception {
		// Setup: Create mock token client that returns different responses
		OkHttpClient tokenClient = createMockTokenClientWithRefreshToken(1);

		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client
		OkHttpClient apiClient = createMockApiClient(interceptor);

		// Execute first API request (will get initial token)
		Request firstApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(firstApiRequest).execute()) {
			assertTrue(response.isSuccessful());
		}

		// Verify initial token request (password grant)
		assertEquals(1, capturedRequests.size());
		Request initialTokenRequest = capturedRequests.get(0);
		RequestBody initialBody = initialTokenRequest.body();
		assertNotNull(initialBody);
		okio.Buffer buffer = new okio.Buffer();
		initialBody.writeTo(buffer);
		String initialTokenBody = buffer.readUtf8();
		assertThat(initialTokenBody, containsString("grant_type=password"));

		// Wait 1 second to ensure we're in a different second for throttling
		// and token is considered expired (1 sec expiry + 1 minute buffer)
		Thread.sleep(1000);

		// Execute second API request (should trigger token refresh)
		Request secondApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(secondApiRequest).execute()) {
			assertTrue(response.isSuccessful());
		}

		// Verify ONLY refresh token request was made (not password grant again)
		assertEquals(2, capturedRequests.size(), "Should have made exactly 2 token requests (password + refresh)");

		Request refreshTokenRequest = capturedRequests.get(1);
		RequestBody refreshBody = refreshTokenRequest.body();
		assertNotNull(refreshBody);
		okio.Buffer refreshBuffer = new okio.Buffer();
		refreshBody.writeTo(refreshBuffer);
		String refreshTokenBody = refreshBuffer.readUtf8();

		// This MUST be a refresh token request, NOT a password grant
		assertThat(refreshTokenBody, containsString("grant_type=refresh_token"));
		assertThat(refreshTokenBody, containsString("refresh_token=refresh-token-value"));
		assertThat(refreshTokenBody, containsString("client_id=test-client-id"));
		assertThat(refreshTokenBody, not(containsString("username")));
		assertThat(refreshTokenBody, not(containsString("password")));
	}

	@Test
	public void testTokenRequestThrottling() throws Exception {
		// Setup: Create mock token client that returns error, then success
		OkHttpClient tokenClient = new OkHttpClient.Builder().addInterceptor(chain -> {
			Request request = chain.request();
			capturedRequests.add(request);

			// First request fails, subsequent requests succeed
			if (capturedRequests.size() == 1) {
				return new Response.Builder().request(request)
						.protocol(Protocol.HTTP_1_1)
						.code(401)
						.message("Unauthorized")
						.body(ResponseBody.create("{\"error\":\"invalid_grant\"}", MediaType.parse("application/json")))
						.build();
			} else {
				String responseBody = createTokenResponse("success-token", "success-refresh-token", 3600);
				return new Response.Builder().request(request)
						.protocol(Protocol.HTTP_1_1)
						.code(200)
						.message("OK")
						.body(ResponseBody.create(responseBody, MediaType.parse("application/json")))
						.build();
			}
		}).build();

		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client
		OkHttpClient apiClient = createMockApiClient(interceptor);

		// Execute first API request (will attempt to get token and fail)
		Request firstApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(firstApiRequest).execute()) {
			assertTrue(response.isSuccessful()); // API call succeeds even without token
		}

		// Verify token request was made
		assertEquals(1, capturedRequests.size(), "First token request should have been made");

		// Execute second API request immediately (within same second)
		Request secondApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(secondApiRequest).execute()) {
			assertTrue(response.isSuccessful());
		}

		// Verify NO additional token request was made due to throttling
		assertEquals(1, capturedRequests.size(), "Second token request should have been throttled");

		// Wait for throttling period to expire (1 second)
		Thread.sleep(1100);

		// Execute third API request (should trigger new token request after throttling period)
		Request thirdApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(thirdApiRequest).execute()) {
			assertTrue(response.isSuccessful());
		}

		// Verify token request was made after throttling period
		assertEquals(2, capturedRequests.size(), "Token request should have been made after throttling period");
	}

	@Test
	public void testWithoutClientSecret() throws Exception {
		// Setup parameters without client secret
		parameters.setOauthClientSecret(null);

		// Setup: Create mock token client
		OkHttpClient tokenClient = createMockTokenClient(createTokenResponse("test-access-token", "test-refresh-token", 3600));

		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client
		OkHttpClient apiClient = createMockApiClient(interceptor);

		// Execute API request
		Request apiRequest = new Request.Builder().url(API_URL).get().build();

		try (Response response = apiClient.newCall(apiRequest).execute()) {
			assertTrue(response.isSuccessful());

			// Verify token request body does NOT contain client_secret
			assertEquals(1, capturedRequests.size());
			Request tokenRequest = capturedRequests.get(0);
			RequestBody body = tokenRequest.body();
			assertNotNull(body);
			okio.Buffer buffer = new okio.Buffer();
			body.writeTo(buffer);
			String tokenBody = buffer.readUtf8();

			assertThat(tokenBody, not(containsString("client_secret")));
		}
	}

	@Test
	public void testWithoutScope() throws Exception {
		// Setup parameters without scope
		parameters.setOauthScope(null);

		// Setup: Create mock token client
		OkHttpClient tokenClient = createMockTokenClient(createTokenResponse("test-access-token", "test-refresh-token", 3600));

		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client
		OkHttpClient apiClient = createMockApiClient(interceptor);

		// Execute API request
		Request apiRequest = new Request.Builder().url(API_URL).get().build();

		try (Response response = apiClient.newCall(apiRequest).execute()) {
			assertTrue(response.isSuccessful());

			// Verify token request body does NOT contain scope
			assertEquals(1, capturedRequests.size());
			Request tokenRequest = capturedRequests.get(0);
			RequestBody body = tokenRequest.body();
			assertNotNull(body);
			okio.Buffer buffer = new okio.Buffer();
			body.writeTo(buffer);
			String tokenBody = buffer.readUtf8();

			assertThat(tokenBody, not(containsString("scope")));
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 401, 403 })
	public void testAuthFailureResponseTriggersTokenRefresh(int statusCode) throws Exception {
		// Track API request count
		final List<Integer> apiRequestCount = new ArrayList<>();

		// Setup: Create mock token client that returns different tokens
		OkHttpClient tokenClient = createMockTokenClientWithRefreshToken(3600);
		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client that returns auth failure on first attempt with initial token, then 200
		OkHttpClient apiClient = new OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(chain -> {
			Request request = chain.request();
			apiRequestCount.add(1);

			String authHeader = request.header("Authorization");

			// First API request with "initial-token" returns auth failure
			if (authHeader != null && authHeader.contains("initial-token")) {
				return createAuthFailureMockApiResponse(request, statusCode);
			}

			// Subsequent requests with refreshed token return 200
			return createMockApiResponse(request);
		}).build();

		// Execute API request
		Request apiRequest = new Request.Builder().url(API_URL).get().build();

		try (Response response = apiClient.newCall(apiRequest).execute()) {
			// Verify final response is successful after retry
			assertTrue(response.isSuccessful());
			assertEquals(200, response.code());

			// Verify we made 2 token requests (initial + refresh after auth failure)
			assertEquals(2, capturedRequests.size(), "Should have made 2 token requests (initial + refresh after " + statusCode + ")");

			// Verify first token request was password grant
			Request firstTokenRequest = capturedRequests.get(0);
			RequestBody firstBody = firstTokenRequest.body();
			assertNotNull(firstBody);
			okio.Buffer firstBuffer = new okio.Buffer();
			firstBody.writeTo(firstBuffer);
			String firstTokenBody = firstBuffer.readUtf8();
			assertThat(firstTokenBody, containsString("grant_type=password"));

			// Verify second token request was refresh grant
			Request secondTokenRequest = capturedRequests.get(1);
			RequestBody secondBody = secondTokenRequest.body();
			assertNotNull(secondBody);
			okio.Buffer secondBuffer = new okio.Buffer();
			secondBody.writeTo(secondBuffer);
			String secondTokenBody = secondBuffer.readUtf8();
			assertThat(secondTokenBody, containsString("grant_type=refresh_token"));
			assertThat(secondTokenBody, containsString("refresh_token=refresh-token-value"));

			// Verify API was called twice (initial auth failure + retry with refreshed token)
			assertEquals(2, apiRequestCount.size(), "API should have been called twice");
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 401, 403 })
	public void testAuthFailureResponseThrottling(int statusCode) throws Exception {
		// Track API request count
		final List<Integer> apiRequestCount = new ArrayList<>();

		// Setup: Create mock token client that always returns same token
		OkHttpClient tokenClient = createMockTokenClient(createTokenResponse("test-token", "test-refresh-token", 3600));
		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client that always returns auth failure
		OkHttpClient apiClient = new OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(chain -> {
			Request request = chain.request();
			apiRequestCount.add(1);

			// Always return auth failure
			return createAuthFailureMockApiResponse(request, statusCode);
		}).build();

		// Execute first API request (will get auth failure, trigger refresh attempt)
		Request firstApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(firstApiRequest).execute()) {
			assertEquals(statusCode, response.code());
		}

		// Verify initial token request + refresh attempt were made
		assertEquals(2, capturedRequests.size(), "Initial token request + refresh after " + statusCode + " should have been made");
		assertEquals(2, apiRequestCount.size(), "API should have been called twice (initial + retry after refresh)");

		// Execute second API request immediately (within same second, should get auth failure but NO refresh)
		Request secondApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(secondApiRequest).execute()) {
			assertEquals(statusCode, response.code());
		}

		// Verify NO additional token requests were made due to throttling
		assertEquals(2, capturedRequests.size(), "Token refresh should have been throttled on second " + statusCode);
		assertEquals(3, apiRequestCount.size(), "API should have been called once more (no retry due to throttling)");

		// Execute third API request immediately (within same second, should still be throttled)
		Request thirdApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(thirdApiRequest).execute()) {
			assertEquals(statusCode, response.code());
		}

		// Verify still NO additional token requests were made
		assertEquals(2, capturedRequests.size(), "Token refresh should still be throttled on third " + statusCode);
		assertEquals(4, apiRequestCount.size(), "API should have been called once more (no retry due to throttling)");

		// Wait for throttling period to expire (1+ second)
		Thread.sleep(1100);

		// Execute fourth API request (should trigger new refresh attempt after throttling period)
		Request fourthApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(fourthApiRequest).execute()) {
			assertEquals(statusCode, response.code());
		}

		// Verify token refresh was attempted after throttling period
		assertEquals(3, capturedRequests.size(), "Token refresh should have been attempted after throttling period");
		assertEquals(6, apiRequestCount.size(), "API should have been called twice (initial + retry after refresh)");

		// Verify third token request was refresh grant
		Request thirdTokenRequest = capturedRequests.get(2);
		RequestBody thirdBody = thirdTokenRequest.body();
		assertNotNull(thirdBody);
		okio.Buffer thirdBuffer = new okio.Buffer();
		thirdBody.writeTo(thirdBuffer);
		String thirdTokenBody = thirdBuffer.readUtf8();
		assertThat(thirdTokenBody, containsString("grant_type=refresh_token"));
		assertThat(thirdTokenBody, containsString("refresh_token=test-refresh-token"));
	}

	@Test
	public void test401And403ThrottledSimultaneously() throws Exception {
		// Track API request count
		final List<Integer> apiRequestCount = new ArrayList<>();

		// Setup: Create mock token client that always returns same token
		OkHttpClient tokenClient = createMockTokenClient(createTokenResponse("test-token", "test-refresh-token", 3600));
		OAuth2PasswordGrantAuthInterceptor interceptor = new OAuth2PasswordGrantAuthInterceptor(tokenClient, parameters);

		// Create API client that alternates between 401 and 403
		OkHttpClient apiClient = new OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(chain -> {
			Request request = chain.request();
			apiRequestCount.add(1);

			// Alternate between 401 and 403
			int statusCode = (apiRequestCount.size() % 2 == 0) ? 403 : 401;
			return createAuthFailureMockApiResponse(request, statusCode);
		}).build();

		// Execute first API request (will get 401, trigger refresh attempt, retry gets 403)
		Request firstApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(firstApiRequest).execute()) {
			assertEquals(403, response.code()); // Final response after retry
		}

		// Verify initial token request + refresh attempt were made
		assertEquals(2, capturedRequests.size(), "Initial token request + refresh after 401 should have been made");
		assertEquals(2, apiRequestCount.size(), "API should have been called twice (initial 401 + retry 403)");

		// Execute second API request immediately (within same second, should get 401 but NO refresh)
		Request secondApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(secondApiRequest).execute()) {
			assertEquals(401, response.code());
		}

		// Verify NO additional token requests were made due to throttling
		assertEquals(2, capturedRequests.size(), "Token refresh should have been throttled on second 401");
		assertEquals(3, apiRequestCount.size(), "API should have been called once more (no retry due to throttling)");

		// Execute third API request immediately (within same second, should get 403 but NO refresh)
		Request thirdApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(thirdApiRequest).execute()) {
			assertEquals(403, response.code());
		}

		// Verify still NO additional token requests were made (401 and 403 throttled together)
		assertEquals(2, capturedRequests.size(), "Token refresh should still be throttled on 403");
		assertEquals(4, apiRequestCount.size(), "API should have been called once more (no retry due to throttling)");

		// Wait for throttling period to expire (1+ second)
		Thread.sleep(1100);

		// Execute fourth API request (should trigger new refresh attempt after throttling period)
		Request fourthApiRequest = new Request.Builder().url(API_URL).get().build();
		try (Response response = apiClient.newCall(fourthApiRequest).execute()) {
			assertEquals(403, response.code()); // Final response after retry
		}

		// Verify token refresh was attempted after throttling period
		assertEquals(3, capturedRequests.size(), "Token refresh should have been attempted after throttling period");
		assertEquals(6, apiRequestCount.size(), "API should have been called twice (initial + retry after refresh)");
	}
}
