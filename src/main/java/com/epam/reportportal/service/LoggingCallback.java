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

import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
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
		public void accept(OperationCompletionRS rs) throws Exception {
			LOGGER.debug(rs.getResultMessage());
		}
	};

	/**
	 * Logs an error
	 */
	static final Consumer<Throwable> LOG_ERROR = new Consumer<Throwable>() {
		@Override
		public void accept(Throwable rs) throws Exception {
			LOGGER.error("ReportPortal execution error", rs);
		}
	};

	/**
	 * Logs message once some entity creation
	 *
	 * @param entry Type of entity
	 * @return Consumer/Callback
	 */
	static Consumer<EntryCreatedRS> logCreated(final String entry) {
		return new Consumer<EntryCreatedRS>() {
			@Override
			public void accept(EntryCreatedRS rs) throws Exception {
				LOGGER.debug("ReportPortal {} with ID '{}' has been created", entry, rs.getId());
			}
		};
	}
}
