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

import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.functions.Consumer;

import static com.epam.reportportal.service.Launch.LOGGER;

/**
 * Set of logging callback for ReportPortal client
 *
 * @author Andrei Varabyeu
 */
final class LoggingCallback {

	private LoggingCallback() {
		//statics only
	}

	/**
	 * Logs success
	 */
	static final Consumer<OperationCompletionRS> LOG_SUCCESS = new Consumer<OperationCompletionRS>() {
		@Override
		public void accept(OperationCompletionRS rs) {
			LOGGER.debug(rs.getResultMessage());
		}
	};

	/**
	 * Logs an error
	 */
	static final Consumer<Throwable> LOG_ERROR = new Consumer<Throwable>() {
		@Override
		public void accept(Throwable rs) {
			LOGGER.error("[{}] ReportPortal execution error", Thread.currentThread().getId(), rs);
		}
	};

	/**
	 * Logs message once some entity creation
	 *
	 * @param entry Type of entity
	 * @return Consumer/Callback
	 */
	static Consumer<EntryCreatedAsyncRS> logCreated(final String entry) {
		return new Consumer<EntryCreatedAsyncRS>() {
			@Override
			public void accept(EntryCreatedAsyncRS rs) {
				LOGGER.debug("ReportPortal {} with ID '{}' has been created", entry, rs.getId());
			}
		};
	}
}
