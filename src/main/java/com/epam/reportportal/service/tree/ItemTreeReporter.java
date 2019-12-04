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
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

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
	 * @param launchId           Launch id
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return {@link Maybe} containing item id
	 */
	public static Maybe<String> startItem(ReportPortalClient reportPortalClient, final StartTestItemRQ startTestItemRQ,
			final Maybe<String> launchId, final TestItemTree.TestItemLeaf testItemLeaf) {
		final Maybe<String> parent = testItemLeaf.getParentId();
		if (parent != null && launchId != null) {
			Maybe<String> itemId = sendStartItemRequest(reportPortalClient, launchId, parent, startTestItemRQ);
			itemId.subscribeOn(Schedulers.io()).subscribe();
			return itemId;
		} else {
			return Maybe.empty();
		}

	}

	/**
	 * @param reportPortalClient {@link ReportPortalClient}
	 * @param finishTestItemRQ   {@link FinishTestItemRQ}
	 * @param launchId           Launch id
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return {@link Maybe} containing item id
	 */
	public static Maybe<OperationCompletionRS> finishItem(final ReportPortalClient reportPortalClient,
			final FinishTestItemRQ finishTestItemRQ, final Maybe<String> launchId, final TestItemTree.TestItemLeaf testItemLeaf) {
		final Maybe<String> item = testItemLeaf.getItemId();
		if (item != null && launchId != null) {
			return launchId.flatMap(new Function<String, MaybeSource<OperationCompletionRS>>() {
				@Override
				public MaybeSource<OperationCompletionRS> apply(final String launchId) {
					finishTestItemRQ.setLaunchUuid(launchId);
					Maybe<OperationCompletionRS> finishResponse = testItemLeaf.getFinishResponse();
					if (finishResponse != null) {
						return finishResponse.flatMap(new Function<OperationCompletionRS, MaybeSource<OperationCompletionRS>>() {
							@Override
							public MaybeSource<OperationCompletionRS> apply(OperationCompletionRS operationCompletionRS) {
								return sendFinishItemRequest(item, finishTestItemRQ, reportPortalClient);
							}
						});
					} else {
						return sendFinishItemRequest(item, finishTestItemRQ, reportPortalClient);
					}
				}
			}).subscribeOn(Schedulers.io());
		} else {
			return Maybe.empty();
		}

	}

	/**
	 * @param reportPortalClient {@link com.epam.reportportal.service.ReportPortal}
	 * @param level              Log level
	 * @param message            Log message
	 * @param logTime            Log time
	 * @param launchId           Launch id
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return True if request is sent otherwise false
	 */
	public static boolean sendLog(final ReportPortalClient reportPortalClient, final String level, final String message, final Date logTime,
			Maybe<String> launchId, TestItemTree.TestItemLeaf testItemLeaf) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (launchId != null && itemId != null) {
			sendLogRequest(reportPortalClient, launchId, itemId, level, message, logTime).subscribe();
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
	 * @param launchId           Launch id
	 * @param testItemLeaf       {@link com.epam.reportportal.service.tree.TestItemTree.TestItemLeaf}
	 * @return True if request is sent otherwise false
	 */
	public static boolean sendLog(final ReportPortalClient reportPortalClient, final String level, final String message, final Date logTime,
			final File file, Maybe<String> launchId, TestItemTree.TestItemLeaf testItemLeaf) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (launchId != null && itemId != null) {
			sendLogMultiPartRequest(reportPortalClient, launchId, itemId, level, message, logTime, file).subscribe();
			return true;
		} else {
			return false;
		}
	}

	private static Maybe<String> sendStartItemRequest(final ReportPortalClient reportPortalClient, Maybe<String> launchId,
			final Maybe<String> parent, final StartTestItemRQ startTestItemRQ) {
		return launchId.flatMap(new Function<String, MaybeSource<String>>() {
			@Override
			public MaybeSource<String> apply(String launchId) {
				return parent.flatMap(new Function<String, MaybeSource<String>>() {
					@Override
					public MaybeSource<String> apply(String parentId) {
						return reportPortalClient.startTestItem(parentId, startTestItemRQ).map(new Function<ItemCreatedRS, String>() {
							@Override
							public String apply(ItemCreatedRS itemCreatedRS) {
								return itemCreatedRS.getId();
							}
						});
					}
				});
			}
		}).cache();
	}

	private static Maybe<OperationCompletionRS> sendFinishItemRequest(Maybe<String> item, final FinishTestItemRQ finishTestItemRQ,
			final ReportPortalClient reportPortalClient) {
		return item.flatMap(new Function<String, MaybeSource<OperationCompletionRS>>() {
			@Override
			public MaybeSource<OperationCompletionRS> apply(final String itemId) {
				return reportPortalClient.finishTestItem(itemId, finishTestItemRQ);
			}
		});
	}

	private static Maybe<EntryCreatedAsyncRS> sendLogRequest(final ReportPortalClient reportPortalClient, Maybe<String> launchId,
			final Maybe<String> itemId, final String level, final String message, final Date logTime) {
		return launchId.flatMap(new Function<String, MaybeSource<EntryCreatedAsyncRS>>() {
			@Override
			public MaybeSource<EntryCreatedAsyncRS> apply(final String launchId) {
				return itemId.flatMap(new Function<String, MaybeSource<EntryCreatedAsyncRS>>() {
					@Override
					public MaybeSource<EntryCreatedAsyncRS> apply(String itemId) {
						SaveLogRQ saveLogRequest = createSaveLogRequest(launchId, itemId, level, message, logTime);
						return reportPortalClient.log(saveLogRequest);
					}
				});
			}
		}).observeOn(Schedulers.io());
	}

	private static Maybe<BatchSaveOperatingRS> sendLogMultiPartRequest(final ReportPortalClient reportPortalClient, Maybe<String> launchId,
			final Maybe<String> itemId, final String level, final String message, final Date logTime, final File file) {
		return launchId.flatMap(new Function<String, MaybeSource<BatchSaveOperatingRS>>() {
			@Override
			public MaybeSource<BatchSaveOperatingRS> apply(final String launchId) {
				return itemId.flatMap(new Function<String, MaybeSource<BatchSaveOperatingRS>>() {
					@Override
					public MaybeSource<BatchSaveOperatingRS> apply(String itemId) throws Exception {
						SaveLogRQ saveLogRequest = createSaveLogRequest(launchId, itemId, level, message, logTime);
						saveLogRequest.setFile(createFileModel(file));
						MultiPartRequest multiPartRequest = HttpRequestUtils.buildLogMultiPartRequest(Lists.newArrayList(saveLogRequest));
						return reportPortalClient.log(multiPartRequest);
					}
				});
			}
		}).observeOn(Schedulers.io());
	}

	private static SaveLogRQ createSaveLogRequest(String launchId, String itemId, String level, String message, Date logTime) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		saveLogRQ.setLaunchUuid(launchId);
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
