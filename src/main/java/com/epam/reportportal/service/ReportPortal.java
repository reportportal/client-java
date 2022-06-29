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
import com.epam.reportportal.service.launch.PrimaryLaunch;
import com.epam.reportportal.service.launch.SecondaryLaunch;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.epam.reportportal.service.LaunchLoggingContext.DEFAULT_LAUNCH_KEY;
import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.epam.reportportal.utils.files.Utils.readFileToBytes;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link retrofit2.Retrofit} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);

	private final ListenerParameters parameters;
	private final LaunchIdLock launchIdLock;
	private final ReportPortalClient rpClient;
	private final ExecutorService executor;

	/**
	 * @param rpClient   ReportPortal client
	 * @param parameters Listener Parameters
	 */
	ReportPortal(@Nullable ReportPortalClient rpClient, @Nonnull ExecutorService executor, @Nonnull ListenerParameters parameters,
			@Nullable LaunchIdLock launchIdLock) {
		this.rpClient = rpClient;
		this.executor = executor;
		this.parameters = Objects.requireNonNull(parameters);
		this.launchIdLock = launchIdLock;
	}

	/**
	 * Starts launch in ReportPortal
	 *
	 * @param rq Request Data
	 * @return Launch
	 */
	@Nonnull
	public Launch newLaunch(@Nonnull StartLaunchRQ rq) {
		if (BooleanUtils.isNotTrue(parameters.getEnable()) || rpClient == null) {
			return Launch.NOOP_LAUNCH;
		}

		if (launchIdLock == null) {
			// do not use multi-client mode
			return new LaunchImpl(rpClient, parameters, rq, executor);
		}

		final String instanceUuid = UUID.randomUUID().toString();
		final String uuid = launchIdLock.obtainLaunchUuid(instanceUuid);
		if (uuid == null) {
			// timeout locking on file or interrupted, anyway it should be logged already
			// we continue to operate normally, since this flag is set by default and we shouldn't fail launches because of it
			return new LaunchImpl(rpClient, parameters, rq, executor);
		}

		if (instanceUuid.equals(uuid)) {
			// We got our own UUID as launch UUID, that means we are primary launch.
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				StartLaunchRQ rqCopy = objectMapper.readValue(objectMapper.writeValueAsString(rq), StartLaunchRQ.class);
				rqCopy.setUuid(uuid);
				return new PrimaryLaunch(rpClient, parameters, rqCopy, executor, launchIdLock, instanceUuid);
			} catch (IOException e) {
				throw new InternalReportPortalClientException("Unable to clone start launch request:", e);
			}
		} else {
			Maybe<String> launch = Maybe.create(emitter -> {
				emitter.onSuccess(uuid);
				emitter.onComplete();
			});
			return new SecondaryLaunch(rpClient, parameters, launch, executor, launchIdLock, instanceUuid);
		}
	}

	/**
	 * Factory method for {@link ReportPortal} that uses already started launch
	 *
	 * @param launchUuid Launch to be used
	 * @return This instance for chaining
	 */
	@Nonnull
	public Launch withLaunch(@Nonnull Maybe<String> launchUuid) {
		return ofNullable(rpClient).map(c -> (Launch) new LaunchImpl(c, parameters, launchUuid, executor)).orElse(Launch.NOOP_LAUNCH);
	}

	/**
	 * @return Configuration parameters
	 */
	@Nonnull
	public ListenerParameters getParameters() {
		return parameters;
	}

	/**
	 * @return ReportPortal client
	 */
	@Nullable
	public ReportPortalClient getClient() {
		return this.rpClient;
	}

	/**
	 * Creates new builder for {@link ReportPortal}
	 *
	 * @return builder for {@link ReportPortal}
	 */
	@Nonnull
	public static Builder builder() {
		return new Builder();
	}

	private static LaunchIdLock getLaunchLock(ListenerParameters parameters) {
		return parameters.getClientJoin() ? parameters.getClientJoinMode().getInstance(parameters) : null;
	}

	/**
	 * Creates new ReportPortal based on already built dependencies
	 *
	 * @param client Report Portal Client
	 * @param params {@link ListenerParameters}
	 * @return builder for {@link ReportPortal}
	 */
	@Nonnull
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
	@Nonnull
	public static ReportPortal create(@Nonnull final ReportPortalClient client, @Nonnull final ListenerParameters params,
			@Nonnull final ExecutorService executor) {
		return new ReportPortal(client, executor, params, getLaunchLock(params));
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 *
	 * @param logSupplier Log supplier. Converts current Item ID to the {@link SaveLogRQ} object
	 * @return true if log has been emitted
	 */
	public static boolean emitLog(final Function<String, SaveLogRQ> logSupplier) {
		final LoggingContext loggingContext = LoggingContext.context();
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
		final LaunchLoggingContext launchLoggingContext = LaunchLoggingContext.context(DEFAULT_LAUNCH_KEY);
		if (null != launchLoggingContext) {
			launchLoggingContext.emit(logSupplier);
			return true;
		}
		return false;
	}

	/**
	 * Emits log message if there is any active context attached to the current thread
	 *
	 * @param itemUuid    Test Item ID promise
	 * @param logSupplier Log supplier. Converts current Item ID to the {@link SaveLogRQ} object
	 * @return true if log has been emitted
	 */
	public static boolean emitLog(Maybe<String> itemUuid, final Function<String, SaveLogRQ> logSupplier) {
		final LoggingContext loggingContext = LoggingContext.context();
		if (null != loggingContext) {
			loggingContext.emit(itemUuid, logSupplier);
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
		static final String API_PATH = "api/";
		private static final String HTTPS = "https";

		private OkHttpClient.Builder httpClient;
		private ListenerParameters parameters;
		private ExecutorService executor;

		public Builder withHttpClient(OkHttpClient.Builder client) {
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
			ListenerParameters params = ofNullable(this.parameters).orElse(new ListenerParameters(defaultPropertiesLoader()));
			ExecutorService executorService = executor == null ? buildExecutorService(params) : executor;
			Class<? extends ReportPortalClient> clientType = params.isAsyncReporting() ?
					ReportPortalClientV2.class :
					ReportPortalClient.class;
			return new ReportPortal(buildClient(clientType, params, executorService), executorService, params, buildLaunchLock(params));
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
			OkHttpClient client = ofNullable(this.httpClient).map(c -> c.addInterceptor(new BearerAuthInterceptor(params.getApiKey()))
					.build()).orElseGet(() -> defaultClient(params));

			return ofNullable(client).map(c -> buildRestEndpoint(params, c, executor).create(clientType)).orElse(null);
		}

		/**
		 * @param parameters {@link ListenerParameters} Report Portal parameters
		 * @param client     {@link OkHttpClient} an HTTP client instance
		 * @return a ReportPortal endpoint description class
		 */
		protected Retrofit buildRestEndpoint(@Nonnull final ListenerParameters parameters, @Nonnull final OkHttpClient client) {
			return buildRestEndpoint(parameters, client, buildExecutorService(parameters));
		}

		/**
		 * @param parameters {@link ListenerParameters} Report Portal parameters
		 * @param client     {@link OkHttpClient} an HTTP client instance
		 * @param executor   {@link ExecutorService} an Executor which will be used for internal request / response queue processing
		 * @return a ReportPortal endpoint description class
		 */
		protected Retrofit buildRestEndpoint(@Nonnull final ListenerParameters parameters, @Nonnull final OkHttpClient client,
				@Nonnull final ExecutorService executor) {
			String baseUrl = (parameters.getBaseUrl().endsWith("/") ? parameters.getBaseUrl() : parameters.getBaseUrl() + "/") + API_PATH;
			Retrofit.Builder builder = new Retrofit.Builder().client(client);
			try {
				builder.baseUrl(baseUrl);
			} catch (NoSuchMethodError e) {
				throw new InternalReportPortalClientException("Unable to initialize OkHttp client. "
						+ "Report Portal client supports OkHttp version 3.11.0 as minimum.\n"
								+ "Please update OkHttp dependency.\n"
								+ "Besides this usually happens due to old selenium-java version (it overrides our dependency), "
								+ "please use selenium-java 3.141.0 as minimum.",
						e
				);
			}
			return builder.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.from(executor)))
					.addConverterFactory(JacksonConverterFactory.create(HttpRequestUtils.MAPPER))
					.build();
		}

		@Nullable
		protected OkHttpClient defaultClient(@Nonnull ListenerParameters parameters) {
			String baseUrlStr = parameters.getBaseUrl();
			if (baseUrlStr == null) {
				LOGGER.warn("Base url for Report Portal server is not set!");
				return null;
			}

			URL baseUrl;
			try {
				baseUrl = new URL(baseUrlStr);
			} catch (MalformedURLException e) {
				LOGGER.warn("Unable to parse Report Portal URL", e);
				return null;
			}

			String keyStore = parameters.getKeystore();
			String keyStorePassword = parameters.getKeystorePassword();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();

			if (HTTPS.equals(baseUrl.getProtocol()) && keyStore != null) {
				if (null == keyStorePassword) {
					String error = "You should provide keystore password parameter [" + ListenerProperty.KEYSTORE_PASSWORD
							+ "] if you use HTTPS protocol";
					LOGGER.error(error);
					throw new InternalReportPortalClientException(error);
				}

				try {
					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					trustManagerFactory.init(SslUtils.loadKeyStore(keyStore, keyStorePassword));
					TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
					X509TrustManager trustManager = (X509TrustManager) ofNullable(trustManagers).flatMap(managers -> Arrays.stream(managers)
							.filter(m -> m instanceof X509TrustManager)
							.findAny())
							.orElseThrow(() -> new InternalReportPortalClientException(
									"Unable to find X509 trust manager, managers:" + Arrays.toString(trustManagers)));

					SSLContext sslContext = SSLContext.getInstance("TLS");
					sslContext.init(null, new TrustManager[] { trustManager }, null);
					SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
					builder.sslSocketFactory(sslSocketFactory, trustManager);
				} catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
					String error = "Unable to load trust store";
					LOGGER.error(error, e);
					throw new InternalReportPortalClientException(error, e);
				}
			}

			String proxyStr = parameters.getProxyUrl();
			if (isNotBlank(proxyStr)) {
				try {
					URL proxyUrl = new URL(proxyStr);
					int port = proxyUrl.getPort();
					builder.proxy(new Proxy(Proxy.Type.HTTP,
							InetSocketAddress.createUnresolved(proxyUrl.getHost(), port >= 0 ? port : proxyUrl.getDefaultPort())
					));
				} catch (MalformedURLException e) {
					LOGGER.warn("Unable to parse proxy URL", e);
					return null;
				}
			}

			builder.addInterceptor(new BearerAuthInterceptor(parameters.getApiKey()));
			builder.addInterceptor(new PathParamInterceptor("projectName", parameters.getProjectName()));
			if (parameters.isHttpLogging()) {
				HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
				logging.setLevel(HttpLoggingInterceptor.Level.BODY);
				builder.addInterceptor(logging);
			}

			ofNullable(parameters.getHttpCallTimeout()).ifPresent(builder::callTimeout);
			ofNullable(parameters.getHttpConnectTimeout()).ifPresent(builder::connectTimeout);
			ofNullable(parameters.getHttpReadTimeout()).ifPresent(builder::readTimeout);
			ofNullable(parameters.getHttpWriteTimeout()).ifPresent(builder::writeTimeout);

			builder.retryOnConnectionFailure(true).cookieJar(new CookieJar() {
				private final Map<String, CopyOnWriteArrayList<Cookie>> STORAGE = new ConcurrentHashMap<>();

				@Override
				public void saveFromResponse(@Nonnull HttpUrl url, @Nonnull List<Cookie> cookies) {
					STORAGE.computeIfAbsent(url.url().getHost(), u -> new CopyOnWriteArrayList<>()).addAll(cookies);
				}

				@Override
				@Nonnull
				public List<Cookie> loadForRequest(@Nonnull HttpUrl url) {
					return STORAGE.computeIfAbsent(url.url().getHost(), u -> new CopyOnWriteArrayList<>());
				}
			});
			return builder.build();
		}

		/**
		 * @param parameters Report Portal parameters
		 * @return Launch lock instance
		 * @deprecated use {@link #buildLaunchLock(ListenerParameters)}
		 */
		@Deprecated
		protected LaunchIdLock buildLockFile(ListenerParameters parameters) {
			return buildLaunchLock(parameters);
		}

		protected LaunchIdLock buildLaunchLock(ListenerParameters parameters) {
			return getLaunchLock(parameters);
		}

		protected PropertiesLoader defaultPropertiesLoader() {
			return PropertiesLoader.load();
		}

		protected ExecutorService buildExecutorService(ListenerParameters params) {
			return ReportPortal.buildExecutorService(params);
		}
	}

	private static ExecutorService buildExecutorService(ListenerParameters params) {
		return Executors.newFixedThreadPool(params.getIoPoolSize(),
				new ThreadFactoryBuilder().setNameFormat("rp-io-%s").setDaemon(true).build()
		);
	}
}
