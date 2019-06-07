package com.epam.reportportal.service;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.Constants;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.epam.reportportal.utils.SubscriptionUtils.logFlowableResults;
import static com.epam.reportportal.utils.files.ImageConverter.convert;
import static com.epam.reportportal.utils.files.ImageConverter.isImage;
import static com.google.common.io.ByteSource.wrap;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class LaunchLoggingContext {

	/* default back-pressure buffer size */
	public static final int DEFAULT_BUFFER_SIZE = 10;
	public static final String DEFAULT_LAUNCH_KEY = "default";

	static ConcurrentHashMap<String, LaunchLoggingContext> loggingContextMap = new ConcurrentHashMap<String, LaunchLoggingContext>();
	/* Log emitter */
	private final PublishSubject<Maybe<SaveLogRQ>> emitter;
	/* ID of TestItem in ReportPortal */
	private final Maybe<String> launchId;
	/* Whether Image should be converted to BlackAndWhite */
	private final boolean convertImages;

	LaunchLoggingContext(Maybe<String> launchId, final ReportPortalClient client, int bufferSize, boolean convertImages) {
		this.launchId = launchId;
		this.emitter = PublishSubject.create();
		this.convertImages = convertImages;
		emitter.toFlowable(BackpressureStrategy.BUFFER).flatMap(new Function<Maybe<SaveLogRQ>, Publisher<SaveLogRQ>>() {
			@Override
			public Publisher<SaveLogRQ> apply(Maybe<SaveLogRQ> rq) throws Exception {
				return rq.toFlowable();
			}
		}).buffer(bufferSize).flatMap(new Function<List<SaveLogRQ>, Flowable<BatchSaveOperatingRS>>() {
			@Override
			public Flowable<BatchSaveOperatingRS> apply(List<SaveLogRQ> rqs) throws Exception {
				MultiPartRequest.Builder builder = new MultiPartRequest.Builder();

				builder.addSerializedPart(Constants.LOG_REQUEST_JSON_PART, rqs);

				for (SaveLogRQ rq : rqs) {
					final SaveLogRQ.File file = rq.getFile();
					if (null != file) {
						builder.addBinaryPart(
								Constants.LOG_REQUEST_BINARY_PART,
								file.getName(),
								Strings.isNullOrEmpty(file.getContentType()) ? MediaType.OCTET_STREAM.toString() : file.getContentType(),
								wrap(file.getContent())
						);
					}
				}
				return client.log(builder.build()).toFlowable();
			}
		}).doOnError(new Consumer<Throwable>() {
			@Override
			public void accept(Throwable throwable) throws Exception {
				throwable.printStackTrace();
			}
		}).observeOn(Schedulers.computation()).subscribe(logFlowableResults("Launch logging context"));

	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchId Launch ID
	 * @param client   Client of ReportPortal
	 * @return New Logging Context
	 */
	public static LaunchLoggingContext init(Maybe<String> launchId, final ReportPortalClient client) {
		return init(launchId, client, DEFAULT_BUFFER_SIZE, false);
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchId      Launch ID
	 * @param client        Client of ReportPortal
	 * @param bufferSize    Size of back-pressure buffer
	 * @param convertImages Whether Image should be converted to BlackAndWhite
	 * @return New Logging Context
	 */
	public static LaunchLoggingContext init(Maybe<String> launchId, final ReportPortalClient client, int bufferSize,
			boolean convertImages) {
		LaunchLoggingContext context = new LaunchLoggingContext(launchId, client, bufferSize, convertImages);
		loggingContextMap.put(DEFAULT_LAUNCH_KEY, context);
		return context;
	}

	/**
	 * Completes context attached to the current thread
	 *
	 * @return Waiting queue to be able to track request sending completion
	 */
	public static Completable complete() {
		final LaunchLoggingContext loggingContext = loggingContextMap.get(DEFAULT_LAUNCH_KEY);
		if (null != loggingContext) {
			return loggingContext.completed();
		} else {
			return Maybe.empty().ignoreElement();
		}
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	public void emit(final com.google.common.base.Function<String, SaveLogRQ> logSupplier) {
		emitter.onNext(launchId.map(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String input) throws Exception {
				final SaveLogRQ rq = logSupplier.apply(input);
				SaveLogRQ.File file = rq.getFile();
				if (convertImages && null != file && isImage(file.getContentType())) {
					final TypeAwareByteSource source = convert(wrap(file.getContent()));
					file.setContent(source.read());
					file.setContentType(source.getMediaType());
				}
				return rq;
			}
		}));

	}

	/**
	 * Marks flow as completed
	 *
	 * @return {@link Completable}
	 */
	public Completable completed() {
		emitter.onComplete();
		return emitter.ignoreElements();
	}
}
