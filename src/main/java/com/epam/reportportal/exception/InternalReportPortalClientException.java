/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-core
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
package com.epam.reportportal.exception;

/**
 * Report portal client exception define any internal client logic exception.
 */
public class InternalReportPortalClientException extends RuntimeException {

	private static final long serialVersionUID = -4231070395029601011L;

	public InternalReportPortalClientException(String message, Exception e) {
		super(message, e);
	}

	public InternalReportPortalClientException(String message) {
		super(message);
	}
}
