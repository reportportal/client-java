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
import com.epam.reportportal.service.launch.PrimaryLaunch;
import com.epam.reportportal.service.launch.SecondaryLaunch;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Maybe;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.epam.reportportal.service.LaunchLoggingContext.DEFAULT_LAUNCH_KEY;
import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.epam.reportportal.utils.files.Utils.readFileToBytes;
import static java.util.Optional.ofNullable;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link RestEndpoint} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);
	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private final AtomicReference<String> instanceUuid = new AtomicReference<>(UUID.randomUUID().toString());

	private final ListenerParameters parameters;
	private final LockFile lockFile;
	private final ReportPortalClient rpClient;
	private final ExecutorService executor;

	/**
	 * @param rpClient   ReportPortal client
	 * @param parameters Listener Parameters
	 */
	ReportPortal(ReportPortalClient rpClient, ExecutorService executor, ListenerParameters parameters, LockFile lockFile) {
		this.rpClient = rpClient;
		this.executor = executor;
		this.parameters = parameters;
		this.lockFile = lockFile;
	}

	/**
	 * Starts launch in ReportPortal
	 *
	 * @param rq Request Data
	 * @return Launch
	 */
	public Launch newLaunch(StartLaunchRQ rq) {
		if (BooleanUtils.isNotTrue(parameters.getEnable())) {
			return Launch.NOOP_LAUNCH;
		}

		return getLaunch(rq);
	}

	private Launch getLaunch(StartLaunchRQ rq) {
		if (lockFile == null) {
			// do not use multi-client mode
			return new LaunchImpl(rpClient, parameters, rq, executor);
		}

		final String uuid = lockFile.obtainLaunchUuid(instanceUuid.get());
		if (uuid == null) {
			// timeout locking on file or interrupted, anyway it should be logged already
			// we continue to operate normally, since this flag is set by default and we shouldn't fail launches because of it
			return new LaunchImpl(rpClient, parameters, rq, executor);
		}

		if (instanceUuid.get().equals(uuid)) {
			// We got our own UUID as launch UUID, that means we are primary launch.
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				StartLaunchRQ rqCopy = objectMapper.readValue(objectMapper.writeValueAsString(rq), StartLaunchRQ.class);
				rqCopy.setUuid(uuid);
				return new PrimaryLaunch(rpClient, parameters, rqCopy, executor, lockFile, instanceUuid);
			} catch (IOException e) {
				throw new InternalReportPortalClientException("Unable to clone start launch request:", e);
			}
		} else {
			Maybe<String> launch = Maybe.create(emitter -> {
				emitter.onSuccess(uuid);
				emitter.onComplete();
			});
			return new SecondaryLaunch(rpClient, parameters, launch, executor, lockFile, instanceUuid);
		}
	}

	/**
	 * Factory method for {@link ReportPortal} that uses already started launch
	 *
	 * @param launchUuid Launch to be used
	 * @return This instance for chaining
	 */
	public Launch withLaunch(Maybe<String> launchUuid) {
		return new LaunchImpl(rpClient, parameters, launchUuid, executor);
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

	private static LockFile getLockFile(ListenerParameters parameters) {
		if (parameters.getClientJoin()) {
			return new LockFile(parameters);
		}
		return null;
	}

	/**
	 * Creates new ReportPortal based on already built dependencies
	 *
	 * @param client Report Portal Client
	 * @param params {@link ListenerParameters}
	 * @return builder for {@link ReportPortal}
	 */
	public static ReportPortal create(ReportPortalClient client, ListenerParameters params) {
		return create(client, params, buildExecutorService(params));
	}

	/**
	 * Creates new ReportPortal based on already built dependencies
	 *
	 * @param client   Report Portal Client
	 * @param params   {@link ListenerParameters}
	 * @param executor An executor service which will be used for internal request / response queue
	 * @return builder for {@link ReportPortal}
	 */
	public static ReportPortal create(@Nonnull final ReportPortalClient client, @Nonnull final ListenerParameters params,
			@Nonnull final ExecutorService executor) {
		return new ReportPortal(client, executor, params, getLockFile(params));
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 *
	 * @param logSupplier Log supplier. Converts current Item ID to the {@link SaveLogRQ} object
	 * @return true if log has been emitted
	 */
	public static boolean emitLog(final Function<String, SaveLogRQ> logSupplier) {
		final LoggingContext loggingContext = LoggingContext.CONTEXT_THREAD_LOCAL.get().peek();
		if (null != loggingContext) {
			loggingContext.emit(logSupplier);
			return true;
		}
		return false;
	}

	/**
	 * Emits log message on Launch level if there is any active context attached to the current thread
	 *
	 * @param logSupplier Log supplier. Converts current Item ID to the {@link SaveLogRQ} object
	 * @return true if log has been emitted
	 */
	public static boolean emitLaunchLog(final Function<String, SaveLogRQ> logSupplier) {
		final LaunchLoggingContext launchLoggingContext = LaunchLoggingContext.loggingContextMap.get(DEFAULT_LAUNCH_KEY);
		if (null != launchLoggingContext) {
			launchLoggingContext.emit(logSupplier);
			return true;
		}
		return false;
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 *
	 * @param message Log message
	 * @param level   Log level
	 * @param time    Log time
	 * @return true if log has been emitted
	 */
	public static boolean emitLog(final String message, final String level, final Date time) {
		return emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setLevel(level);
			rq.setLogTime(time);
			rq.setItemUuid(itemUuid);
			rq.setMessage(message);
			return rq;
		});

	}

	/**
	 * Emits log message on Launch level if there is any active context attached to the current thread
	 *
	 * @param message Log message
	 * @param level   Log level
	 * @param time    Log time
	 * @return true if log has been emitted
	 */
	public static boolean emitLaunchLog(final String message, final String level, final Date time) {
		return emitLaunchLog(launchUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setLevel(level);
			rq.setLogTime(time);
			rq.setLaunchUuid(launchUuid);
			rq.setMessage(message);
			return rq;
		});
	}

	private static void fillSaveLogRQ(final SaveLogRQ rq, final String message, final String level, final Date time, final File file) {
		rq.setMessage(message);
		rq.setLevel(level);
		rq.setLogTime(time);

		try {
			SaveLogRQ.File f = new SaveLogRQ.File();
			f.setContentType(detect(file));
			f.setContent(readFileToBytes(file));

			f.setName(UUID.randomUUID().toString());
			rq.setFile(f);
		} catch (IOException e) {
			// seems like there is some problem. Do not report an file
			LOGGER.error("Cannot send file to ReportPortal", e);
		}
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 *
	 * @param message Log message
	 * @param level   Log level
	 * @param time    Log time
	 * @param file    a file to attach to the log message
	 * @return true if log has been emitted
	 */
	public static boolean emitLog(final String message, final String level, final Date time, final File file) {
		return emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			fillSaveLogRQ(rq, message, level, time, file);
			return rq;
		});
	}

	/**
	 * Emits log message on Launch level if there is any active context attached to the current thread
	 *
	 * @param message Log message
	 * @param level   Log level
	 * @param time    Log time
	 * @param file    a file to attach to the log message
	 * @return true if log has been emitted
	 */
	public static boolean emitLaunchLog(final String message, final String level, final Date time, final File file) {
		return emitLaunchLog(launchUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setLaunchUuid(launchUuid);
			fillSaveLogRQ(rq, message, level, time, file);
			return rq;
		});
	}

	private static void fillSaveLogRQ(final SaveLogRQ rq, final String level, final Date time, final ReportPortalMessage message) {
		rq.setLevel(level);
		rq.setLogTime(time);
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
	}

	public static boolean emitLog(final ReportPortalMessage message, final String level, final Date time) {
		return emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			fillSaveLogRQ(rq, level, time, message);
			return rq;
		});
	}

	public static boolean emitLaunchLog(final ReportPortalMessage message, final String level, final Date time) {
		return emitLaunchLog(launchUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setLaunchUuid(launchUuid);
			fillSaveLogRQ(rq, level, time, message);
			return rq;
		});
	}

	public static class Builder {
		static final String API_V1_BASE = "/api/v1";
		static final String API_V2_BASE = "/api/v2";
		private static final String HTTPS = "https";

		private HttpClientBuilder httpClient;
		private ListenerParameters parameters;
		private ExecutorService executor;

		public Builder withHttpClient(HttpClientBuilder client) {
			this.httpClient = client;
			return this;
		}

		public Builder withParameters(ListenerParameters parameters) {
			this.parameters = parameters;
			return this;
		}

		public Builder withExecutorService(ExecutorService executor) {
			this.executor = executor;
			return this;
		}

		public ReportPortal build() {
			try {
				ListenerParameters params = ofNullable(this.parameters).orElse(new ListenerParameters(defaultPropertiesLoader()));
				ExecutorService executorService = executor == null ? buildExecutorService(params): executor;
				return new ReportPortal(buildClient(ReportPortalClient.class, params, executorService),
						executorService,
						params,
						buildLockFile(params)
				);
			} catch (Exception e) {
				String errMsg = "Cannot build ReportPortal client";
				LOGGER.error(errMsg, e);
				throw new InternalReportPortalClientException(errMsg, e);
			}
		}

		/**
		 * @param clientType a class to instantiate
		 * @param params     {@link ListenerParameters} Report Portal parameters
		 * @param <T>        Report Portal Client interface class
		 * @return a Report Portal Client instance
		 */
		public <T extends ReportPortalClient> T buildClient(@Nonnull final Class<T> clientType, @Nonnull final ListenerParameters params) {
			return buildClient(clientType, params, buildExecutorService(params));
		}

		/**
		 * @param clientType a class to instantiate
		 * @param params     {@link ListenerParameters} Report Portal parameters
		 * @param <T>        Report Portal Client interface class
		 * @param executor   {@link ExecutorService} an Executor which will be used for internal request / response queue processing
		 * @return a Report Portal Client instance
		 */
		public <T extends ReportPortalClient> T buildClient(@Nonnull final Class<T> clientType, @Nonnull final ListenerParameters params,
				@Nonnull final ExecutorService executor) {
			try {
				HttpClient client = ofNullable(this.httpClient)
						.map(c -> (HttpClient) c.addInterceptorLast(new BearerAuthInterceptor(params.getApiKey())).build())
						.orElse(defaultClient(params));
				return RestEndpoints.forInterface(clientType, buildRestEndpoint(params, client, executor));
			} catch (Exception e) {
				String errMsg = "Cannot build ReportPortal client";
				LOGGER.error(errMsg, e);
				throw new InternalReportPortalClientException(errMsg, e);
			}
		}

		/**
		 * @param parameters {@link ListenerParameters} Report Portal parameters
		 * @param client     {@link HttpClient} an apache HTTP client instance
		 * @return a ReportPortal endpoint description class
		 */
		protected RestEndpoint buildRestEndpoint(@Nonnull final ListenerParameters parameters, @Nonnull final HttpClient client) {
			return buildRestEndpoint(parameters, client, buildExecutorService(parameters));
		}

		/**
		 * @param parameters {@link ListenerParameters} Report Portal parameters
		 * @param client     {@link HttpClient} an apache HTTP client instance
		 * @param executor   {@link ExecutorService} an Executor which will be used for internal request / response queue processing
		 * @return a ReportPortal endpoint description class
		 */
		protected RestEndpoint buildRestEndpoint(@Nonnull final ListenerParameters parameters, @Nonnull final HttpClient client,
				@Nonnull final ExecutorService executor) {
			final ObjectMapper om = new ObjectMapper();
			om.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));
			om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			String baseUrl = parameters.getBaseUrl();
			String project = parameters.getProjectName();

			final JacksonSerializer jacksonSerializer = new JacksonSerializer(om);
			return new HttpClientRestEndpoint(client,
					new LinkedList<Serializer>() {{
						add(jacksonSerializer);
						add(new ByteArraySerializer());
					}},
					new ReportPortalErrorHandler(jacksonSerializer),
					buildEndpointUrl(baseUrl, project, parameters.isAsyncReporting()),
					executor
			);
		}

		protected String buildEndpointUrl(String baseUrl, String project, boolean asyncReporting) {
			String apiBase = asyncReporting ? API_V2_BASE : API_V1_BASE;
			return baseUrl + apiBase + "/" + project;
		}

		protected HttpClient defaultClient(ListenerParameters parameters) throws MalformedURLException {
			String baseUrl = parameters.getBaseUrl();
			if (baseUrl == null) {
				throw new InternalReportPortalClientException("Base url for Report Portal server is not set!");
			}
			String keyStore = parameters.getKeystore();
			String keyStorePassword = parameters.getKeystorePassword();

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
			String proxyUrl = parameters.getProxyUrl();
			if (proxyUrl != null) {
				builder.setProxy(HttpHost.create(proxyUrl));
			}
			builder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
					.setRetryHandler(new StandardHttpRequestRetryHandler(parameters.getTransferRetries(), true))
					.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
						@Override
						public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
							long keepAliveDuration = super.getKeepAliveDuration(response, context);
							if (keepAliveDuration == -1) {
								return parameters.getMaxConnectionTtlMs();
							}
							return keepAliveDuration;
						}
					})
					.setMaxConnPerRoute(parameters.getMaxConnectionsPerRoute())
					.setMaxConnTotal(parameters.getMaxConnectionsTotal())
					.setConnectionTimeToLive(parameters.getMaxConnectionTtlMs(), TimeUnit.MILLISECONDS)
					.evictIdleConnections(parameters.getMaxConnectionIdleTtlMs(), TimeUnit.MILLISECONDS);
			return builder.addInterceptorLast(new BearerAuthInterceptor(parameters.getApiKey())).build();
		}

		protected LockFile buildLockFile(ListenerParameters parameters) {
			return getLockFile(parameters);
		}

		protected PropertiesLoader defaultPropertiesLoader() {
			return PropertiesLoader.load();
		}

		protected ExecutorService buildExecutorService(ListenerParameters params) {
			return ReportPortal.buildExecutorService(params);
		}
	}

	private static ExecutorService buildExecutorService(ListenerParameters params) {
		return Executors.newFixedThreadPool(params.getIoPoolSize(), new ThreadFactoryBuilder().setNameFormat("rp-io-%s").build());
	}
}
