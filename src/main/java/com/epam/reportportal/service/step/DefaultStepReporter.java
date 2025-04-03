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
import com.epam.reportportal.utils.ObjectUtils;
import com.epam.reportportal.utils.StatusEvaluation;
import com.epam.reportportal.utils.files.Utils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

import static com.epam.reportportal.service.step.StepRequestUtils.buildFinishTestItemRequest;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * {@inheritDoc}
 */
@SuppressWarnings("ReactiveStreamsUnusedPublisher")
public class DefaultStepReporter implements StepReporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStepReporter.class);

	private final ThreadLocal<Deque<Maybe<String>>> stepStack = ThreadLocal.withInitial(ConcurrentLinkedDeque::new);

	private final Map<Maybe<String>, StepEntry> steps = new ConcurrentHashMap<>();

	private final Deque<Maybe<String>> imperativeSteps = new ConcurrentLinkedDeque<>();

	private final Set<Maybe<String>> parentFailures = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final Launch launch;

	public DefaultStepReporter(Launch currentLaunch) {
		launch = currentLaunch;
	}

	private Deque<Maybe<String>> getParentStack() {
		return stepStack.get();
	}

	@Override
	@Nullable
	public Maybe<String> getParent() {
		return getParentStack().peekLast();
	}

	@Override
	public void setStepStatus(@Nonnull ItemStatus status) {
		ofNullable(steps.get(getParent())).ifPresent(step -> step.getFinishTestItemRQ().setStatus(status.name()));
	}

	@Override
	public void setParent(@Nullable final Maybe<String> parentUuid) {
		if (parentUuid != null) {
			getParentStack().add(parentUuid);
		}
	}

	@Override
	public void removeParent(@Nullable final Maybe<String> parentUuid) {
		if (parentUuid != null) {
			getParentStack().removeLastOccurrence(parentUuid);
			parentFailures.remove(parentUuid);
		}
	}

	@Override
	public boolean isFailed(@Nullable final Maybe<String> parentId) {
		if (parentId != null) {
			return parentFailures.contains(parentId);
		}
		return false;
	}

	@Nonnull
	protected Maybe<String> sendStep(@Nonnull final ItemStatus status, @Nonnull final String name, @Nullable final Runnable actions) {
		StartTestItemRQ rq = buildStartStepRequest(name);
		Maybe<String> stepId = startStepRequest(rq);
		imperativeSteps.add(stepId);
		if (actions != null) {
			try {
				actions.run();
			} catch (Throwable e) {
				LOGGER.error("Unable to process nested step: {}", e.getLocalizedMessage(), e);
			}
		}
		addStepEntry(stepId, status, rq.getStartTime());
		return stepId;
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(@Nonnull final ItemStatus status, @Nonnull final String name) {
		return sendStep(
				status, name, () -> {
				}
		);
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(@Nonnull final String name) {
		return sendStep(ItemStatus.PASSED, name);
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(@Nonnull final ItemStatus status, @Nonnull final String name, final String... logs) {
		Runnable actions = ofNullable(logs).map(l -> (Runnable) () -> Arrays.stream(l)
				.forEach(log -> ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, log, LogLevel.INFO)))).orElse(null);

		return sendStep(status, name, actions);
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(@Nonnull final String name, final String... logs) {
		return sendStep(ItemStatus.PASSED, name, logs);
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(final @Nonnull ItemStatus status, @Nonnull final String name, final Throwable throwable) {
		return sendStep(status, name, () -> ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, throwable)));
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(final @Nonnull ItemStatus status, @Nonnull final String name, final File... files) {
		Runnable actions = ofNullable(files).map(f -> (Runnable) () -> Arrays.stream(f)
				.forEach(file -> ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, "", LogLevel.INFO, file)))).orElse(null);

		return sendStep(status, name, actions);
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(@Nonnull final String name, final File... files) {
		return sendStep(ItemStatus.PASSED, name, files);
	}

	@Override
	@Nonnull
	public Maybe<String> sendStep(final @Nonnull ItemStatus status, @Nonnull final String name, final Throwable throwable,
			final File... files) {
		return sendStep(
				status, name, () -> {
					for (final File file : files) {
						ReportPortal.emitLog(itemId -> buildSaveLogRequest(itemId, throwable, file));
					}
				}
		);
	}

	private Optional<StepEntry> finishPreviousStepInternal(@Nullable ItemStatus finishStatus) {
		return ofNullable(imperativeSteps.pollLast()).map(steps::remove).map(stepEntry -> {
			FinishTestItemRQ finishRq = stepEntry.getFinishTestItemRQ();
			ItemStatus status = StatusEvaluation.evaluateStatus(
					ofNullable(finishRq.getStatus()).map(ItemStatus::valueOf)
							.orElse(ItemStatus.PASSED), finishStatus
			);
			ofNullable(status).ifPresent(s -> finishRq.setStatus(s.name()));
			launch.finishTestItem(stepEntry.getItemId(), finishRq);
			return stepEntry;
		});
	}

	private void failParents() {
		parentFailures.addAll(getParentStack());
	}

	/**
	 * Finish current step started by any of <code>#sendStep</code> methods. Overrides original status if provided.
	 *
	 * @param status finish status
	 */
	@Override
	@Nonnull
	public Maybe<String> finishPreviousStep(@Nullable ItemStatus status) {
		return finishPreviousStepInternal(status).map(e -> {
			if (ItemStatus.FAILED.name().equalsIgnoreCase(e.getFinishTestItemRQ().getStatus())) {
				failParents();
			}
			return e.getItemId();
		}).orElse(Maybe.empty());
	}

	/**
	 * Finish current step started by any of <code>#sendStep</code> methods.
	 */
	@Override
	@Nonnull
	public Maybe<String> finishPreviousStep() {
		return finishPreviousStep(null);
	}

	@Override
	@Nonnull
	public Maybe<String> startNestedStep(@Nonnull StartTestItemRQ startStepRequest) {
		Maybe<String> parent = getParent();
		if (parent == null) {
			LOGGER.warn("Unable to find parent ID, skipping step: {}", startStepRequest.getName());
			return Maybe.empty();
		}
		Maybe<String> itemId = launch.startTestItem(parent, startStepRequest);
		steps.put(itemId, new StepEntry(itemId, new FinishTestItemRQ()));
		return itemId;
	}

	@Override
	@Nonnull
	public Maybe<OperationCompletionRS> finishNestedStep(@Nonnull FinishTestItemRQ finishStepRequest) {
		Maybe<String> stepId = getParent();
		if (stepId == null) {
			LOGGER.warn("Unable to find item ID, skipping step a finish step");
			return Maybe.empty();
		}
		StepEntry manualRequest = steps.remove(stepId);
		String manualStatus = ofNullable(manualRequest).map(StepEntry::getFinishTestItemRQ).map(FinishTestItemRQ::getStatus).orElse(null);
		String runStatus = ofNullable(finishStepRequest.getStatus()).orElse(ItemStatus.PASSED.name());

		FinishTestItemRQ actualRequest = ObjectUtils.clonePojo(finishStepRequest, FinishTestItemRQ.class);
		if (manualStatus != null) {
			actualRequest.setStatus(manualStatus);
			if (ItemStatus.FAILED.name().equalsIgnoreCase(manualStatus)) {
				failParents();
			}
		} else {
			actualRequest.setStatus(runStatus);
		}
		actualRequest.setStatus(ofNullable(manualStatus).orElse(runStatus));
		return launch.finishTestItem(stepId, actualRequest);
	}

	@Override
	@Nonnull
	public Maybe<OperationCompletionRS> finishNestedStep(@Nonnull ItemStatus status) {
		FinishTestItemRQ finishStepRequest = buildFinishTestItemRequest(status);
		return finishNestedStep(finishStepRequest);
	}

	@Override
	@Nonnull
	public Maybe<OperationCompletionRS> finishNestedStep() {
		return finishNestedStep(ItemStatus.PASSED);
	}

	@Override
	@Nonnull
	public Maybe<OperationCompletionRS> finishNestedStep(@Nullable Throwable throwable) {
		ReportPortal.emitLog(itemUuid -> buildSaveLogRequest(itemUuid, throwable));
		return finishNestedStep(ItemStatus.FAILED);
	}

	@Override
	@Nonnull
	public Maybe<String> step(@Nonnull ItemStatus status, @Nonnull String name) {
		Maybe<String> itemId = startNestedStep(buildStartStepRequest(name));
		return finishNestedStep(status).flatMap(finishRs -> itemId);
	}

	@Override
	@Nonnull
	public Maybe<String> step(@Nonnull String name) {
		return step(ItemStatus.PASSED, name);
	}

	@Nullable
	@Override
	public <T> T step(@Nonnull ItemStatus stepSuccessStatus, @Nonnull String name, @Nonnull Supplier<T> actions) {
		startNestedStep(buildStartStepRequest(name));
		try {
			T result = actions.get();
			finishNestedStep(stepSuccessStatus);
			return result;
		} catch (RuntimeException | Error e) {
			finishNestedStep(ItemStatus.FAILED);
			throw e;
		}
	}

	@Nullable
	@Override
	public <T> T step(@Nonnull String name, @Nonnull Supplier<T> actions) {
		return step(ItemStatus.PASSED, name, actions);
	}

	@Nonnull
	private Maybe<String> startStepRequest(final StartTestItemRQ startTestItemRQ) {
		finishPreviousStepInternal(null).ifPresent(e -> {
			Date previousDate = e.getTimestamp();
			Date currentDate = startTestItemRQ.getStartTime();
			if (!previousDate.before(currentDate)) {
				startTestItemRQ.setStartTime(new Date(previousDate.getTime() + 1));
			}
			if (ItemStatus.FAILED.name().equalsIgnoreCase(e.getFinishTestItemRQ().getStatus())) {
				failParents();
			}
		});
		return startNestedStep(startTestItemRQ);
	}

	private StartTestItemRQ buildStartStepRequest(@Nonnull String name) {
		StartTestItemRQ startTestItemRQ = new StartTestItemRQ();
		startTestItemRQ.setName(name);
		startTestItemRQ.setType("STEP");
		startTestItemRQ.setHasStats(false);
		startTestItemRQ.setStartTime(Calendar.getInstance().getTime());
		return startTestItemRQ;
	}

	private void addStepEntry(Maybe<String> stepId, ItemStatus status, Date timestamp) {
		FinishTestItemRQ finishTestItemRQ = buildFinishTestItemRequest(status);
		steps.put(stepId, new StepEntry(stepId, timestamp, finishTestItemRQ));
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
				LOGGER.error("Unable to read file attachment: {}", e.getMessage(), e);
			}
		}
		return logRQ;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, Throwable throwable, File file) {
		String message = throwable != null ? getStackTrace(throwable) : "Test has failed without exception";
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
