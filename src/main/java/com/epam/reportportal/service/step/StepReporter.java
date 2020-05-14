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
import com.google.common.base.Function;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Optional.ofNullable;

/**
 * A class for manual nested step reporting.
 * <p>
 * Usage:
 * <code>
 * StepReporter stepReporter = Launch.currentLaunch().getStepReporter();
 * stepReporter.sendStep("My step name");
 * // step actions
 * stepReporter.sendStep(ItemStatus.FAILED, "My failure step name", new File("screenshot/fail.jpg"));
 * </code>
 */
public class StepReporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(StepReporter.class);

	private final Deque<Maybe<String>> parent = new ConcurrentLinkedDeque<>();

	private final Deque<StepEntry> steps = new ConcurrentLinkedDeque<>();
	;

	private final Set<Maybe<String>> parentFailures = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final Launch launch;

	public StepReporter(Launch currentLaunch) {
		launch = currentLaunch;
	}

	private static class StepEntry {
		private final Maybe<String> itemId;
		private final Date timestamp;
		private final FinishTestItemRQ finishTestItemRQ;

		private StepEntry(Maybe<String> itemId, Date timestamp, FinishTestItemRQ finishTestItemRQ) {
			this.itemId = itemId;
			this.timestamp = timestamp;
			this.finishTestItemRQ = finishTestItemRQ;
		}

		public Maybe<String> getItemId() {
			return itemId;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public FinishTestItemRQ getFinishTestItemRQ() {
			return finishTestItemRQ;
		}
	}

	public void setParent(final Maybe<String> parentUuid) {
		if (parentUuid != null) {
			parent.add(parentUuid);
		}
	}

	public Maybe<String> getParent() {
		return parent.peekLast();
	}

	public void removeParent(final Maybe<String> parentUuid) {
		if (parentUuid != null) {
			parent.removeLastOccurrence(parentUuid);
			parentFailures.remove(parentUuid);
		}
	}

	public boolean isFailed(final Maybe<String> parentId) {
		return parentFailures.contains(parentId);
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

	public void sendStep(final String name) {
		sendStep(ItemStatus.PASSED, name, () -> {
		});
	}

	public void sendStep(final String name, final String... logs) {
		sendStep(ItemStatus.PASSED, name, logs);
	}

	public void sendStep(@NotNull final ItemStatus status, final String name) {
		sendStep(status, name, () -> {
		});
	}

	public void sendStep(@NotNull final ItemStatus status, final String name, final String... logs) {
		Runnable actions = ofNullable(logs).map(l -> (Runnable) () -> Arrays.stream(l)
				.forEach(log -> ReportPortal.emitLog((Function<String, SaveLogRQ>) itemId -> buildSaveLogRequest(itemId,
						log,
						LogLevel.INFO
				)))).orElse(null);

		sendStep(status, name, actions);
	}

	public void sendStep(final @NotNull ItemStatus status, final String name, final Throwable throwable) {
		sendStep(status,
				name,
				() -> ReportPortal.emitLog((Function<String, SaveLogRQ>) itemId -> buildSaveLogRequest(itemId, LogLevel.ERROR, throwable))
		);
	}

	public void sendStep(final String name, final File... files) {
		sendStep(ItemStatus.PASSED, name, files);
	}

	public void sendStep(final @NotNull ItemStatus status, final String name, final File... files) {
		Runnable actions = ofNullable(files).map(f -> (Runnable) () -> Arrays.stream(f)
				.forEach(file -> ReportPortal.emitLog((Function<String, SaveLogRQ>) itemId -> buildSaveLogRequest(itemId,
						null,
						LogLevel.INFO,
						file
				)))).orElse(null);

		sendStep(status, name, actions);
	}

	public void sendStep(final @NotNull ItemStatus status, final String name, final Throwable throwable, final File... files) {
		sendStep(status, name, () -> {
			for (final File file : files) {
				ReportPortal.emitLog((Function<String, SaveLogRQ>) itemId -> buildSaveLogRequest(itemId, LogLevel.ERROR, throwable, file));
			}
		});
	}

	private Optional<StepEntry> finishPreviousStepInternal() {
		return ofNullable(steps.poll()).map(stepEntry -> {
			launch.finishTestItem(stepEntry.getItemId(), stepEntry.getFinishTestItemRQ());
			return stepEntry;
		});
	}

	public void finishPreviousStep() {
		finishPreviousStepInternal().ifPresent(e -> {
			if (ItemStatus.FAILED.name().equalsIgnoreCase(e.getFinishTestItemRQ().getStatus())) {
				parentFailures.add(parent.getLast());
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
				parentFailures.add(parent.getLast());
			}
		});
		return launch.startTestItem(parent.getLast(), startTestItemRQ);
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
		steps.add(new StepEntry(stepId, timestamp, finishTestItemRQ));
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

	private SaveLogRQ buildSaveLogRequest(String itemId, LogLevel level, Throwable throwable, File file) {
		String message = throwable != null ? getStackTraceAsString(throwable) : "Test has failed without exception";
		return buildSaveLogRequest(itemId, message, level, file);
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, LogLevel level, Throwable throwable) {
		return buildSaveLogRequest(itemId, level, throwable, null);
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
