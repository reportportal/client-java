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

import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.LaunchResource;
import com.epam.ta.reportportal.ws.model.launch.MergeLaunchesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import retrofit2.http.*;

import java.util.Map;

/**
 * @author Andrei Varabyeu
 */
public interface ReportPortalClient {

	@POST("v1/launch")
	Maybe<StartLaunchRS> startLaunch(@Body StartLaunchRQ rq);

	@POST("v1/launch/merge")
	Maybe<LaunchResource> mergeLaunches(@Body MergeLaunchesRQ rq);

	@PUT("v1/launch/{launchId}/finish")
	Maybe<OperationCompletionRS> finishLaunch(@Path("launchId") String launch, @Body FinishExecutionRQ rq);

	@POST("v1/item/")
	Maybe<ItemCreatedRS> startTestItem(@Body StartTestItemRQ rq);

	@POST("v1/item/{parent}")
	Maybe<ItemCreatedRS> startTestItem(@Path("parent") String parent, @Body StartTestItemRQ rq);

	@PUT("v1/item/{itemId}")
	Maybe<OperationCompletionRS> finishTestItem(@Path("itemId") String itemId, @Body FinishTestItemRQ rq);

	@POST("v1/log/")
	Maybe<EntryCreatedAsyncRS> log(@Body SaveLogRQ rq);

	@Multipart
	@POST("v1/log/")
	Maybe<BatchSaveOperatingRS> log(@PartMap Map<String, Object> rq);

	@GET("v1/launch/uuid/{launchUuid}")
	Maybe<LaunchResource> getLaunchByUuid(@Path("launchUuid") String launchUuid);
}
