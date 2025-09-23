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
package com.epam.reportportal.service.logs;

import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of logging callback for ReportPortal client
 *
 * @author Andrei Varabyeu
 */
public final class LaunchLoggingCallback {

	private static final Logger LOGGER = LoggerFactory.getLogger(Launch.class);

	private LaunchLoggingCallback() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Logs success
	 */
	public static final Consumer<OperationCompletionRS> LOG_SUCCESS = rs -> LOGGER.debug(rs.getMessage());

	/**
	 * Logs an error
	 */
	public static final Consumer<Throwable> LOG_ERROR = rs -> LOGGER.error(
			"[{}] ReportPortal execution error",
			Thread.currentThread().getId(),
			rs
	);

	/**
	 * Logs message once some entity creation
	 *
	 * @param entry Type of entity
	 * @return Consumer/Callback
	 */
	public static Consumer<EntryCreatedAsyncRS> logCreated(final String entry) {
		return rs -> LOGGER.debug("ReportPortal {} with ID '{}' has been created", entry, rs.getId());
	}
}
