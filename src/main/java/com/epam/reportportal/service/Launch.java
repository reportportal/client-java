package com.epam.reportportal.service;

import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.google.common.io.Files.toByteArray;

public abstract class Launch {
	static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param rq Finish RQ
	 */
	abstract public void finish(final FinishExecutionRQ rq);

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq Start RQ
	 * @return Test Item ID promise
	 */
	abstract public Maybe<String> startTestItem(final StartTestItemRQ rq);

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq Start RQ
	 * @return Test Item ID promise
	 */
	abstract public Maybe<String> startTestItem(final Maybe<String> parentId, final StartTestItemRQ rq);

	/**
	 * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
	 *
	 * @param itemId Item ID promise
	 * @param rq     Finish request
	 */
	abstract public void finishTestItem(Maybe<String> itemId, final FinishTestItemRQ rq);

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

	/**
	 * Implementation for disabled Reporting
	 */
	public static final Launch NOOP_LAUNCH = new Launch() {

		@Override
		public void finish(FinishExecutionRQ rq) {

		}

		@Override
		public Maybe<String> startTestItem(StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		public Maybe<String> startTestItem(Maybe<String> parentId, StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		public void finishTestItem(Maybe<String> itemId, FinishTestItemRQ rq) {

		}
	};
}
