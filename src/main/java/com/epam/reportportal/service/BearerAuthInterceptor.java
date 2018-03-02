package com.epam.reportportal.service;

import com.google.common.net.HttpHeaders;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Adds Bearer TOKEN to the request headers
 */
public class BearerAuthInterceptor implements HttpRequestInterceptor {

	private final String uuid;

	public BearerAuthInterceptor(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		request.setHeader(HttpHeaders.AUTHORIZATION, "bearer " + uuid);

	}
}
