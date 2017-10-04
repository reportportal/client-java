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
package com.epam.reportportal.service;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.avarabyeu.restendpoint.http.HttpClientRestEndpoint;
import com.github.avarabyeu.restendpoint.http.RestEndpoint;
import com.github.avarabyeu.restendpoint.http.RestEndpoints;
import com.github.avarabyeu.restendpoint.serializer.ByteArraySerializer;
import com.github.avarabyeu.restendpoint.serializer.json.JacksonSerializer;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import io.reactivex.Maybe;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link com.github.avarabyeu.restendpoint.http.RestEndpoint} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ReportPortalClient rpClient;
    private ListenerParameters parameters;

    /**
     * @param rpClient   ReportPortal client
     * @param parameters Listener Parameters
     */
    ReportPortal(ReportPortalClient rpClient, ListenerParameters parameters) {
        this.rpClient = rpClient;
        this.parameters = parameters;
    }

    /**
     * Starts launch in ReportPortal
     *
     * @param rq Request Data
     * @return Launch ID promise
     */
    public Launch startLaunch(StartLaunchRQ rq) {
        if (!parameters.getEnable()) {
            return Launch.NOOP_LAUNCH;
        }

        LaunchImpl service = new LaunchImpl(rpClient, parameters);
        service.startLaunch(rq);
        return service;
    }

    /**
     * Factory method for {@link ReportPortal} that uses already started launch
     *
     * @param currentLaunchId Launch to be used
     * @return
     */
    public Launch withLaunch(
            Maybe<String> currentLaunchId) {
        LaunchImpl service = new LaunchImpl(rpClient, parameters);
        service.useLaunch(currentLaunchId);
        return service;
    }


    public static class Builder {
        public static final String API_BASE = "/api/v1";
        private static final String HTTPS = "https";


        private CloseableHttpAsyncClient httpClient;
        private ListenerParameters parameters;

        public Builder withHttpClient(CloseableHttpAsyncClient client) {
            this.httpClient = client;
            return this;
        }


        public Builder withParameters(ListenerParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public ReportPortal build() throws MalformedURLException {
            ListenerParameters params = null == this.parameters ? new ListenerParameters(defaultPropertiesLoader()) : this.parameters;
            CloseableHttpAsyncClient client = null == this.httpClient ? defaultClient(params) : this.httpClient;

            ReportPortalClient restEndpoint = RestEndpoints.forInterface(ReportPortalClient.class, buildRestEndpoint(params, client));
            return new ReportPortal(restEndpoint, params);

        }

        protected RestEndpoint buildRestEndpoint(ListenerParameters parameters, CloseableHttpAsyncClient client) {
            final ObjectMapper om = new ObjectMapper();
            om.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));

            String baseUrl = parameters.getBaseUrl();
            String project = parameters.getProjectName();

            JacksonSerializer jacksonSerializer = new JacksonSerializer(om);
            return new HttpClientRestEndpoint(client, Lists.newArrayList(jacksonSerializer, new ByteArraySerializer()), new ReportPortalErrorHandler(jacksonSerializer), buildEndpointUrl(baseUrl, project));
        }

        protected String buildEndpointUrl(String baseUrl, String project) {
            return baseUrl + API_BASE + "/" + project;
        }


        protected CloseableHttpAsyncClient defaultClient(ListenerParameters parameters) throws MalformedURLException {
            String baseUrl = parameters.getBaseUrl();
            String keyStore = parameters.getKeystore();
            String keyStorePassword = parameters.getKeystorePassword();
            final String uuid = parameters.getUuid();

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

        protected PropertiesLoader defaultPropertiesLoader() {
            return PropertiesLoader.load();
        }
    }

}
