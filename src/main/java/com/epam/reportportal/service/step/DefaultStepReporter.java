/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service.step;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.files.Utils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Optional.ofNullable;

/**
 * {@inheritDoc}
 */
public class DefaultStepReporter implements StepReporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStepReporter.class);

	// Do not use InheritableThreadLocal here, or it will be the same issue as here:
	// https://github.com/reportportal/agent-java-testNG/issues/76
	private final ThreadLocal<Deque<Maybe<String>>> parents = ThreadLocal.withInitial(ArrayDeque::new);

	// Do not use InheritableThreadLocal here, or it will be the same issue as here:
	// https://github.com/reportportal/agent-java-testNG/issues/76
	private final ThreadLocal<Deque<StepEntry>> steps = ThreadLocal.withInitial(ArrayDeque::new);

	private final Set<Maybe<String>> parentFailures = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final Launch launch;

	public DefaultStepReporter(Launch currentLaunch) {
		launch = currentLaunch;
	}

	@Override
	public void setParent(final Maybe<String> parentUuid) {
		if (parentUuid != null) {
			parents.get().add(parentUuid);
		}
	}

	@Override
	public Maybe<String> getParent() {
		return parents.get().peekLast();
	}

	@Override
	public void removeParent(final Maybe<String> parentUuid) {
		if (parentUuid != null) {
			parents.get().removeLastOccurrence(parentUuid);
			parentFailures.remove(parentUuid);
		}
	}

	@Override
	public boolean isFailed(final Maybe<String> parentId) {
		if (parentId != null) {
			return parentFailures.contains(parentId);
		}
		return false;
	}

	protected void sendStep(final ItemStatus status, final String name, final Runnable actions) {
		StartTestItemRQ rq = buildStartStepRequest(name);
		Maybe<String> stepId = startStepRequest(rq);
		if (actions != null) {
			try {
				actions.run();
			} catch (Throwable e) {
				LOGGER.error("Unable to process nested step: " + e.getLocalizedMessage(), e);
			}
		}
		finishStepRequest(stepId, status, rq.getStartTime());
	}

	@Override
	public void sendStep(final String name) {
		sendStep(ItemStatus.PASSED, name, () -> {
		});
	}

	@Override
	public void sendStep(final String name, final String... logs) {
		sendStep(ItemStatus.PASSED, name, logs);
	}

	@Override
	public void sendStep(@Nonnull final ItemStatus status, final String name) {
		sendStep(status, name, () -> {
		});
	}

	@Override
	public void sendStep(@Nonnull final ItemStatus status, final String name, final String... logs) {
		Runnable actions = ofNullable(logs).map(l -> (Runnable) () -> Arrays.stream(l)
				.forEach(log -> ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, log, LogLevel.INFO)))).orElse(null);

		sendStep(status, name, actions);
	}

	@Override
	public void sendStep(final @Nonnull ItemStatus status, final String name, final Throwable throwable) {
		sendStep(status, name, () -> ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, throwable)));
	}

	@Override
	public void sendStep(final String name, final File... files) {
		sendStep(ItemStatus.PASSED, name, files);
	}

	@Override
	public void sendStep(final @Nonnull ItemStatus status, final String name, final File... files) {
		Runnable actions = ofNullable(files).map(f -> (Runnable) () -> Arrays.stream(f)
				.forEach(file -> ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, "", LogLevel.INFO, file)))).orElse(null);

		sendStep(status, name, actions);
	}

	@Override
	public void sendStep(final @Nonnull ItemStatus status, final String name, final Throwable throwable, final File... files) {
		sendStep(status, name, () -> {
			for (final File file : files) {
				ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, throwable, file));
			}
		});
	}

	private Optional<StepEntry> finishPreviousStepInternal() {
		return ofNullable(steps.get().poll()).map(stepEntry -> {
			launch.finishTestItem(stepEntry.getItemId(), stepEntry.getFinishTestItemRQ());
			return stepEntry;
		});
	}

	@Override
	public void finishPreviousStep() {
		finishPreviousStepInternal().ifPresent(e -> {
			if (ItemStatus.FAILED.name().equalsIgnoreCase(e.getFinishTestItemRQ().getStatus())) {
				parentFailures.addAll(parents.get());
			}
		});
	}

	private Maybe<String> startStepRequest(final StartTestItemRQ startTestItemRQ) {
		finishPreviousStepInternal().ifPresent(e -> {
			Date previousDate = e.getTimestamp();
			Date currentDate = startTestItemRQ.getStartTime();
			if (!previousDate.before(currentDate)) {
				startTestItemRQ.setStartTime(new Date(previousDate.getTime() + 1));
			}
			if (ItemStatus.FAILED.name().equalsIgnoreCase(e.getFinishTestItemRQ().getStatus())) {
				parentFailures.addAll(parents.get());
			}
		});
		return launch.startTestItem(parents.get().getLast(), startTestItemRQ);
	}

	private StartTestItemRQ buildStartStepRequest(String name) {
		StartTestItemRQ startTestItemRQ = new StartTestItemRQ();
		startTestItemRQ.setName(name);
		startTestItemRQ.setType("STEP");
		startTestItemRQ.setHasStats(false);
		startTestItemRQ.setStartTime(Calendar.getInstance().getTime());
		return startTestItemRQ;
	}

	private void finishStepRequest(Maybe<String> stepId, ItemStatus status, Date timestamp) {
		FinishTestItemRQ finishTestItemRQ = buildFinishTestItemRequest(status, Calendar.getInstance().getTime());
		steps.get().add(new StepEntry(stepId, timestamp, finishTestItemRQ));
	}

	private FinishTestItemRQ buildFinishTestItemRequest(ItemStatus status, Date endTime) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status.name());
		finishTestItemRQ.setEndTime(endTime);
		return finishTestItemRQ;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String message, LogLevel level) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setItemUuid(itemId);
		rq.setMessage(message);
		rq.setLevel(level.name());
		rq.setLogTime(Calendar.getInstance().getTime());
		return rq;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String message, LogLevel level, File file) {
		SaveLogRQ logRQ = buildSaveLogRequest(itemId, message, level);
		if (file != null) {
			try {
				logRQ.setFile(createFileModel(file));
			} catch (IOException e) {
				LOGGER.error("Unable to read file attachment: " + e.getMessage(), e);
			}
		}
		return logRQ;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, Throwable throwable, File file) {
		String message = throwable != null ? getStackTraceAsString(throwable) : "Test has failed without exception";
		return buildSaveLogRequest(itemId, message, LogLevel.ERROR, file);
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, Throwable throwable) {
		return buildSaveLogRequest(itemId, throwable, null);
	}

	private SaveLogRQ.File createFileModel(File file) throws IOException {
		TypeAwareByteSource dataSource = Utils.getFile(file);
		SaveLogRQ.File fileModel = new SaveLogRQ.File();
		fileModel.setContent(dataSource.read());
		fileModel.setContentType(dataSource.getMediaType());
		fileModel.setName(UUID.randomUUID().toString());
		return fileModel;
	}
}
