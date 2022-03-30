/*
 *  Copyright 2022 EPAM Systems
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

package com.epam.reportportal.service;

import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoggingContextTest {

	@Test
	@Order(1)
	public void test_logging_context_current_null_safety() {
		assertNull(LoggingContext.context());
	}

	@Test
	public void test_logging_context_init() {
		LoggingContext context = LoggingContext.init(Maybe.just("launch_id"),
				Maybe.just("item_id"),
				mock(ReportPortalClient.class),
				Schedulers.from(Executors.newSingleThreadExecutor())
		);

		assertThat(LoggingContext.context(), sameInstance(context));
	}

	@Test
	public void test_second_logging_context_init_appends_instance_to_deque() {
		Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
		LoggingContext.init(Maybe.just("launch_id"), Maybe.just("item_id"), mock(ReportPortalClient.class), scheduler);

		LoggingContext context2 = LoggingContext.init(Maybe.just("launch_id"),
				Maybe.just("item_id2"),
				mock(ReportPortalClient.class),
				scheduler
		);

		assertThat(LoggingContext.context(), sameInstance(context2));
	}

	@Test
	public void test_complete_method_removes_context() {
		LoggingContext context = LoggingContext.init(Maybe.just("launch_id"),
				Maybe.just("item_id"),
				mock(ReportPortalClient.class),
				Schedulers.from(Executors.newSingleThreadExecutor())
		);

		LoggingContext.complete();
		assertThat(LoggingContext.context(), anyOf(nullValue(), not(sameInstance(context))));
	}
}
