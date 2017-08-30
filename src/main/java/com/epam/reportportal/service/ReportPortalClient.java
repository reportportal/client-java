/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.service;

import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.github.avarabyeu.restendpoint.http.MultiPartRequest;
import com.github.avarabyeu.restendpoint.http.annotation.Body;
import com.github.avarabyeu.restendpoint.http.annotation.Multipart;
import com.github.avarabyeu.restendpoint.http.annotation.Path;
import com.github.avarabyeu.restendpoint.http.annotation.Request;
import io.reactivex.Maybe;

import static com.github.avarabyeu.restendpoint.http.HttpMethod.POST;
import static com.github.avarabyeu.restendpoint.http.HttpMethod.PUT;

/**
 * @author Andrei Varabyeu
 */
public interface ReportPortalClient {

    @Request(method = POST, url = "/launch")
    Maybe<EntryCreatedRS> startLaunch(@Body StartLaunchRQ rq);

    @Request(method = PUT, url = "/launch/{launchId}/finish")
    Maybe<OperationCompletionRS> finishLaunch(@Path("launchId") String launch, @Body FinishExecutionRQ rq);

    @Request(method = POST, url = "/item/")
    Maybe<EntryCreatedRS> startTestItem(@Body StartTestItemRQ rq);

    @Request(method = POST, url = "/item/{parent}")
    Maybe<EntryCreatedRS> startTestItem(@Path("parent") String parent, @Body StartTestItemRQ rq);

    @Request(method = PUT, url = "/item/{itemId}")
    Maybe<OperationCompletionRS> finishTestItem(@Path("itemId") String itemId, @Body FinishTestItemRQ rq);

    @Request(method = POST, url = "/log/")
    Maybe<EntryCreatedRS> log(@Body SaveLogRQ rq);

    @Request(method = POST, url = "/log/")
    Maybe<BatchSaveOperatingRS> log(@Body @Multipart MultiPartRequest rq);
}
