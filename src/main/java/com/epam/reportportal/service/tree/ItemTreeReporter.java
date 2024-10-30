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

package com.epam.reportportal.service.tree;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.reporting.*;
import com.epam.ta.reportportal.ws.reporting.SaveLogRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import static com.epam.reportportal.utils.files.Utils.getFile;

/**
 * This class provides methods for sending requests to the ReportPortal instance, using {@link ReportPortalClient}
 * and {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}.
 * Provided requests:
 * - start test item
 * - finish test item
 * - send log
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeReporter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemTreeReporter.class);

	private ItemTreeReporter() {
		//static only
	}

	/**
	 * @param reportPortalClient {@link ReportPortalClient}
	 * @param startTestItemRQ    {@link StartTestItemRQ}
	 * @param launchUuid         Launch UUID
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return {@link Maybe} containing item UUID
	 */
	public static Maybe<String> startItem(ReportPortalClient reportPortalClient, final StartTestItemRQ startTestItemRQ,
			final Maybe<String> launchUuid, final TestItemTree.TestItemLeaf testItemLeaf) {
		final Maybe<String> parent = testItemLeaf.getParentId();
		if (parent != null && launchUuid != null) {
			return sendStartItemRequest(reportPortalClient, launchUuid, parent, startTestItemRQ);
		} else {
			return Maybe.empty();
		}

	}

	/**
	 * @param reportPortalClient {@link ReportPortalClient}
	 * @param finishTestItemRQ   {@link FinishTestItemRQ}
	 * @param launchUuid         Launch UUID
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return {@link Maybe} containing item UUID
	 */
	public static Maybe<OperationCompletionRS> finishItem(final ReportPortalClient reportPortalClient,
			final FinishTestItemRQ finishTestItemRQ, final Maybe<String> launchUuid, final TestItemTree.TestItemLeaf testItemLeaf) {
		final Maybe<String> item = testItemLeaf.getItemId();
		final Maybe<OperationCompletionRS> finishResponse = testItemLeaf.getFinishResponse();
		if (item == null || launchUuid == null) {
			return Maybe.empty();
		}
		if (finishResponse != null) {
			Throwable t = finishResponse.ignoreElement().blockingGet(); //  ensure we are the last update in the chain
			if (t != null) {
				LOGGER.warn("A main item finished with error", t);
			}
		}
		return sendFinishItemRequest(reportPortalClient, launchUuid, item, finishTestItemRQ);
	}

	/**
	 * @param reportPortalClient {@link com.epam.reportportal.service.ReportPortal}
	 * @param level              Log level
	 * @param message            Log message
	 * @param logTime            Log time
	 * @param launchUuid         Launch UUID
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return True if request is sent otherwise false
	 */
	public static boolean sendLog(final ReportPortalClient reportPortalClient, final String level, final String message, final Date logTime,
			Maybe<String> launchUuid, TestItemTree.TestItemLeaf testItemLeaf) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (launchUuid != null && itemId != null) {
			sendLogRequest(reportPortalClient, launchUuid, itemId, level, message, logTime).subscribe();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param reportPortalClient {@link com.epam.reportportal.service.ReportPortal}
	 * @param level              Log level
	 * @param message            Log message
	 * @param logTime            Log time
	 * @param file               a file to attach to the log message
	 * @param launchUuid         Launch UUID
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return True if request is sent otherwise false
	 */
	public static boolean sendLog(final ReportPortalClient reportPortalClient, final String level, final String message, final Date logTime,
			final File file, Maybe<String> launchUuid, TestItemTree.TestItemLeaf testItemLeaf) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (launchUuid != null && itemId != null) {
			sendLogMultiPartRequest(reportPortalClient, launchUuid, itemId, level, message, logTime, file).subscribe();
			return true;
		} else {
			return false;
		}
	}

	private static Maybe<String> sendStartItemRequest(final ReportPortalClient reportPortalClient, Maybe<String> launchUuid,
			final Maybe<String> parent, final StartTestItemRQ startTestItemRQ) {
		startTestItemRQ.setLaunchUuid(launchUuid.blockingGet());
		return reportPortalClient.startTestItem(parent.blockingGet(), startTestItemRQ).map(EntryCreatedAsyncRS::getId).cache();
	}

	private static Maybe<OperationCompletionRS> sendFinishItemRequest(final ReportPortalClient reportPortalClient, Maybe<String> launchUuid,
			Maybe<String> item, final FinishTestItemRQ finishTestItemRQ) {
		finishTestItemRQ.setLaunchUuid(launchUuid.blockingGet());
		return reportPortalClient.finishTestItem(item.blockingGet(), finishTestItemRQ);
	}

	private static Maybe<EntryCreatedAsyncRS> sendLogRequest(final ReportPortalClient reportPortalClient, Maybe<String> launchUuid,
			final Maybe<String> itemUuid, final String level, final String message, final Date logTime) {
		SaveLogRQ saveLogRequest = createSaveLogRequest(launchUuid.blockingGet(), itemUuid.blockingGet(), level, message, logTime);
		return reportPortalClient.log(saveLogRequest);
	}

	private static Maybe<BatchSaveOperatingRS> sendLogMultiPartRequest(final ReportPortalClient reportPortalClient,
			Maybe<String> launchUuid, final Maybe<String> itemId, final String level, final String message, final Date logTime,
			final File file) {
		SaveLogRQ saveLogRequest = createSaveLogRequest(launchUuid.blockingGet(), itemId.blockingGet(), level, message, logTime);
		try {
			saveLogRequest.setFile(createFileModel(file));
		} catch (IOException e) {
			return Maybe.error(e);
		}
		return reportPortalClient.log(HttpRequestUtils.buildLogMultiPartRequest(Collections.singletonList(saveLogRequest)));
	}

	private static SaveLogRQ createSaveLogRequest(String launchUuid, String itemId, String level, String message, Date logTime) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		saveLogRQ.setLaunchUuid(launchUuid);
		saveLogRQ.setItemUuid(itemId);
		saveLogRQ.setLevel(level);
		saveLogRQ.setLogTime(logTime);
		saveLogRQ.setMessage(message);
		return saveLogRQ;
	}

	private static SaveLogRQ.File createFileModel(File file) throws IOException {
		TypeAwareByteSource data = getFile(file);
		SaveLogRQ.File fileModel = new SaveLogRQ.File();
		fileModel.setContent(data.read());
		fileModel.setContentType(data.getMediaType());
		fileModel.setName(file.getName());
		return fileModel;
	}
}
