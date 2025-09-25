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
import com.epam.reportportal.utils.MultithreadingUtils;
import com.epam.reportportal.utils.SslUtils;
import com.epam.reportportal.utils.files.Utils;
import com.epam.reportportal.utils.http.ClientUtils;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static com.epam.reportportal.utils.ObjectUtils.clonePojo;
import static com.epam.reportportal.utils.formatting.ExceptionUtils.getStackTrace;
import static java.util.Optional.ofNullable;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link retrofit2.Retrofit} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);
	private static final CookieJar COOKIE_JAR = new CookieJar() {
		private final Map<String, Map<String, Cookie>> HOST_STORAGE = new ConcurrentHashMap<>();

		@Override
		public void saveFromResponse(@Nonnull HttpUrl url, @Nonnull List<Cookie> cookies) {
			Map<String, Cookie> storage = HOST_STORAGE.computeIfAbsent(url.url().getHost(), u -> new ConcurrentHashMap<>());
			cookies.forEach(cookie -> storage.put(cookie.name(), cookie));
		}

		@Override
		@Nonnull
		public List<Cookie> loadForRequest(@Nonnull HttpUrl url) {
			return new ArrayList<>(HOST_STORAGE.computeIfAbsent(url.url().getHost(), u -> new ConcurrentHashMap<>()).values());
		}
	};

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

		StartLaunchRQ rqCopy = clonePojo(rq, StartLaunchRQ.class);
		String launchUuid = parameters.getLaunchUuid();
		boolean launchUuidSet = StringUtils.isNotBlank(launchUuid);
		if (launchUuidSet) {
			if (parameters.isLaunchUuidCreationSkip()) {
				// a Launch UUID specified, but we should skip its creation
				return new LaunchImpl(rpClient, parameters, Maybe.just(launchUuid), executor);
			} else {
				// a Launch UUID specified, but we should create a new Launch with it
				rqCopy.setUuid(launchUuid);
			}
		}

		if (launchIdLock == null) {
			// do not use multi-client mode
			return new LaunchImpl(rpClient, parameters, rqCopy, executor);
		}

		final String instanceUuid = UUID.randomUUID().toString();
		final String uuid = launchIdLock.obtainLaunchUuid(instanceUuid);
		if (uuid == null) {
			// timeout locking on file or interrupted, anyway it should be logged already
			// we continue to operate normally, since this flag is set by default, and we shouldn't fail launches because of it
			return new LaunchImpl(rpClient, parameters, rqCopy, executor);
		}

		if (instanceUuid.equals(uuid)) {
			// We got our own instance UUID, that means we are primary launch.
			if (!launchUuidSet) {
				// If we got Launch UUID from parameters, we should use it, otherwise we should use instance UUID as Launch UUID
				rqCopy.setUuid(instanceUuid);
			}
			return new PrimaryLaunch(rpClient, parameters, rqCopy, executor, launchIdLock, instanceUuid);
		} else {
			// If we got Launch UUID from parameters, we should use it, otherwise we should use obtained UUID as a Secondary Launch
			Maybe<String> launch = launchUuidSet ? Maybe.just(launchUuid) : Maybe.just(uuid);
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
	 * @param client ReportPortal Client
	 * @param params {@link ListenerParameters}
	 * @return builder for {@link ReportPortal}
	 */
	@Nonnull
	public static ReportPortal create(ReportPortalClient client, ListenerParameters params) {
		return create(client, params, MultithreadingUtils.buildExecutorService("rp-io-", params));
	}

	/**
	 * Creates new ReportPortal based on already built dependencies
	 *
	 * @param client   ReportPortal Client
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
		final Launch launch = Launch.currentLaunch();
		if (launch != null && launch != Launch.NOOP_LAUNCH) {
			launch.log(logSupplier);
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
	public static boolean emitLog(@Nullable String message, @Nullable String level, @Nonnull Comparable<? extends Comparable<?>> time) {
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
	public static boolean emitLaunchLog(@Nullable String message, @Nullable String level,
			@Nonnull Comparable<? extends Comparable<?>> time) {
		return emitLaunchLog(launchUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setLevel(level);
			rq.setLogTime(time);
			rq.setLaunchUuid(launchUuid);
			rq.setMessage(message);
			return rq;
		});
	}

	/**
	 * Converts {@link ReportPortalMessage} to {@link SaveLogRQ}.
	 *
	 * @param launchUuid a UUID of the launch
	 * @param itemUuid   a UUID of the test item
	 * @param level      message level
	 * @param time       timestamp of the message
	 * @param message    an object to convert
	 * @return converted object
	 */
	@Nonnull
	public static SaveLogRQ toSaveLogRQ(@Nullable String launchUuid, @Nullable String itemUuid, @Nullable String level,
			@Nonnull Comparable<? extends Comparable<?>> time, @Nonnull ReportPortalMessage message) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setItemUuid(itemUuid);
		rq.setLaunchUuid(launchUuid);
		rq.setLevel(level);
		rq.setLogTime(time);
		rq.setMessage(message.getMessage());
		final TypeAwareByteSource data = message.getData();
		if (data != null) {
			try {
				SaveLogRQ.File file = new SaveLogRQ.File();
				file.setContent(data.read());
				file.setContentType(data.getMediaType());
				file.setName(UUID.randomUUID().toString());
				rq.setFile(file);
			} catch (IOException e) {
				LOGGER.error("Cannot send file to ReportPortal", e);
			}
		}
		return rq;
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
	public static boolean emitLog(@Nullable String message, @Nullable String level, @Nonnull Comparable<? extends Comparable<?>> time,
			final File file) {
		return emitLog(itemUuid -> {
			try {
				TypeAwareByteSource byteSource = Utils.getFile(file);
				return toSaveLogRQ(null, itemUuid, level, time, new ReportPortalMessage(byteSource, message));
			} catch (IOException e) {
				LOGGER.error("Cannot read file", e);
				return toSaveLogRQ(null, itemUuid, level, time, new ReportPortalMessage(message));
			}
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
	public static boolean emitLaunchLog(@Nullable String message, @Nullable String level, @Nonnull Comparable<? extends Comparable<?>> time,
			final File file) {
		return emitLaunchLog(launchUuid -> {
			try {
				TypeAwareByteSource byteSource = Utils.getFile(file);
				return toSaveLogRQ(launchUuid, null, level, time, new ReportPortalMessage(byteSource, message));
			} catch (IOException e) {
				LOGGER.error("Cannot read file", e);
				return toSaveLogRQ(launchUuid, null, level, time, new ReportPortalMessage(message));
			}
		});
	}

	/**
	 * Emit log message to the current test item.
	 *
	 * @param message an instance of the message
	 * @param level   message level
	 * @param time    timestamp of the message
	 * @return true if log has been emitted otherwise false
	 */
	public static boolean emitLog(final ReportPortalMessage message, @Nullable String level,
			@Nonnull Comparable<? extends Comparable<?>> time) {
		return emitLog(itemUuid -> toSaveLogRQ(null, itemUuid, level, time, message));
	}

	/**
	 * Emit log message to the current Launch.
	 *
	 * @param message an instance of the message
	 * @param level   message level
	 * @param time    timestamp of the message
	 * @return true if log has been emitted otherwise false
	 */
	public static boolean emitLaunchLog(final ReportPortalMessage message, @Nullable String level,
			@Nonnull Comparable<? extends Comparable<?>> time) {
		return emitLaunchLog(launchUuid -> toSaveLogRQ(launchUuid, null, level, time, message));
	}

	/**
	 * Formats and reports a {@link Throwable} to ReportPortal
	 *
	 * @param cause a {@link Throwable}
	 */
	public static void sendStackTraceToRP(final Throwable cause) {
		Launch launch = Launch.currentLaunch();
		ListenerParameters myParameters = ofNullable(launch).map(Launch::getParameters).orElseGet(ListenerParameters::new);
		boolean useMicroseconds = ofNullable(launch).map(Launch::useMicroseconds).orElse(false);
		Throwable base = new Throwable();
		ReportPortal.emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel("ERROR");
			rq.setLogTime(useMicroseconds ? Instant.now() : Calendar.getInstance().getTime());
			if (cause != null) {
				if (myParameters.isExceptionTruncate()) {
					rq.setMessage(getStackTrace(cause, base));
				} else {
					rq.setMessage(ExceptionUtils.getStackTrace(cause));
				}
			} else {
				rq.setMessage("Test has failed without exception");
			}
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
		 * @param params     {@link ListenerParameters} ReportPortal parameters
		 * @param <T>        ReportPortal Client interface class
		 * @return a ReportPortal Client instance
		 */
		public <T extends ReportPortalClient> T buildClient(@Nonnull final Class<T> clientType, @Nonnull final ListenerParameters params) {
			return buildClient(clientType, params, buildExecutorService(params));
		}

		/**
		 * @param clientType a class to instantiate
		 * @param params     {@link ListenerParameters} ReportPortal parameters
		 * @param <T>        ReportPortal Client interface class
		 * @param executor   {@link ExecutorService} an Executor which will be used for internal request / response queue processing
		 * @return a ReportPortal Client instance
		 */
		public <T extends ReportPortalClient> T buildClient(@Nonnull final Class<T> clientType, @Nonnull final ListenerParameters params,
				@Nonnull final ExecutorService executor) {
			OkHttpClient client = ofNullable(this.httpClient).map(c -> c.addInterceptor(new BearerAuthInterceptor(params.getApiKey()))
					.build()).orElseGet(() -> defaultClient(params));

			return ofNullable(client).map(c -> buildRestEndpoint(params, c, executor).create(clientType)).orElse(null);
		}

		/**
		 * @param parameters {@link ListenerParameters} ReportPortal parameters
		 * @param client     {@link OkHttpClient} an HTTP client instance
		 * @return a ReportPortal endpoint description class
		 */
		protected Retrofit buildRestEndpoint(@Nonnull final ListenerParameters parameters, @Nonnull final OkHttpClient client) {
			return buildRestEndpoint(parameters, client, buildExecutorService(parameters));
		}

		/**
		 * @param parameters {@link ListenerParameters} ReportPortal parameters
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
				throw new InternalReportPortalClientException(
						"Unable to initialize OkHttp client. ReportPortal client supports OkHttp version 3.11.0 as minimum.\n"
								+ "Please upComparable<? extends Comparable<?>>  OkHttp dependency.\n"
								+ "Besides this usually happens due to old selenium-java version (it overrides our dependency), "
								+ "please use selenium-java 3.141.0 as minimum.", e
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
				LOGGER.warn("Base url for ReportPortal server is not set!");
				return null;
			}

			URL baseUrl;
			try {
				baseUrl = new URL(baseUrlStr);
			} catch (MalformedURLException e) {
				LOGGER.warn("Unable to parse ReportPortal URL", e);
				return null;
			}

			String keyStore = parameters.getKeystore();
			String keyStorePassword = parameters.getKeystorePassword();
			String trustStore = parameters.getTruststore();
			String trustStorePassword = parameters.getTruststorePassword();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();

			if (HTTPS.equals(baseUrl.getProtocol()) && (keyStore != null || trustStore != null)) {
				KeyManager[] keyManagers = null;
				if (keyStore != null) {
					KeyStore ks = SslUtils.loadKeyStore(keyStore, keyStorePassword, parameters.getKeystoreType());
					try {
						KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
						kmf.init(ks, ofNullable(keyStorePassword).map(String::toCharArray).orElse(null));
						keyManagers = kmf.getKeyManagers();
					} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
						String error = "Unable to load key store";
						LOGGER.error(error, e);
						throw new InternalReportPortalClientException(error, e);
					}
				}

				TrustManager[] trustManagers = null;
				if (trustStore != null) {
					KeyStore ts = SslUtils.loadKeyStore(trustStore, trustStorePassword, parameters.getTruststoreType());
					try {
						TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						tmf.init(ts);
						trustManagers = tmf.getTrustManagers();
					} catch (KeyStoreException | NoSuchAlgorithmException e) {
						String trustStoreError = "Unable to load trust store";
						LOGGER.error(trustStoreError, e);
						throw new InternalReportPortalClientException(trustStoreError, e);
					}
				}

				if (trustManagers == null) {
					try {
						TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						tmf.init((KeyStore) null);
						trustManagers = tmf.getTrustManagers();
					} catch (NoSuchAlgorithmException | KeyStoreException e) {
						String trustStoreError = "Unable to load default trust store";
						LOGGER.error(trustStoreError, e);
						throw new InternalReportPortalClientException(trustStoreError, e);
					}
				}

				try {
					SSLContext sslContext = SSLContext.getInstance("TLS");
					sslContext.init(keyManagers, trustManagers, new SecureRandom());
					X509TrustManager trustManager = Arrays.stream(ofNullable(trustManagers).orElse(new TrustManager[] {}))
							.filter(m -> m instanceof X509TrustManager)
							.map(m -> (X509TrustManager) m)
							.findAny()
							.orElseThrow(() -> new InternalReportPortalClientException("Unable to find X509 trust manager"));
					builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
				} catch (NoSuchAlgorithmException | KeyManagementException e) {
					String error = "Unable to initialize SSL context";
					LOGGER.error(error, e);
					throw new InternalReportPortalClientException(error, e);
				}
			}

			ClientUtils.setupProxy(builder, parameters);
			builder.addInterceptor(new BearerAuthInterceptor(parameters.getApiKey()));
			builder.addInterceptor(new PathParamInterceptor("projectName", parameters.getProjectName()));
			if (parameters.isHttpLogging()) {
				HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
				logging.setLevel(HttpLoggingInterceptor.Level.BODY);
				builder.addNetworkInterceptor(logging);
			}

			ofNullable(parameters.getHttpCallTimeout()).ifPresent(builder::callTimeout);
			ofNullable(parameters.getHttpConnectTimeout()).ifPresent(builder::connectTimeout);
			ofNullable(parameters.getHttpReadTimeout()).ifPresent(builder::readTimeout);
			ofNullable(parameters.getHttpWriteTimeout()).ifPresent(builder::writeTimeout);

			builder.retryOnConnectionFailure(true).cookieJar(COOKIE_JAR);
			return builder.build();
		}

		protected LaunchIdLock buildLaunchLock(ListenerParameters parameters) {
			return getLaunchLock(parameters);
		}

		protected PropertiesLoader defaultPropertiesLoader() {
			return PropertiesLoader.load();
		}

		protected ExecutorService buildExecutorService(ListenerParameters params) {
			return MultithreadingUtils.buildExecutorService("rp-io-", params);
		}
	}
}
