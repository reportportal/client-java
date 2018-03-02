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
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.restendpoint.http.HttpClientRestEndpoint;
import com.epam.reportportal.restendpoint.http.RestEndpoint;
import com.epam.reportportal.restendpoint.http.RestEndpoints;
import com.epam.reportportal.restendpoint.serializer.ByteArraySerializer;
import com.epam.reportportal.restendpoint.serializer.json.JacksonSerializer;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.reactivex.Maybe;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.google.common.io.Files.toByteArray;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link RestEndpoint} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);
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
	 * @return Launch
	 */
	public Launch newLaunch(StartLaunchRQ rq) {
		if (!parameters.getEnable()) {
			return Launch.NOOP_LAUNCH;
		}

		LaunchImpl service = new LaunchImpl(rpClient, parameters, rq);
		return service;
	}

	/**
	 * Factory method for {@link ReportPortal} that uses already started launch
	 *
	 * @param currentLaunchId Launch to be used
	 * @return This instance for chaining
	 */
	public Launch withLaunch(Maybe<String> currentLaunchId) {
		return new LaunchImpl(rpClient, parameters, currentLaunchId);
	}

	/**
	 * @return Configuration parameters
	 */
	public ListenerParameters getParameters() {
		return parameters;
	}

	/**
	 * Creates new builder for {@link ReportPortal}
	 *
	 * @return builder for {@link ReportPortal}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 *
	 * @param logSupplier Log supplier. Converts current Item ID to the {@link SaveLogRQ} object
	 */
	public static boolean emitLog(com.google.common.base.Function<String, SaveLogRQ> logSupplier) {
		final LoggingContext loggingContext = LoggingContext.CONTEXT_THREAD_LOCAL.get();
		if (null != loggingContext) {
			loggingContext.emit(logSupplier);
			return true;
		}
		return false;
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 */
	public static boolean emitLog(final String message, final String level, final Date time) {
		return emitLog(new com.google.common.base.Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(@Nullable String id) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLevel(level);
				rq.setLogTime(time);
				rq.setTestItemId(id);
				rq.setMessage(message);
				return rq;
			}
		});

	}

	public static boolean emitLog(final String message, final String level, final Date time, final File file) {
		return emitLog(new com.google.common.base.Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(@Nullable String id) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLevel(level);
				rq.setLogTime(time);
				rq.setTestItemId(id);
				rq.setMessage(message);

				try {
					SaveLogRQ.File f = new SaveLogRQ.File();
					f.setContentType(detect(file));
					f.setContent(toByteArray(file));

					f.setName(UUID.randomUUID().toString());
					rq.setFile(f);
				} catch (IOException e) {
					// seems like there is some problem. Do not report an file
					LOGGER.error("Cannot send file to ReportPortal", e);
				}

				return rq;
			}
		});
	}

	public static boolean emitLog(final ReportPortalMessage message, final String level, final Date time) {
		return emitLog(new com.google.common.base.Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(@Nullable String id) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLevel(level);
				rq.setLogTime(time);
				rq.setTestItemId(id);
				rq.setMessage(message.getMessage());
				try {
					final TypeAwareByteSource data = message.getData();
					SaveLogRQ.File file = new SaveLogRQ.File();
					file.setContent(data.read());

					file.setContentType(data.getMediaType());
					file.setName(UUID.randomUUID().toString());
					rq.setFile(file);

				} catch (Exception e) {
					// seems like there is some problem. Do not report an file
					LOGGER.error("Cannot send file to ReportPortal", e);
				}

				return rq;
			}
		});
	}

	public static class Builder {
		public static final String API_BASE = "/api/v1";
		private static final String HTTPS = "https";

		private HttpClientBuilder httpClient;
		private ListenerParameters parameters;

		public Builder withHttpClient(HttpClientBuilder client) {
			this.httpClient = client;
			return this;
		}

		public Builder withParameters(ListenerParameters parameters) {
			this.parameters = parameters;
			return this;
		}

		public ReportPortal build() {
			try {
				ListenerParameters params = null == this.parameters ? new ListenerParameters(defaultPropertiesLoader()) : this.parameters;
				HttpClient client = null == this.httpClient ?
						defaultClient(params) :
						this.httpClient.addInterceptorLast(new BearerAuthInterceptor(params.getUuid())).build();

				ReportPortalClient restEndpoint = RestEndpoints.forInterface(ReportPortalClient.class, buildRestEndpoint(params, client));
				return new ReportPortal(restEndpoint, params);
			} catch (Exception e) {
				String errMsg = "Cannot build ReportPortal client";
				LOGGER.error(errMsg, e);
				throw new InternalReportPortalClientException(errMsg, e);
			}

		}

		protected RestEndpoint buildRestEndpoint(ListenerParameters parameters, HttpClient client) {
			final ObjectMapper om = new ObjectMapper();
			om.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));
			om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			String baseUrl = parameters.getBaseUrl();
			String project = parameters.getProjectName();

			JacksonSerializer jacksonSerializer = new JacksonSerializer(om);
			return new HttpClientRestEndpoint(
					client,
					Lists.newArrayList(jacksonSerializer, new ByteArraySerializer()),
					new ReportPortalErrorHandler(jacksonSerializer),
					buildEndpointUrl(baseUrl, project)
			);
		}

		protected String buildEndpointUrl(String baseUrl, String project) {
			return baseUrl + API_BASE + "/" + project;
		}

		protected HttpClient defaultClient(ListenerParameters parameters) throws MalformedURLException {
			String baseUrl = parameters.getBaseUrl();
			String keyStore = parameters.getKeystore();
			String keyStorePassword = parameters.getKeystorePassword();
			final String uuid = parameters.getUuid();

			final HttpClientBuilder builder = HttpClients.custom();
			if (HTTPS.equals(new URL(baseUrl).getProtocol()) && keyStore != null) {
				if (null == keyStorePassword) {
					throw new InternalReportPortalClientException(
							"You should provide keystore password parameter [" + ListenerProperty.KEYSTORE_PASSWORD
									+ "] if you use HTTPS protocol");
				}

				try {
					builder.setSSLContext(SSLContextBuilder.create()
							.loadTrustMaterial(SslUtils.loadKeyStore(keyStore, keyStorePassword), TrustSelfSignedStrategy.INSTANCE)
							.build());
				} catch (Exception e) {
					throw new InternalReportPortalClientException("Unable to load trust store");
				}

			}
			builder.setMaxConnPerRoute(50).setMaxConnTotal(100);
			return builder.addInterceptorLast(new BearerAuthInterceptor(uuid)).build();

		}

		protected PropertiesLoader defaultPropertiesLoader() {
			return PropertiesLoader.load();
		}
	}

}
