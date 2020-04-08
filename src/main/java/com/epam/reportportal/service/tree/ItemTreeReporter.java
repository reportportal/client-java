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
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import io.reactivex.Maybe;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * This class provides methods for sending requests to the Report Portal instance, using {@link ReportPortalClient}
 * and {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}.
 * Provided requests:
 * - start test item
 * - finish test item
 * - send log
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeReporter {

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
		if (item != null && launchUuid != null) {
			Maybe<OperationCompletionRS> finishResponse = testItemLeaf.getFinishResponse();
			if (finishResponse != null) {
				@SuppressWarnings("unused")
				OperationCompletionRS finishItem = finishResponse.blockingGet(); // we do this call to ensure we are the latest update ith the chain
			}
			return sendFinishItemRequest(reportPortalClient, launchUuid, item, finishTestItemRQ);
		} else {
			return Maybe.empty();
		}
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
		MultiPartRequest multiPartRequest = HttpRequestUtils.buildLogMultiPartRequest(Lists.newArrayList(saveLogRequest));
		return reportPortalClient.log(multiPartRequest);
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
		TypeAwareByteSource data = new TypeAwareByteSource(Files.asByteSource(file), MimeTypeDetector.detect(file));
		SaveLogRQ.File fileModel = new SaveLogRQ.File();
		fileModel.setContent(data.read());
		fileModel.setContentType(data.getMediaType());
		fileModel.setName(file.getName());
		return fileModel;
	}
}
