/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.guice;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.ReportPortalErrorHandler;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.avarabyeu.restendpoint.http.ErrorHandler;
import com.github.avarabyeu.restendpoint.http.HttpClientRestEndpoint;
import com.github.avarabyeu.restendpoint.http.RestEndpoint;
import com.github.avarabyeu.restendpoint.http.RestEndpoints;
import com.github.avarabyeu.restendpoint.serializer.ByteArraySerializer;
import com.github.avarabyeu.restendpoint.serializer.Serializer;
import com.github.avarabyeu.restendpoint.serializer.json.JacksonSerializer;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Report portal service endpoint and utils module
 */

public class ReportPortalClientModule implements Module {

    public static final String API_BASE = "/api/v1";
    private static final String HTTPS = "https";

    @Override
    public void configure(Binder binder) {

    }

    /**
     * Default {@link ReportPortalErrorHandler
     * ReportPortalErrorHandler(Serializer)} binding
     *
     * @param serializer Default serializer for error handler
     */
    @Provides
    @Singleton
    public ErrorHandler provideErrorHandler(Serializer serializer) {
        return new ReportPortalErrorHandler(serializer);
    }

    /**
     * Default {@link Serializer} binding
     */
    @Provides
    @Singleton
    public Serializer provideSerializer() {
        return new JacksonSerializer(new ObjectMapper());
    }

    @Provides
    @Singleton
    @Named("serializers")
    public List<Serializer> provideSerializers(Serializer defaultSerializer) {
        return Lists.newArrayList(defaultSerializer, new ByteArraySerializer());
    }

    /**
     * Default {@link com.github.avarabyeu.restendpoint.http.RestEndpoint} binding
     *
     * @param serializers  Set of serializers to marshal request/response body
     * @param errorHandler Handler for 4xx/5xx HTTP responses
     * @param baseUrl      Base url of application
     */
    @Provides
    public RestEndpoint provideRestEndpoint(CloseableHttpAsyncClient httpClient,
            @Named("serializers") List<Serializer> serializers,
            @ListenerPropertyValue(ListenerProperty.BASE_URL) String baseUrl,
            @ListenerPropertyValue(ListenerProperty.PROJECT_NAME) String project,
            ErrorHandler errorHandler) {
        return new HttpClientRestEndpoint(httpClient, serializers, errorHandler, baseUrl + API_BASE + "/" + project);
    }

    /**
     * Default {@link HttpAsyncClient} binding
     */
    @Provides
    public CloseableHttpAsyncClient provideHttpClient(
            @ListenerPropertyValue(ListenerProperty.BASE_URL) String baseUrl,
            @ListenerPropertyValue(ListenerProperty.UUID) final String uuid,
            @Nullable @ListenerPropertyValue(ListenerProperty.KEYSTORE_RESOURCE) String keyStore,
            @Nullable @ListenerPropertyValue(ListenerProperty.KEYSTORE_PASSWORD) String keyStorePassword)
            throws MalformedURLException {

        final HttpAsyncClientBuilder builder = HttpAsyncClients.custom();
        if (HTTPS.equals(new URL(baseUrl).getProtocol()) && keyStore != null) {
            if (null == keyStorePassword) {
                throw new InternalReportPortalClientException(
                        "You should provide keystore password parameter [" + ListenerProperty.KEYSTORE_PASSWORD
                                + "] if you use HTTPS protocol");
            }

            try {
                builder.setSSLContext(
                        SSLContextBuilder.create().loadTrustMaterial(SslUtils.loadKeyStore(keyStore, keyStorePassword),
                                TrustSelfSignedStrategy.INSTANCE).build());
            } catch (Exception e) {
                throw new InternalReportPortalClientException(
                        "Unable to load trust store");
            }

        }
        return builder.addInterceptorLast(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                request.setHeader(HttpHeaders.AUTHORIZATION, "bearer " + uuid);
            }
        }).build();
    }

    /**
     * Binds the same instance for {@link ReportPortal} interface.
     * Guice cannot bind one implementation to two interfaces automatically
     *
     * @param restEndpoint Instance for binding
     * @return {@link ReportPortal} instance
     */
    @Provides
    @Singleton
    public ReportPortalClient reportPortalClient(RestEndpoint restEndpoint) {
        return RestEndpoints.forInterface(ReportPortalClient.class, restEndpoint);
    }

    /**
     * provides message parser
     */
    @Provides
    @Singleton
    public MessageParser provideMessageParser() {
        return new HashMarkSeparatedMessageParser();
    }

}
