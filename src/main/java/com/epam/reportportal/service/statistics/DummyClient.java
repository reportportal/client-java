/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service.statistics;

import com.epam.reportportal.service.statistics.item.StatisticsItem;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class DummyClient implements Statistics {

	@Override
	public Maybe<Response<ResponseBody>> send(StatisticsItem item) {
		return Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create(MediaType.get("text/plain"), ""))));
	}

	@Override
	public void close() {
		// did nothing - do nothing
	}
}
