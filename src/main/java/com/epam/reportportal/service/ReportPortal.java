/*
 * Copyright (C) 2018 EPAM Systems
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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.restendpoint.http.HttpClientRestEndpoint;
import com.epam.reportportal.restendpoint.http.RestEndpoint;
import com.epam.reportportal.restendpoint.http.RestEndpoints;
import com.epam.reportportal.restendpoint.serializer.ByteArraySerializer;
import com.epam.reportportal.restendpoint.serializer.Serializer;
import com.epam.reportportal.restendpoint.serializer.json.JacksonSerializer;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Maybe;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		if (Boolean.TRUE != parameters.getEnable()) {
			return Launch.NOOP_LAUNCH;
		}

		return new LaunchImpl(rpClient, parameters, rq);
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
	 * @return ReportPortal client
	 */
	public ReportPortalClient getClient() {
		return this.rpClient;
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
	 * Creates new ReportPortal based on already built dependencies
	 *
	 * @return builder for {@link ReportPortal}
	 */
	public static ReportPortal create(ReportPortalClient client, ListenerParameters params) {
		return new ReportPortal(client, params);
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

	public static boolean emitLaunchLog(Function<String, SaveLogRQ> logSupplier) {
		final LaunchLoggingContext launchLoggingContext = LaunchLoggingContext.CONTEXT_THREAD_LOCAL.get();
		if (null != launchLoggingContext) {
			launchLoggingContext.emit(logSupplier);
			return true;
		}
		return false;
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 */
	public static boolean emitLog(final String message, final String level, final Date time) {
		return emitLog(getLogSupplier(message, level, time));

	}

	public static boolean emitLaunchLog(final String message, String level, Date time) {
		return emitLaunchLog(getLogSupplier(message, level, time));
	}

	private static Function<String, SaveLogRQ> getLogSupplier(final String message, final String level, final Date time) {
		return new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLevel(level);
				rq.setLogTime(time);
				rq.setItemId(itemId);
				rq.setMessage(message);
				return rq;
			}
		};
	}

	public static boolean emitLog(final String message, final String level, final Date time, final File file) {
		return emitLog(getLogSupplier(message, level, time, file));
	}

	public static boolean emitLaunchLog(final String message, final String level, final Date time, final File file) {
		return emitLaunchLog(getLogSupplier(message, level, time, file));
	}

	private static Function<String, SaveLogRQ> getLogSupplier(final String message, final String level, final Date time, final File file) {
		return new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLevel(level);
				rq.setLogTime(time);
				rq.setItemId(itemId);
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
		};
	}

	public static boolean emitLog(final ReportPortalMessage message, final String level, final Date time) {
		return emitLog(getLogSupplier(message, level, time));
	}

	public static boolean emitLaunchLog(final ReportPortalMessage message, final String level, final Date time) {
		return emitLaunchLog(getLogSupplier(message, level, time));
	}

	private static Function<String, SaveLogRQ> getLogSupplier(final ReportPortalMessage message, final String level, final Date time) {
		return new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLevel(level);
				rq.setLogTime(time);
				rq.setItemId(itemId);
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
		};
	}

	public static class Builder {
		public static final String API_BASE = "/api/v1";
		private static final String HTTPS = "https";

		private HttpClientBuilder httpClient;
		private ListenerParameters parameters;
		private ExecutorService executorService;

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
				executorService = Executors.newFixedThreadPool(params.getIoPoolSize(),
						new ThreadFactoryBuilder().setNameFormat("rp-io-%s").build()
				);
				return new ReportPortal(buildClient(ReportPortalClient.class, params), params);
			} catch (Exception e) {
				String errMsg = "Cannot build ReportPortal client";
				LOGGER.error(errMsg, e);
				throw new InternalReportPortalClientException(errMsg, e);
			}

		}

		public <T extends ReportPortalClient> T buildClient(Class<T> clientType, ListenerParameters params) {
			try {
				HttpClient client = null == this.httpClient ?
						defaultClient(params) :
						this.httpClient.addInterceptorLast(new BearerAuthInterceptor(params.getUuid())).build();

				return RestEndpoints.forInterface(clientType, buildRestEndpoint(params, client));
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

			final JacksonSerializer jacksonSerializer = new JacksonSerializer(om);
			return new HttpClientRestEndpoint(client, new LinkedList<Serializer>() {{
				add(jacksonSerializer);
				add(new ByteArraySerializer());
			}}, new ReportPortalErrorHandler(jacksonSerializer), buildEndpointUrl(baseUrl, project), executorService);
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

			builder.disableAutomaticRetries()
					.setMaxConnPerRoute(parameters.getMaxConnectionsPerRoute())
					.setMaxConnTotal(parameters.getMaxConnectionsTotal())
					.evictExpiredConnections();
			return builder.addInterceptorLast(new BearerAuthInterceptor(uuid)).build();

		}

		protected PropertiesLoader defaultPropertiesLoader() {
			return PropertiesLoader.load();
		}
	}

}
